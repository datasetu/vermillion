import json
import requests
from behave import when
import urllib3
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from savetoken import *
VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'
PUBLISH_ENDPOINT = '/publish'

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

#XXX Secure-timeseries tests need definition here

@when('The consumer requests for a standalone authorised ID')
def step_imp(context):
 
    
    headers = {
    'Content-Type': 'application/json',
}

#    params = (
#    ('token', 'auth.local/consumer@iisc.ac.in/d5435c4d8a136674085218bffb8dca93'),

#    )

 #   data = '{ "id": "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/secure-ts", "time": { "start": "2021-01-01", "end": "2021-11-01" } }'

  #  r = requests.post(VERMILLION_URL+SEARCH_ENDPOINT, headers=headers, params=params, data=data, verify=False)
    
    params = (
    ('id', 'rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/secure-ts1'),
    ('token', t),
)

    data = '{"data": {"hello": "world"}}'

    r = requests.post(VERMILLION_URL + PUBLISH_ENDPOINT, headers=headers, params=params, data=data, verify=False)




    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)

@when('The consumer requests for a standalone authorised ID with invalid token')
def step_imp(context):


    headers = {
    'Content-Type': 'application/json',
}
    params = (
    ('id', 'rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/secure-ts1'),
    ('token', 'auth.local/consumer@iisc.ac.in/i0c24770faaeaf547f0f370cd3254ab90'),
)

    data = '{"data": {"hello": "world"}}'

    r = requests.post(VERMILLION_URL + PUBLISH_ENDPOINT, headers=headers, params=params, data=data, verify=False)



    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)


@when('The consumer requests for a standalone authorised ID with empty token')
def step_imp(context):


    headers = {
    'Content-Type': 'application/json',
}

    params = (
    ('id', 'rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/secure-ts1'),
    ('token', ''),
)

    data = '{"data": {"hello": "world"}}'

    r = requests.post(VERMILLION_URL + PUBLISH_ENDPOINT, headers=headers, params=params, data=data, verify=False)


    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)


@when('The consumer requests for a standalone authorised ID with invalid resource id')
def step_imp(context):


    headers = {
    'Content-Type': 'application/json',
}

    params = (
    ('id', 'rbccps.orghh/elpij096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/secure-ts1'),
    ('token', t),
)

    data = '{"data": {"hello": "world"}}'

    r = requests.post(VERMILLION_URL + PUBLISH_ENDPOINT, headers=headers, params=params, data=data, verify=False)



    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)

@when('The consumer requests for a standalone authorised ID with empty resource id')
def step_imp(context):


    headers = {
    'Content-Type': 'application/json',
}

    params = (
    ('id', ''),
    ('token', t),
)

    data = '{"data": {"hello": "world"}}'

    r = requests.post(VERMILLION_URL + PUBLISH_ENDPOINT, headers=headers, params=params, data=data, verify=False)


    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)

@when('The consumer requests for an unauthorised ID')
def step_imp(context):
     

    headers = {
    'Content-Type': 'application/json',
}

    params = (
    ('id', 'rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/secure-ts11'),
    ('token', t),
)

    data = '{"data": {"hello": "world"}}'

    r = requests.post(VERMILLION_URL + PUBLISH_ENDPOINT, headers=headers, params=params, data=data, verify=False)

   
    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)


@when('The consumer requests for multiple authorised IDs')
def step_imp(context):
     

    headers = {
    'Content-Type': 'application/json',
}

    params = (
    ('token', t),

    )
    data =  {
                "id": [
                        "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/secure-ts1", 
                        "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/secure-ts"
                    ], 
                "time": { 
                    "start": "2021-01-01", 
                    "end": "2021-11-01" 
                }
            }

    r = requests.post(VERMILLION_URL+SEARCH_ENDPOINT, headers=headers, params=params, data=json.dumps(data), verify=False)
    print(r.text)
    
    
    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)

@when('The consumer requests for multiple unauthorised IDs')
def step_imp(context):


    headers = {
    'Content-Type': 'application/json',
}

    params = (
    ('token', t),

    )
    data =  {
                "id": [
                        "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/secure-ts11",
                        "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/secure-ts"
                    ],
                "time": {
                    "start": "2021-01-01",
                    "end": "2021-11-01"
                }
            }

    r = requests.post(VERMILLION_URL+SEARCH_ENDPOINT, headers=headers, params=params, data=json.dumps(data), verify=False)
    print(r.text)

    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)

@when('The consumer requests for unauthorised IDs among authorised IDs')
def step_imp(context):


    headers = {
    'Content-Type': 'application/json',
}

    params = (
    ('token', t),

    )
    data =  {
                "id": [
                        "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/secure-ts12",
                        "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/secure-ts00"
                    ],
                "time": {
                    "start": "2021-01-01",
                    "end": "2021-11-01"
                }
            }

    r = requests.post(VERMILLION_URL+SEARCH_ENDPOINT, headers=headers, params=params, data=json.dumps(data), verify=False)
    print(r.text)

    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)




@then('The response should contain the secure timeseries data')
def step_impl(context):
    if not context.response:
        raise ValueError('Secure Timeseries data not found in response')
