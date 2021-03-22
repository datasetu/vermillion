import json

from utils import generate_random_chars
import requests
import urllib3
from requests.packages.urllib3.exceptions import InsecureRequestWarning

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

CONSUMER_CERT_PATH = 'consumer.pem'
CONSUMER_KEY_PATH = 'consumer.key.pem'
PROVIDER_CERT_PATH = 'provider.pem'
PROVIDER_KEY_PATH = 'provider.key.pem'

headers = {
    'content-type': 'application/json',
    'Host': 'auth.local',
}

resource_ids = []
for i in range(0, 3):
    resource_ids.append(generate_random_chars(special_chars=False))

acl_set_policy = ""

for i in range(0, 2):
    acl_set_policy += "consumer@iisc.ac.in can access example.com/test-category/" + resource_ids[
        i] + " for 1 month;"
acl_set_policy+= "consumer@iisc.ac.in can access example.com/test-category/" + resource_ids[
    2] + " for 1 month"
data = {"policy": acl_set_policy}
# print(data)
response = requests.post(
    'https://localhost:8443/auth/v1/acl/append',
    headers=headers,
    data=json.dumps(data),
    cert=(PROVIDER_CERT_PATH, PROVIDER_KEY_PATH),
    verify=False)
# print(response)
res = {}
request_ids = []
tokens = {}
id_prefix = "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/"
for i in range(0, 3):
    res[i] = id_prefix + resource_ids[i]
    # print(res[i])
    request_ids.append({
        "id": id_prefix + resource_ids[i],
        "scopes": ["read", "write"]
    })

data = {"request": request_ids}

response = requests.post(
    'https://localhost:8443/auth/v1/token',
    headers=headers,
    cert=(CONSUMER_CERT_PATH, CONSUMER_KEY_PATH),
    data=json.dumps(data),
    verify=False)
# print(response.json())
r = response.json()
tokens["down"] = r['token']
for i in range(0, 3):
    res[i] = id_prefix + resource_ids[i]