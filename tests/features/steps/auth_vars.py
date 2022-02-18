import json

from utils import generate_random_chars
import requests
import urllib3
from requests.packages.urllib3.exceptions import InsecureRequestWarning

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)
VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'
CONSUMER_CERT_PATH = 'consumer.pem'
CONSUMER_KEY_PATH = 'consumer.key.pem'
PROVIDER_CERT_PATH = 'provider.pem'
PROVIDER_KEY_PATH = 'provider.key.pem'

headers = {
    'content-type': 'application/json',
    'Host': 'auth.local',
}

resource_ids = []

for i in range(0, 13):
    resource_ids.append(generate_random_chars(special_chars=False))
#    print(resource_ids[i])

acl_set_policy = ""
acl_set_policy += "consumer@iisc.ac.in can access example.com/test-category/" + resource_ids[
    0] + ".public for 1 month"
for i in range(1, 6):
    acl_set_policy += "; consumer@iisc.ac.in can access example.com/test-category/" + resource_ids[
        i] + " for 1 month"
for i in range(6, 8):
    acl_set_policy += "; consumer@iisc.ac.in can access example.com/test-category/" + resource_ids[
        i] + " for 1 second if scope = read"

data = {"policy": acl_set_policy}
response = requests.post(
    'https://localhost:8443/auth/v1/acl/set',
    headers=headers,
    data=json.dumps(data),
    cert=(PROVIDER_CERT_PATH, PROVIDER_KEY_PATH),
    verify=False)
# print(response)
res = {}
request_ids = []
tokens = {}
id_prefix = "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/"
for i in range(1, 6):
    res[i] = id_prefix + resource_ids[i]
    # print(res[i])
    request_ids.append({
        "id": id_prefix + resource_ids[i],
        "scopes": ["read", "write"]
    })

request_ids.append({
    "id":
        id_prefix + resource_ids[0] + ".public",
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
tokens["master"] = r['token']

for i in range(1, 2):
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
tokens["1_2_read_write"] = r['token']

for i in range(3, 4):
    request_ids.append({
        "id": id_prefix + resource_ids[i], "scope": "write"
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
tokens["3_4_write"] = r['token']

for i in range(2, 5):
    request_ids.append({
        "id": id_prefix + resource_ids[i], "scopes": ["read", "write"]
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
tokens["2_5_write"] = r['token']
# print(tokens)
for i in range(6, 8):
    request_ids.append({
        "id": id_prefix + resource_ids[i],
        "scopes": ["read"]
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
tokens["6_7_read"] = r['token']
# print(tokens)
res[0] = id_prefix + resource_ids[0] + ".public"
for i in range(1, 8):
    res[i] = id_prefix + resource_ids[i]
# print(res)

acl_set_policy1 = ""
req_id = []
for i in range(8, 10):
    acl_set_policy1 += "consumer@iisc.ac.in can access example.com/test-category/" + resource_ids[
        i] + " for 1 month;"
acl_set_policy1 += "consumer@iisc.ac.in can access example.com/test-category/" + resource_ids[
    10] + " for 1 month"
data = {"policy": acl_set_policy1}
# print(data)
response = requests.post(
    'https://localhost:8443/auth/v1/acl/append',
    headers=headers,
    data=json.dumps(data),
    cert=(PROVIDER_CERT_PATH, PROVIDER_KEY_PATH),
    verify=False)
# print(response)

for i in range(8, 11):
    res[i] = id_prefix + resource_ids[i]
    # print(res[i])
    req_id.append({
        "id": id_prefix + resource_ids[i],
        "scopes": ["read", "write"]
    })

data = {"request": req_id}

response = requests.post(
    'https://localhost:8443/auth/v1/token',
    headers=headers,
    cert=(CONSUMER_CERT_PATH, CONSUMER_KEY_PATH),
    data=json.dumps(data),
    verify=False)
# print(response.json())
r = response.json()
tokens["8_10_rw"] = r['token']

for i in range(8, 11):
    res[i] = id_prefix + resource_ids[i]

requested_id = []
acl_set_policy2 = "consumer@iisc.ac.in can access example.com/test-category/" + resource_ids[
    11] + " for 1 month"
data = {"policy": acl_set_policy2}
# print(data)
response = requests.post(
    'https://localhost:8443/auth/v1/acl/append',
    headers=headers,
    data=json.dumps(data),
    cert=(PROVIDER_CERT_PATH, PROVIDER_KEY_PATH),
    verify=False)
# print(response.json())

requested_id.append({
    "id": id_prefix + resource_ids[11],
    "scopes": ["read", "write"]
})

data = {"request": requested_id}

response = requests.post(
    'https://localhost:8443/auth/v1/token',
    headers=headers,
    cert=(CONSUMER_CERT_PATH, CONSUMER_KEY_PATH),
    data=json.dumps(data),
    verify=False)
# print(response.json())
r = response.json()
tokens["11_rw"] = r['token']
res[11] = id_prefix + resource_ids[11]

requested_id = []
acl_set_policy3 = "consumer@iisc.ac.in can access example.com/test-category/" + resource_ids[
    12] + " for 1 month"
data = {"policy": acl_set_policy3}
# print(data)
response = requests.post(
    'https://localhost:8443/auth/v1/acl/append',
    headers=headers,
    data=json.dumps(data),
    cert=(PROVIDER_CERT_PATH, PROVIDER_KEY_PATH),
    verify=False)
# print(response.json())

requested_id.append({
    "id": id_prefix + resource_ids[12],
    "scopes": ["read", "write"]
})

data = {"request": requested_id}

response = requests.post(
    'https://localhost:8443/auth/v1/token',
    headers=headers,
    cert=(CONSUMER_CERT_PATH, CONSUMER_KEY_PATH),
    data=json.dumps(data),
    verify=False)
# print(response.json())
r = response.json()
tokens["12_rw"] = r['token']
res[12] = id_prefix + resource_ids[12]

# print(tokens)
# print(res)
sc_id = []
s_id = []
payload = {
    "id":
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
    "scroll_duration": "1000s",
    "size": 500,
    "geo_distance": {
        "coordinates": [82.9739, 25.3176],
        "distance": "10000m",

    }
}

url = VERMILLION_URL + SEARCH_ENDPOINT
response = requests.post(url, headers=headers, data=json.dumps(payload), verify=False)
r = response.json()
sc_id = r['scroll_id']
# print(sc_id)

params = (
    ('token', tokens["1_2_read_write"]),

)
payload = {
    "id": res[1],
    "scroll_duration": "1000s",
    "size": 500,
    "geo_distance": {
        "coordinates": [82.9739, 25.3176],
        "distance": "10000m",

    }
}
response = requests.post(url, params=params, headers=headers, data=json.dumps(payload), verify=False)
r = response.json()
s_id = r['scroll_id']
