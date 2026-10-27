#!/usr/bin/env python3
"""Reassemble an offline-signed APK from its *exact* unsigned bytes and public v2 signing block.

A signing block is a public signature, not a private key. Fail closed unless the
complete unsigned SHA-256 matches the offline signer input, and the complete
reconstructed signed SHA-256 matches the offline signer output. This is only
usable for the one preauthorized build; it cannot sign modified APKs.
"""
import argparse
import base64
import hashlib
import json
from pathlib import Path
import struct
import zipfile

def sha(b):
    return hashlib.sha256(b).hexdigest()

def assemble(unsigned, manifest):
    required={"unsignedSha256","signedSha256","unsignedSize","signedSize","offset","insertBase64"}
    if not required.issubset(manifest): raise ValueError("Signing manifest missing required field")
    if len(unsigned)!=manifest["unsignedSize"] or sha(unsigned)!=manifest["unsignedSha256"]:
        raise ValueError("Offline signing input mismatch; cannot reconstruct or publish")
    pos=unsigned.rfind(b"PK\x05\x06",max(0,len(unsigned)-65557))
    if pos<0 or pos+22!=len(unsigned): raise ValueError("Unsupported ZIP comment or invalid EOCD")
    offset=struct.unpack_from("<I",unsigned,pos+16)[0]
    cd_size=struct.unpack_from("<I",unsigned,pos+12)[0]
    if offset!=manifest["offset"] or offset+cd_size!=pos:
        raise ValueError("Unexpected Android APK central directory layout")
    block=base64.b64decode(manifest["insertBase64"],validate=True)
    if not 1000<len(block)<16_384 or not block.endswith(b"APK Sig Block 42"):
        raise ValueError("Invalid public APK signing block")
    eocd=bytearray(unsigned[pos:])
    struct.pack_into("<I",eocd,16,offset+len(block))
    signed=unsigned[:offset]+block+unsigned[offset:pos]+eocd
    if len(signed)!=manifest["signedSize"] or sha(signed)!=manifest["signedSha256"]:
        raise ValueError("Reassembled APK differs from offline-signed reference")
    return signed

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--unsigned",required=True)
    parser.add_argument("--signature-patch",required=True)
    parser.add_argument("--output",required=True)
    args=parser.parse_args()
    metadata=json.loads(Path(args.signature_patch).read_text())
    signed=assemble(Path(args.unsigned).read_bytes(),metadata)
    target=Path(args.output);target.parent.mkdir(parents=True,exist_ok=True)
    target.write_bytes(signed)
    with zipfile.ZipFile(target) as z:
        if z.testzip() is not None:raise ValueError("ZIP CRC validation failed")
    print("Validated preauthorized signed APK, SHA-256:",sha(signed))
if __name__=="__main__":
    main()
