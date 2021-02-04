import json
import requests
from behave import when
import urllib3
from savetoken import *
from requests.packages.urllib3.exceptions import InsecureRequestWarning

VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'

PUBLISH_ENDPOINT= '/publish'


urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

#XXX Secure-files tests need definition here
@when('The consumer requests having a valid token')
def step_imp(context):
    params = (
    ('id', 'rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/secure-file'),
    ('token', t),
)

    files = {
    'file': ('sample.txt', open('sample.txt', 'rb')),
    'metadata': ('meta.json', open('meta.json', 'rb')),
}

    r = requests.post('https://localhost/publish', params=params, files=files, verify=False)
    
    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)

@when('The consumer requests having an invalid token')
def step_imp(context):
    params = (
    ('id', 'rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/secure-file'),
    ('token', 'auth.local/consumer@iisc.ac.in/ii6f015b9152e9e14208be1f092c47530c'),
)

    files = {
    'file': ('sample.txt', open('sample.txt', 'rb')),
    'metadata': ('meta.json', open('meta.json', 'rb')),
}

    r = requests.post('https://localhost/publish', params=params, files=files, verify=False)

    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)


@when('The consumer requests having an empty token')
def step_imp(context):
    params = (
    ('id', 'rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/secure-file'),
    ('token', ''),
)

    files = {
    'file': ('sample.txt', open('sample.txt', 'rb')),
    'metadata': ('meta.json', open('meta.json', 'rb')),
}

    r = requests.post('https://localhost/publish', params=params, files=files, verify=False)

    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)

@when('The consumer requests having an invalid id')
def step_imp(context):
    params = (
    ('id', 'rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/secure-file545'),
    ('token', t),
)

    files = {
    'file': ('sample.txt', open('sample.txt', 'rb')),
    'metadata': ('meta.json', open('meta.json', 'rb')),
}

    r = requests.post('https://localhost/publish', params=params, files=files, verify=False)

    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)

@when('The consumer requests having an empty id')
def step_imp(context):
    params = (
    ('id', ''),
    ('token', t),
)

    files = {
    'file': ('sample.txt', open('sample.txt', 'rb')),
    'metadata': ('meta.json', open('meta.json', 'rb')),
}

    r = requests.post('https://localhost/publish', params=params, files=files, verify=False)

    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)



@when('The consumer requests along with a valid token')
def step_imp(context):
    params = (
    ('id', 'rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/secure-file'),
    ('token', t),
)

    r = requests.get('https://localhost/download', params=params, verify=False)
    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)




@when('The consumer requests along with an invalid token')
def step_imp(context):
    params = (
    ('id', 'rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/secure-file'),
    ('token', 'dvdfdf'),
)

    r = requests.get('https://localhost/download', params=params, verify=False)
    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)

@when('The consumer requests along with an empty token')
def step_imp(context):
    params = (
    ('id', 'rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/secure-file'),
    ('token', ''),
)

    r = requests.get('https://localhost/download', params=params, verify=False)
    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)

@when('The consumer requests along with an invalid id')
def step_imp(context):
    params = (
    ('id', 'rbccps.org/e096b3abef24b99383d9bd28e9b8c8dvdfdfdf9cfd50be0b/example.com/test-category/secure-file'),
    ('token', t),
)

    r = requests.get('https://localhost/download', params=params, verify=False)
    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)

@when('The consumer requests along with an empty id')
def step_imp(context):
    params = (
    ('id', ''),
    ('token', t),
)

    r = requests.get('https://localhost/download', params=params, verify=False)
    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)


