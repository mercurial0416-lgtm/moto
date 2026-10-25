#!/usr/bin/env python3
import importlib.util
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
import sys

source = Path(__file__).resolve().parents[1] / "tools" / "choose_release_version.py"
spec=importlib.util.spec_from_file_location("choose_version", source)
mod=importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)
base={"packageName":"com.motohub.next","certificateSha256":mod.EXPECTED_CERT,"versionCode":517}
checks=0
def eq(expected,got,name):
    global checks
    checks+=1
    assert expected==got,(name,expected,got)
def invalid(block,manifest,label):
    global checks
    checks+=1
    try:mod.choose(block,manifest)
    except ValueError:return
    raise AssertionError(label)
eq(518,mod.choose("versionCode 518",base),"first upgrade from 517")
eq(519,mod.choose("versionCode 518",{**base,"versionCode":518}),"next consecutive release")
eq(600,mod.choose("versionCode 600",{**base,"versionCode":518}),"source ahead")
eq(601,mod.choose("versionCode 518",{**base,"versionCode":600}),"published ahead")
invalid("versionCode 518",{**base,"packageName":"wrong.package"},"wrong application id")
invalid("versionCode 518",{**base,"certificateSha256":"bad"},"wrong signing identity")
invalid("versionCode 518",{**base,"versionCode":True},"boolean version")
invalid("versionCode 518",{**base,"versionCode":"517"},"string version")
invalid("no version",base,"missing source version")
invalid("versionCode 518",{**base,"versionCode":999999999},"overflow refused")
with TemporaryDirectory() as folder:
    root=Path(folder)
    (root/"app.gradle").write_text("versionCode 518",encoding="utf-8")
    (root/"published.json").write_text(json.dumps(base),encoding="utf-8")
    result=subprocess.check_output([sys.executable,str(source),"--gradle",str(root/"app.gradle"),
                                    "--manifest",str(root/"published.json")],text=True).strip()
    eq("518",result,"command-line implementation")
print("PASS fixed release version selection:",checks,"checks")
