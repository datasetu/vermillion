import json
import time
import requests
import urllib3
import os
from behave import when
from auth_vars import res, tokens
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from utils import post_files, generate_random_chars, get_request

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)
CONSUMER_CERT_PATH = 'consumer.pem'
CONSUMER_KEY_PATH = 'consumer.key.pem'
PROVIDER_CERT_PATH = 'provider.pem'
PROVIDER_KEY_PATH = 'provider.key.pem'

# XXX Secure-files tests need definition here
@when('The consumer publishes secure file with a valid token')
def step_impl(context):
    params = (
        ('id', res[8]),
        ('token', tokens["8_10_rw"]),
    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }
    time.sleep(3)
    post_files(params, files, context)


@when('The consumer publishes with a valid token(1)')
def step_impl(context):
    params = (

        ("id", res[9]),
        ('token', tokens["8_10_rw"]),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }
    time.sleep(3)
    post_files(params, files, context)


@when('The consumer publishes with a valid token(2)')
def step_impl(context):
    params = (

        ("id", res[10]),
        ('token', tokens["8_10_rw"]),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }
    time.sleep(3)
    post_files(params, files, context)


@when('The consumer publishes with a valid token(3)')
def step_impl(context):
    params = (

        ("id", res[11]),
        ('token', tokens["11_rw"]),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }
    time.sleep(3)
    post_files(params, files, context)


@when('The consumer downloads file by passing multiple resource ids and a token')
def step_impl(context):
    url = 'https://localhost/download?&token=' + tokens["8_10_rw"] + '&id=' + res[8] + ',' + res[10]
    get_request(url, "", context)


@when('The consumer downloads file by passing a valid reroute link')
def step_impl(context):
    param = tokens["8_10_rw"]
    url = 'https://localhost/consumer/' + param + '/'
    get_request(url, "", context)


@when('The consumer downloads file by passing only token for single auth id')
def step_impl(context):
    params = (
        ('token', tokens["11_rw"]),

    )
    url = 'https://localhost/download'
    get_request(url, params, context)


@when('The consumer downloads file by passing a valid reroute link for single authorised id')
def step_impl(context):
    param = tokens["11_rw"]

    r = requests.get('https://localhost/consumer/' + param + '/', verify=False)
    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer downloads file by passing only token')
def step_impl(context):
    params = (
        ('token', tokens["8_10_rw"]),

    )
    url = 'https://localhost/download'
    get_request(url, params, context)


@when('The consumer publishes secure file with a file and timeseries data')
def step_impl(context):
    params = (
        ('id', res[8]),
        ('token', tokens["8_10_rw"]),
    )
    data = {"data": {"hello": "world"}}
    headers = {'Content-type': 'multipart/form-data'}
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    r = requests.post('https://localhost/publish', data=data, params=params, files=files, verify=False, headers=headers)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes secure file with an invalid token')
def step_impl(context):
    params = (
        ('id', res[9]),
        ('token', generate_random_chars()),
    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }
    post_files(params, files, context)


@when('The consumer publishes secure file with an empty token')
def step_impl(context):
    params = (
        ('id', res[9]),
        ('token', ''),
    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }
    post_files(params, files, context)


@when('The consumer publishes secure file with an invalid resource id')
def step_impl(context):
    params = (
        ('id', generate_random_chars() + ".public"),
        ('token', tokens["8_10_rw"]),
    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }
    post_files(params, files, context)


@when('The consumer publishes secure file with an empty resource id')
def step_impl(context):
    params = (
        ('id', ''),
        ('token', tokens["8_10_rw"]),
    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }
    post_files(params, files, context)

