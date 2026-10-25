#!/usr/bin/env python3
"""Sign or verify APK using APK Signature Scheme v2 (no external apksigner)."""
from __future__ import annotations
import argparse, hashlib, os, struct, zipfile, getpass
from pathlib import Path
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives.serialization import pkcs12

MAGIC = b'APK Sig Block 42'
V2_ID = 0x7109871a
ALG_RSA_PKCS1_SHA256 = 0x0103
U32 = lambda n: struct.pack('<I', n)
U64 = lambda n: struct.pack('<Q', n)
LP = lambda b: U32(len(b)) + b

def unpack_u32(b, idx=0): return struct.unpack_from('<I', b, idx)[0]
def eocd_find(b):
    # reject ZIP64 and comment ambiguity, choose the EOCD that ends at EOF.
    for i in range(len(b)-22, max(-1, len(b)-65557), -1):
        if b[i:i+4] == b'PK\x05\x06' and i + 22 <= len(b) and i+22+struct.unpack_from('<H',b,i+20)[0] == len(b):
            if b[i+4:i+8] != b'\0'*4: raise ValueError('Multi-disk ZIP unsupported')
            off=unpack_u32(b,i+16)
            if off == 0xffffffff: raise ValueError('ZIP64 unsupported')
            return i, off
    raise ValueError('EOCD not found')

def split_apk(b):
    eocd, central = eocd_find(b)
    if not (central<=eocd<len(b)):raise ValueError('Bad central dir pointer')
    old_start = central
    if central >=24 and b[central-16:central]==MAGIC:
        old_size=struct.unpack_from('<Q',b,central-24)[0]
        first=central-(old_size+8)
        if first<0 or struct.unpack_from('<Q',b,first)[0] != old_size:
            raise ValueError('Malformed prior APK signing block')
        old_start=first
    prefix=b[:old_start]
    directory=b[central:eocd]
    # EOCD central directory offset changed to the offset of signing block while digesting.
    end_record=bytearray(b[eocd:]); struct.pack_into('<I',end_record,16,old_start)
    return prefix,directory,bytes(end_record)

def content_digest(parts):
    n=sum((len(b)+1048575)//1048576 for b in parts)
    h=hashlib.sha256();h.update(b'\x5a'+U32(n))
    for b in parts:
        for i in range(0,len(b),1048576):
            chunk=b[i:i+1048576]
            h.update(hashlib.sha256(b'\xa5'+U32(len(chunk))+chunk).digest())
    return h.digest()

def sign_apk(src,out,keystore,password):
    key,cert,_=pkcs12.load_key_and_certificates(Path(keystore).read_bytes(),password.encode())
    if key is None or cert is None: raise ValueError('Missing key/cert')
    signed_path=Path(src).read_bytes(); prefix,directory,eocd=split_apk(signed_path)
    d=content_digest((prefix,directory,eocd))
    cert_der=cert.public_bytes(serialization.Encoding.DER)
    pk=key.public_key().public_bytes(serialization.Encoding.DER,serialization.PublicFormat.SubjectPublicKeyInfo)
    digests=LP(LP(U32(ALG_RSA_PKCS1_SHA256)+LP(d)))
    certs=LP(LP(cert_der))
    signed_data=digests+certs+LP(b'')
    rawsig=key.sign(signed_data,padding.PKCS1v15(),hashes.SHA256())
    signatures=LP(LP(U32(ALG_RSA_PKCS1_SHA256)+LP(rawsig)))
    signer=LP(signed_data)+signatures+LP(pk)
    v2value=LP(LP(signer))
    pair=U64(len(v2value)+4)+U32(V2_ID)+v2value
    block=U64(len(pair)+24)+pair+U64(len(pair)+24)+MAGIC
    # central dir offset final points to after new signing block
    corrected_eocd=bytearray(eocd)
    struct.pack_into('<I',corrected_eocd,16,len(prefix)+len(block))
    with open(out,'wb') as f:
        f.write(prefix); f.write(block); f.write(directory); f.write(corrected_eocd)
    return hashlib.sha256(cert_der).hexdigest()

def verify_v2(path):
    b=Path(path).read_bytes();eocd,central=eocd_find(b)
    if b[central-16:central] != MAGIC: raise ValueError('V2 signing block missing')
    oldsize=struct.unpack_from('<Q',b,central-24)[0]
    first=central-(oldsize+8)
    if struct.unpack_from('<Q',b,first)[0]!=oldsize: raise ValueError('Bad signing block sizes')
    pos=first+8;end=central-24;got=None
    while pos<end:
        length=struct.unpack_from('<Q',b,pos)[0]; kind=unpack_u32(b,pos+8)
        if kind==V2_ID:got=b[pos+12:pos+8+length]
        pos+=8+length
    if got is None: raise ValueError('Missing v2 id')
    def take_lp(buf,idx=0):
        length=unpack_u32(buf,idx);return buf[idx+4:idx+4+length],idx+4+length
    signers,p=take_lp(got)
    signer,p=take_lp(signers)
    sdata,p=take_lp(signer)
    signatures,p=take_lp(signer,p)
    pub,p=take_lp(signer,p)
    ds,q=take_lp(sdata);certs,q=take_lp(sdata,q)
    digest_rec,_=take_lp(ds);algo=unpack_u32(digest_rec)
    digest,_=take_lp(digest_rec,4)
    signature_rec,_=take_lp(signatures)
    signature,_=take_lp(signature_rec,4)
    if algo != ALG_RSA_PKCS1_SHA256 or unpack_u32(signature_rec)!=algo:raise ValueError('Algorithm mismatch')
    crt,_=take_lp(certs)
    from cryptography import x509
    cert=x509.load_der_x509_certificate(crt)
    cert_key=cert.public_key().public_bytes(serialization.Encoding.DER,serialization.PublicFormat.SubjectPublicKeyInfo)
    if cert_key!=pub:raise ValueError('Public key mismatch')
    cert.public_key().verify(signature,sdata,padding.PKCS1v15(),hashes.SHA256())
    eocd_record=bytearray(b[eocd:]);struct.pack_into('<I',eocd_record,16,first)
    actual=content_digest((b[:first],b[central:eocd],bytes(eocd_record)))
    if actual!=digest:raise ValueError('APK digest mismatch')
    with zipfile.ZipFile(path) as z:
        corrupt=z.testzip()
        if corrupt:raise ValueError('ZIP contents corrupted: '+corrupt)
    return hashlib.sha256(crt).hexdigest()

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('apk');parser.add_argument('--output');parser.add_argument('--keystore');parser.add_argument('--password');parser.add_argument('--verify',action='store_true');a=parser.parse_args()
    if a.verify:print('V2 verified certificate SHA-256:',verify_v2(a.apk))
    else:
        if not a.output or not a.keystore: parser.error('--output and --keystore required for signing')
        password=a.password or os.getenv('MOTO_SIGNING_STORE_PASSWORD') or getpass.getpass('PKCS12 password: ')
        print('V2 signed certificate SHA-256:',sign_apk(a.apk,a.output,a.keystore,password))
