import json
import requests
from behave import given, when, then, step
import urllib3
from requests.packages.urllib3.exceptions import InsecureRequestWarning

VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'
AUTH_URL = 'https://localhost:8443'
SET_POLICY_ENDPOINT = '/auth/v1/acl/set'
REQUEST_TOKEN_ENDPOINT = '/auth/v1/token'
INTROSPECT_ENDPOINT = '/auth/v1/token/introspect'

PROVIDER_CERT_PATH = '../datasetu-ca/provider/provider.pem'
PROVIDER_KEY_PATH = '../datasetu-ca/provider/provider.key.pem'

CONSUMER_CERT_PATH = '../datasetu-ca/consumer/consumer.pem'
CONSUMER_KEY_PATH = '../datasetu-ca/consumer/consumer.key.pem'

RESOURCE_SERVER_CERT_PATH = '../datasetu-ca/resource-server/resource-server.pem'
RESOURCE_SERVER_KEY_PATH = '../datasetu-ca/resource-server/resource-server.key.pem'

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


@when('The provider sets rules in the auth server')
def step_impl(context):
    context.type = 'set_rules'
    payload = {
        "policy":
        "consumer@iisc.ac.in can access example.com/test-category/test-resource-1 for 1 month;consumer@iisc.ac.in can access example.com/test-category/test-resource-2 for 1 month;consumer@iisc.ac.in can access example.com/test-category/test-resource-3 for 1 month;consumer@iisc.ac.in can access example.com/test-category/test-resource.public for 1 month if scope = write"
    }

    r = requests.post(url=AUTH_URL + SET_POLICY_ENDPOINT,
                      headers={
                          'content-type': 'application/json',
                          'host': 'auth.local'
                      },
                      data=json.dumps(payload),
                      cert=(PROVIDER_CERT_PATH, PROVIDER_KEY_PATH),
                      verify=False)

    print('hello')

    context.response = r.json()
    context.status_code = r.status_code

    print(context.status_code, context.response)


@when('The consumer requests for a token')
def step_impl(context):
    context.type = 'token_request'
    payload = {
        "request": [{
            "id":
            "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource-1"
        }, {
            "id":
            "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource-2"
        }, {
            "id":
            "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource-3"
        }, {
            "id":
            "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource.public",
            "scope": "write"
        }]
    }

    r = requests.post(url=AUTH_URL + REQUEST_TOKEN_ENDPOINT,
                      headers={
                          'content-type': 'application/json',
                          'host': 'auth.local'
                      },
                      data=json.dumps(payload),
                      cert=(CONSUMER_CERT_PATH, CONSUMER_KEY_PATH),
                      verify=False)

    context.response = r.json()
    context.status_code = r.status_code
    context.token = context.response['token']

@then('The response should contain an auth token')
def step_impl(context):
    assert context.response['token']

@then('Introspect should succeed')
def step_impl(context):

    context.type = 'token_introspect'
    payload = {'token': context.token}

    r = requests.post(url=AUTH_URL + INTROSPECT_ENDPOINT,
                      headers={
                          'content-type': 'application/json',
                          'host': 'auth.local'
                      },
                      data=json.dumps(payload),
                      cert=(RESOURCE_SERVER_CERT_PATH,
                            RESOURCE_SERVER_KEY_PATH),
                      verify=False)

    context.response = r.json()
    context.status_code = r.status_code

    expected_response = {
        "consumer":
        "consumer@iisc.ac.in",
        "request": [{
            "id":
            "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource-1",
            "apis": ["/*"],
            "body": None,
            "scopes": ["read"],
            "methods": ["*"],
            "environments": ["*"]
        }, {
            "id":
            "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource-2",
            "apis": ["/*"],
            "body": None,
            "scopes": ["read"],
            "methods": ["*"],
            "environments": ["*"]
        }, {
            "id":
            "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource-3",
            "apis": ["/*"],
            "body": None,
            "scopes": ["read"],
            "methods": ["*"],
            "environments": ["*"]
        }, {
            "id":
            "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource.public",
            "apis": ["/*"],
            "body": None,
            "scopes": ["write"],
            "methods": ["*"],
            "environments": ["*"]
        }],
        "consumer-certificate-class": 2
    }


    context.response.pop('expiry', None)

    assert ordered(expected_response) == ordered(context.response)


def ordered(obj):
    if isinstance(obj, dict):
        return sorted((k, ordered(v)) for k, v in obj.items())
    if isinstance(obj, list):
        return sorted(ordered(x) for x in obj)
    else:
        return obj