@when('The consumer publishes secure file with an expired token')
# This is a test case to test if the tokens which are expired can be used to publish or not
#  First a policy is set for a resource id for 1sec; Next a token for the same is fetched
#  Publish a secure file with the fetched token
def step_impl(context):


    headers = {
        'content-type': 'application/json',
        'Host': 'auth.local',
}
    requested_id2=[]
    acl_set_policy = "consumer@iisc.ac.in can access example.com/test-category/tokenexpiry for 1 second"
    data = {"policy": acl_set_policy}
    # print(data)
    response1 = requests.post(
        'https://localhost:8443/auth/v1/acl/set',
        headers=headers,
        data=json.dumps(data),
        cert=(PROVIDER_CERT_PATH, PROVIDER_KEY_PATH),
        verify=False)
    # print(response.json())

    requested_id2.append({
        "id": "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/tokenexpiry",
        "scopes": ["read", "write"]
    })

    data = {"request": requested_id2}
    # print(data)
    response1 = requests.post(
        'https://localhost:8443/auth/v1/token',
        headers=headers,
        cert=(CONSUMER_CERT_PATH, CONSUMER_KEY_PATH),
        data=json.dumps(data),
        verify=False)
    # print(response.json())
    r = response1.json()
    tokens["16_rw"] = r['token']
    res[16] = "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/tokenexpiry"
    params= (
        ('id',res[16]),
        ('token',tokens["16_rw"])
    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }
    time.sleep(3)
    post_files(params, files, context)

@when('The consumer downloads file by passing a valid token')
def step_impl(context):
    params = (
        ('id',
         res[8]
         ),
        ('token', tokens["8_10_rw"]),
    )

    url = 'https://localhost/download'
    get_request(url, params, context)


@when('The consumer downloads file by passing an invalid token')
def step_impl(context):
    params = (
        ('id', res[8]),
        ('token', generate_random_chars()),
    )
    url = 'https://localhost/download'
    get_request(url, params, context)


@when('The consumer downloads file by passing an empty token')
def step_impl(context):
    params = (
        ('id', res[8]),
        ('token', ''),
    )
    url = 'https://localhost/download'
    get_request(url, params, context)


@when('The consumer downloads file without passing token')
def step_impl(context):
    params = (
        ('id', res[8]),
    )
    url = 'https://localhost/download'
    get_request(url, params, context)


@when('The consumer downloads file by passing an invalid resource id')
def step_impl(context):
    params = (
        ('id', generate_random_chars()),
        ('token', tokens["8_10_rw"]),

    )
    url = 'https://localhost/download'
    get_request(url, params, context)


@when('The consumer downloads file by passing an empty resource id')
def step_impl(context):
    params = (
        ('id', ''),
        ('token', tokens["8_10_rw"]),
    )
    url = 'https://localhost/download'
    get_request(url, params, context)


@when('The consumer downloads file by passing public id and token')
def step_impl(context):
    params = (
        ('id', res[8] + ".public"),
        ('token', tokens["8_10_rw"]),
    )
    url = 'https://localhost/download'
    get_request(url, params, context)


@when('The consumer downloads file by passing only token and requested id is not present')
def step_impl(context):
    params = (
        ('token', tokens["12_rw"]),
    )
    url = 'https://localhost/download'
    get_request(url, params, context)


@when('The consumer downloads file by passing id,token and requested id is not present')
def step_impl(context):
    params = (
        ('token', tokens["12_rw"]),
        ('id', res[12]),
    )

    url = 'https://localhost/download'
    get_request(url, params, context)


@when('The consumer publishes with a valid token and could not move files')
def step_impl(context):
    params = (
        ('id', res[8]),
        ('token', tokens["8_10_rw"]),
    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }
    os.chmod("../setup/provider", 0o444)
    post_files(params, files, context)


@when('The consumer publishes secure file with a valid token for 15 secs')
def step_impl(context):
    params = (
        ('id', res[14]),
        ('token', tokens["14_rw"]),
    )

    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    post_files(params, files, context)

    time.sleep(30)


@when('The consumer downloads with expired token')
def step_impl(context):
    params = (
        ('id', res[14]),
        ('token', tokens["14_rw"]),

    )
    url = 'https://localhost/download'
    get_request(url,params, context)

@when('The consumer downloads with expired token via reroute link')
def step_impl(context):
    url = 'https://localhost/consumer/' + tokens['14_rw']
    get_request(url, None, context)