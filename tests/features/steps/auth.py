import json
import requests
from behave import when, then, step
import urllib3
from requests.packages.urllib3.exceptions import InsecureRequestWarning

VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'
AUTH_URL = 'https://localhost:8443'
SET_POLICY_ENDPOINT = '/auth/v1/acl/set'
REQUEST_TOKEN_ENDPOINT = '/auth/v1/token'
INTROSPECT_ENDPOINT = '/auth/v1/token/introspect'

PROVIDER_CERT_PATH = 'provider.pem'
PROVIDER_KEY_PATH = 'provider.key.pem'

CONSUMER_CERT_PATH = 'consumer.pem'
CONSUMER_KEY_PATH = 'consumer.key.pem'

RESOURCE_SERVER_CERT_PATH = 'resource-server.pem'
RESOURCE_SERVER_KEY_PATH = 'resource-server.key.pem'

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

