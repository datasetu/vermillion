import json
import requests
from behave import when
import urllib3
import re
from requests.packages.urllib3.exceptions import InsecureRequestWarning

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

CONSUMER_CERT_PATH = 'consumer.pem'
CONSUMER_KEY_PATH = 'consumer.key.pem'




headers = {
    'content-type': 'application/json',
    'Host': 'auth.local',
}

data = '{ "request": [ { "id": "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource-1" }, { "id":"rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource-2" }, { "id": "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource-3" }, { "id": "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource.public", "scope": "write" }, { "id": "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/secure-ts" }, { "id": "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/secure-file" }, { "id": "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/secure-ts1" } ] }'
    
#    files = {'f': ('token.txt',open('token.txt','r'))}
response = requests.post('https://localhost:8443/auth/v1/token', headers=headers, cert=(CONSUMER_CERT_PATH,CONSUMER_KEY_PATH),data=data, verify=False)
r=str(response.text)

t=[]
for i in range(0,len(r)):
    if r[i]=="a" and r[i+1]=="u" and r[i+2]=="t":
        t=r[i:i+63]

#print(t)

#with open("response.txt","w") as f:
 #       f.write(response.text)

