import json
f=open('fail-port1-pretty-soso.json')
ob=json.load(f)
json.dump([x['status'] for x in ob], open('error-msgs.json', 'w'))
f.close()
f=open('ok-port1-pretty-soso.json')
ob=json.load(f)
json.dump([x['port_trace'][len(x['port_trace'])-1] for x in ob], open('oks.json', 'w'))
f.close()
