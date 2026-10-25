from pathlib import Path
from concurrent.futures import ThreadPoolExecutor,as_completed
import subprocess,json,time
root=Path(__file__).resolve().parents[1]
scripts=sorted((root/'tests').glob('run-*.sh'))
# Test requiring a separate missing org.json jar cannot compile in this container.
skips={'run-model-tests.sh':'requires org.json test JAR'}
def run(p):
 if p.name in skips:return dict(test=p.name,result='SKIP',detail=skips[p.name])
 try:
  x=subprocess.run(['bash',str(p)],cwd=root,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=45,encoding='utf-8',errors='replace')
  return dict(test=p.name,result='PASS' if x.returncode==0 else 'FAIL',returncode=x.returncode,tail=x.stdout[-1200:])
 except subprocess.TimeoutExpired as e:
  return dict(test=p.name,result='TIMEOUT',detail='45 sec limit')
with ThreadPoolExecutor(max_workers=8) as pool:
 results=sorted((f.result() for f in as_completed([pool.submit(run,p) for p in scripts])),key=lambda i:i['test'])
(root/'CT_SHELL_REGRESSION_REPORT.json').write_text(json.dumps(results,ensure_ascii=False,indent=2))
from collections import Counter
print(Counter(r['result'] for r in results))
for r in results:
 if r['result']!='PASS':print(r['test'],r['result'],r.get('tail','')[-500:])
