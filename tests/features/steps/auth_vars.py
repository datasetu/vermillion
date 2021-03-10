import json
import requests
import urllib3
import random
import string
from requests.packages.urllib3.exceptions import InsecureRequestWarning

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

CONSUMER_CERT_PATH = 'consumer.pem'
CONSUMER_KEY_PATH = 'consumer.key.pem'
PROVIDER_CERT_PATH = 'provider.pem'
PROVIDER_KEY_PATH = 'provider.key.pem'


def generate_random_chars(n=32, letters=True, digits=True, special_chars=True):
    generate = ''
    if letters:
        generate += string.ascii_letters
    if digits:
        generate += string.digits
    if special_chars:
        generate += string.punctuation

    return ''.join([random.choice(generate) for _ in range(n)])


headers = {
    'content-type': 'application/json',
    'Host': 'auth.local',
}

resource_ids = []

for i in range(0, 9):
    resource_ids.append(generate_random_chars(special_chars=False))
    # print(resource_ids[i])

acl_set_policy = ""
acl_set_policy += "consumer@iisc.ac.in can access example.com/test-category/" + resource_ids[
    0] + ".public for 1 month if scope = 'write'"
for i in range(1, 6):
    acl_set_policy += ";consumer@iisc.ac.in can access example.com/test-category/" + resource_ids[
        i] + " for 1 month if scope = 'write'"

for i in range(6, 9):
    acl_set_policy += ";consumer@iisc.ac.in can access example.com/test-category/" + resource_ids[
        i] + " for 1 month"

print(acl_set_policy)
data = {"policy": acl_set_policy}
response = requests.post(
    'https://localhost:8443/auth/v1/acl/set',
    headers=headers,
    data=json.dumps(data),
    cert=(PROVIDER_CERT_PATH, PROVIDER_KEY_PATH),
    verify=False)
# print(response.json())
res = {}
request_ids = []
re_ids1 = []
re_ids2 = []
tokens = {}
id_prefix = "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/"
# for i in range(1, 6):
#     res[i] = id_prefix + resource_ids[i]
#     #print(res[i])
#     request_ids.append({
#         "id": id_prefix + resource_ids[i],
#         "scopes": ["read","write"]
#     })
# request_ids.append({
#     "id":
#     id_prefix + resource_ids[0] + ".public",
#     "scopes": ["read","write"]
# })
#
# data = {"request": request_ids}
#
# response = requests.post(
#     'https://localhost:8443/auth/v1/token',
#     headers=headers,
#     cert=(CONSUMER_CERT_PATH, CONSUMER_KEY_PATH),
#     data=json.dumps(data),
#     verify=False)
# #print(response.json())
# r = response.json()
# tokens["master"] = r['token']

for i in range(6, 8):
    res[i] = id_prefix + resource_ids[i]
    # print(res[i])
    request_ids.append({
        "id": id_prefix + resource_ids[i],

    })

data = {"request": request_ids}
# print(data)
response = requests.post(
    'https://localhost:8443/auth/v1/token',
    headers=headers,
    cert=(CONSUMER_CERT_PATH, CONSUMER_KEY_PATH),
    data=json.dumps(data),
    verify=False)
# print(response.json())
r = response.json()
tokens["6_7_read"] = r['token']

for i in range(0, 1):
    res[i] = id_prefix + resource_ids[i]
    # print(res[i])
    request_ids.append
    {
        "id": id_prefix + resource_ids[i], "scope": "write"
    }

data = {"request": request_ids}

response = requests.post(
    'https://localhost:8443/auth/v1/token',
    headers=headers,
    cert=(CONSUMER_CERT_PATH, CONSUMER_KEY_PATH),
    data=json.dumps(data),
    verify=False)
# print(response.json())
r = response.json()
tokens["master"] = r['token']

# request_ids= []
for i in range(3, 5):
    res[i] = id_prefix + resource_ids[i]
    print(res[i])
    re_ids1.append(
        {

            "id": id_prefix + resource_ids[i],
            "scope": 'write'})

data = {"request": re_ids1}
print(data)
response = requests.post(
    'https://localhost:8443/auth/v1/token',
    headers={'content-type': 'application/json'},
    cert=(CONSUMER_CERT_PATH, CONSUMER_KEY_PATH),
    data=json.dumps(data),
    verify=False)
print(response.json())
r = response.json()
tokens["3_4_write"] = r['token']

for i in range(2, 3):
    res[i] = id_prefix + resource_ids[i]
    # print(res[i])
    re_ids2.append({

        "id": id_prefix + resource_ids[i], "scope": "write"
    })

data = {"request": re_ids2}
# print(data)
response = requests.post(
    'https://localhost:8443/auth/v1/token',
    headers=headers,
    cert=(CONSUMER_CERT_PATH, CONSUMER_KEY_PATH),
    data=json.dumps(data),
    verify=False)
# print(response.json())
r = response.json()
tokens["2_write"] = r['token']
# print(tokens)
res[0] = id_prefix + resource_ids[0] + ".public"
for i in range(1, 9):
    res[i] = id_prefix + resource_ids[i]
# print(res)
