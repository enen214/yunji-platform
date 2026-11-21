from local_runtime import LocalRuntime
from urllib.request import Request,urlopen
import json,traceback
r=LocalRuntime()
try:
 r.start_database();r.migrate();r.seed();r.start_redis();r.start_api()
 body={'clientId':'e5cd7e4891bf95d1d19206ce24a7b32e','grantType':'password','tenantId':'000000','username':'admin','password':'admin123'}
 req=Request(r.url+'/auth/login',data=json.dumps(body).encode(),headers={'Content-Type':'application/json'})
 print('framework login code',json.loads(urlopen(req).read())['code'])
except Exception: traceback.print_exc()
finally:
 print('LOGS',r.work);r.close()
