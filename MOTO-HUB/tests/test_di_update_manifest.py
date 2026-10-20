from pathlib import Path
import tempfile,importlib.util
src=Path(__file__).resolve().parents[1]/'tools/make_update_manifest.py'
spec=importlib.util.spec_from_file_location('updater',src)
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
cert=m.CERT
sgn='Signer #1 certificate SHA-256 digest: '+cert
meta="package: name='com.motohub.next' versionCode='518' versionName='4.0.0-alpha6-di'\nsdkVersion:'26'"
with tempfile.TemporaryDirectory() as d:
    f=Path(d)/'app.apk';f.write_bytes(b'A'*12000)
    data=m.create_manifest(f,'https://github.com/mercurial0416-lgtm/moto/releases/download/v518/app.apk',sgn,meta)
    assert data['versionCode']==518 and data['certificateSha256']==cert and data['sizeBytes']==12000
    for s,b in [(sgn.replace(cert,'0'*64),meta),(sgn,meta.replace("versionCode='518'","versionCode='517'")),(sgn,meta.replace('com.motohub.next','invalid.app'))]:
        try:m.create_manifest(f,'https://github.com/mercurial0416-lgtm/moto/releases/download/v518/app.apk',s,b)
        except ValueError:pass
        else:raise AssertionError('bad release not refused')
    for url in ['http://github.com/mercurial0416-lgtm/moto/releases/download/v518/app.apk','https://github.com.evil.co/release','https://github.com/another/repo/releases/download/v518/app.apk']:
        try:m.create_manifest(f,url,sgn,meta)
        except ValueError:pass
        else:raise AssertionError('untrusted release URL')
print('PASS DI future signed-update manifest generation: 7 checks')
