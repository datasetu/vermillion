import json
import requests
from behave import when
import urllib3

from requests.packages.urllib3.exceptions import InsecureRequestWarning

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

PROVIDER_CERT_PATH = 'provider.pem'
PROVIDER_KEY_PATH = 'provider.key.pem'


headers = {
    'content-type': 'application/json',
}

data = {
   "policy": "consumer@iisc.ac.in can access example.com/test-category/test-resource-1 for 1 month;consumer@iisc.ac.in can access example.com/test-category/test-resource-2 for 1 month;consumer@iisc.ac.in can access example.com/test-category/test-resource-3 for 1 month;consumer@iisc.ac.in can access example.com/test-category/test-resource.public for 1 month if scope = write;consumer@iisc.ac.in can access example.com/test-category/secure-ts for 1 month;consumer@iisc.ac.in can access example.com/test-category/secure-file for 1 month;consumer@iisc.ac.in can access example.com/test-category/open-file.public for 1 month;consumer@iisc.ac.in can access example.com/test-category/secure-ts1 for 1 month"}
    
response = requests.post('https://localhost:8443/auth/v1/acl/set', headers=headers, data=json.dumps(data),cert=(PROVIDER_CERT_PATH,PROVIDER_KEY_PATH), verify=False)
#r=response.json()
#print(response.json())




