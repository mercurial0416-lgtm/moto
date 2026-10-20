#!/usr/bin/env python3
"""Emit a public manifest only after Android has verified a fixed-signed APK.

Never includes private signing keys. Not run in this source-only checkpoint.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
from urllib.parse import urlsplit

CERT='212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b'
PACKAGE='com.motohub.next'

def cert_from_apksigner(output):
    match=re.search(r'Signer #1 certificate SHA-256 digest:\s*([0-9a-fA-F:]{64,95})',output)
    if not match: raise ValueError('Missing signing certificate fingerprint')
    return match.group(1).replace(':','').lower()

def package_metadata(badging):
    line=next((s for s in badging.splitlines() if s.startswith('package: ')),None)
    if not line: raise ValueError('aapt missing package metadata')
    data=dict(re.findall(r"(\w+)='([^']*)'",line))
    if data.get('name')!=PACKAGE:raise ValueError('Wrong Android applicationId')
    code=int(data['versionCode'])
    if code<=517: raise ValueError('Refusing stale fixed-release version')
    name=data['versionName']
    if not name or len(name)>100:raise ValueError('Invalid version name')
    sdk=re.search(r"^sdkVersion:'(\d+)'$",badging,re.MULTILINE)
    if not sdk:raise ValueError('Missing minimum SDK')
    return code,name,int(sdk.group(1))

def validate_url(url):
    u=urlsplit(url)
    if u.scheme!='https' or u.username or u.password or u.fragment or not u.netloc:
        raise ValueError('HTTPS release asset URL required')
    if u.hostname!='github.com' or not u.path.startswith('/mercurial0416-lgtm/moto/releases/download/'):
        raise ValueError('Expected a published MOTO HUB GitHub Release asset')
    return url

def create_manifest(apk,apk_url,signer_output,badging):
    fingerprint=cert_from_apksigner(signer_output)
    if fingerprint!=CERT: raise ValueError('Wrong signing certificate; refusing manifest')
    code,name,minimum=package_metadata(badging)
    if minimum<26:raise ValueError('Unexpected min SDK')
    file=Path(apk)
    size=file.stat().st_size
    if size<4096 or size>200_000_000:raise ValueError('Invalid APK size')
    digest=hashlib.sha256(file.read_bytes()).hexdigest()
    return {'packageName':PACKAGE,'versionCode':code,'versionName':name,
            'minSdk':minimum,'sizeBytes':size,'sha256':digest,'certificateSha256':CERT,
            'apkUrl':validate_url(apk_url),'notes':'고정 서명 MOTO HUB 업데이트. 기존 데이터가 유지되며 Android 설치 승인이 필요합니다.'}

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--apk',required=True)
    parser.add_argument('--apk-url',required=True)
    parser.add_argument('--apksigner',required=True)
    parser.add_argument('--aapt',required=True)
    parser.add_argument('--output',required=True)
    args=parser.parse_args()
    signer=subprocess.check_output([args.apksigner,'verify','--print-certs',args.apk],text=True)
    metadata=subprocess.check_output([args.aapt,'dump','badging',args.apk],text=True)
    data=create_manifest(args.apk,args.apk_url,signer,metadata)
    Path(args.output).parent.mkdir(parents=True,exist_ok=True)
    Path(args.output).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
    print('Verified fixed signer, manifest version',data['versionCode'])

if __name__=='__main__':main()
