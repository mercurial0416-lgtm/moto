#!/usr/bin/env python3
"""Compare two plaintext MOTO HUB FULL ZIP exports without disclosing their contents.

Exit 0: no pre-upgrade record or GPS/photo lost; 1: loss/change; 2: invalid ZIP.
For the encrypted backup mode, decrypt locally before running this script.
"""
import argparse
from collections import Counter
import hashlib
import json
import time
import stat
import sys
import zipfile
from pathlib import Path

COLLECTIONS=("vehicles","watchlist","listings","services","expenses","shops","routes","specs","gear")
MAX_TOTAL=250_000_000
EXCLUDED={"README-KO.txt","MOTO-HUB-batch.txt"}

def unique(pairs):
    result={}
    for key,value in pairs:
        if key in result: raise ValueError("Duplicate JSON key")
        result[key]=value
    return result

def canonical(data):
    return json.dumps(data,sort_keys=True,ensure_ascii=False,separators=(",",":"),allow_nan=False)

def load(path):
    if not Path(path).is_file(): raise ValueError("Full ZIP missing")
    manifest={}
    records=None
    preferences=None
    readme=None
    total=0
    with zipfile.ZipFile(path) as archive:
        entries=archive.infolist()
        if len(entries)>100_000: raise ValueError("Too many ZIP entries")
        for item in entries:
            name=item.filename
            if name.startswith("/") or "\\" in name or "\0" in name or ".." in name.split("/"):
                raise ValueError("Unsafe ZIP entry")
            if stat.S_ISLNK(item.external_attr>>16): raise ValueError("Symlink in ZIP")
            if item.is_dir(): continue
            if name in manifest: raise ValueError("Duplicate ZIP entry")
            if name=="MOTO-HUB-batch.txt": raise ValueError("Split ZIP is not a FULL backup")
            total+=item.file_size
            if item.file_size>50_000_000 or total>MAX_TOTAL: raise ValueError("ZIP size limit exceeded")
            digest=hashlib.sha256()
            payload=bytearray() if name in ("MOTO-HUB-data.json","MOTO-HUB-preferences.json","README-KO.txt") else None
            with archive.open(item) as source:
                copied=0
                while True:
                    chunk=source.read(32768)
                    if not chunk: break
                    copied+=len(chunk)
                    if copied>50_000_000: raise ValueError("ZIP entry size limit exceeded")
                    digest.update(chunk)
                    if payload is not None:
                        limit=8_000_000 if name=="MOTO-HUB-data.json" else 32_768 if name=="MOTO-HUB-preferences.json" else 8192
                        if copied>limit: raise ValueError("Backup metadata size limit exceeded")
                        payload.extend(chunk)
            manifest[name]=digest.hexdigest()
            if name=="MOTO-HUB-data.json":
                records=json.loads(payload.decode("utf8"),object_pairs_hook=unique)
            elif name=="MOTO-HUB-preferences.json":
                preferences=json.loads(payload.decode("utf8"),object_pairs_hook=unique)
            elif name=="README-KO.txt":
                readme=payload.decode("utf8")
    if not isinstance(records,dict) or not all(isinstance(records.get(k),list) for k in COLLECTIONS):
        raise ValueError("Not a MOTO HUB FULL backup")
    if readme is None or readme.startswith("MOTO-HUB-PART:"):
        raise ValueError("Full backup README missing or split archive selected")
    if preferences is not None and not isinstance(preferences,dict):
        raise ValueError("Invalid UI preferences metadata")
    return records,manifest,preferences

def compare_preferences(old,new):
    if old is None: return False
    if new is None: return True
    if not isinstance(new,dict): return True
    for key,value in old.items():
        if key!="fuelDrafts":
            if key not in new or canonical(value)!=canonical(new[key]):return True
            continue
        if not isinstance(value,dict) or not isinstance(new.get("fuelDrafts",{}),dict):return True
        for bike,draft in value.items():
            after_draft=new.get("fuelDrafts",{}).get(bike)
            if after_draft is not None:
                if canonical(draft)!=canonical(after_draft):return True
                continue
            # The Android form intentionally expires drafts after seven days;
            # a naturally expired one is not retained user data loss.
            if not isinstance(draft,dict):return True
            saved=draft.get("savedAt",0)
            if not isinstance(saved,int) or saved<=0 or time.time()*1000-saved<=7*24*60*60*1000:
                return True
    return False

def compare(before,after):
    old,old_files,old_prefs=load(before)
    new,new_files,new_prefs=load(after)
    losses={}
    additions={}
    for collection in COLLECTIONS:
        a=Counter(canonical(x) for x in old[collection])
        b=Counter(canonical(x) for x in new[collection])
        losses[collection]=sum((a-b).values())
        additions[collection]=sum((b-a).values())
    # Protect all persisted noncollection settings except the schema version.
    metadata_changed=any(key not in new or canonical(value)!=canonical(new[key])
        for key,value in old.items() if key not in COLLECTIONS and key!="version")
    expected={name:digest for name,digest in old_files.items()
        if name not in EXCLUDED and name not in ("MOTO-HUB-data.json","MOTO-HUB-preferences.json")}
    missing=sum(name not in new_files for name in expected)
    changed=sum(name in new_files and new_files[name]!=digest for name,digest in expected.items())
    preferences_changed=compare_preferences(old_prefs,new_prefs)
    return {"safe":sum(losses.values())==0 and missing==0 and changed==0 and not metadata_changed and not preferences_changed,
            "missing_records":losses,"added_records":additions,
            "missing_gps_or_media_files":missing,"changed_gps_or_media_files":changed,
            "settings_changed":metadata_changed or preferences_changed}

def main(argv=None):
    parser=argparse.ArgumentParser(description="Offline full-ZIP upgrade data audit")
    parser.add_argument("before")
    parser.add_argument("after")
    args=parser.parse_args(argv)
    try: report=compare(args.before,args.after)
    except (OSError,RuntimeError,ValueError,zipfile.BadZipFile,UnicodeError) as error:
        print("Invalid backup: "+str(error),file=sys.stderr)
        return 2
    print(json.dumps(report,ensure_ascii=False,indent=2,sort_keys=True))
    return 0 if report["safe"] else 1

if __name__=="__main__":
    sys.exit(main())
