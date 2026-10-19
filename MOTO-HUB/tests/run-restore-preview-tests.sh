#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)";trap 'rm -rf "$out"' EXIT
mkdir -p "$out/org/json"
cat > "$out/org/json/JSONArray.java" <<'JAVA'
package org.json;
public final class JSONArray { final int count; public JSONArray(int n){count=n;} public int length(){return count;} }
JAVA
cat > "$out/org/json/JSONObject.java" <<'JAVA'
package org.json;
import java.util.*;
public final class JSONObject {
 private final Map<String,JSONArray> data=new HashMap<>();
 public JSONObject put(String key,JSONArray arr){data.put(key,arr);return this;}
 public JSONArray optJSONArray(String key){return data.get(key);}
}
JAVA
javac -encoding UTF-8 -d "$out" "$out"/org/json/*.java \
 app/src/main/java/com/motohub/next/RestorePreview.java tests/RestorePreviewTest.java
java -cp "$out" com.motohub.next.RestorePreviewTest
