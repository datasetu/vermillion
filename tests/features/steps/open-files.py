import json
import requests
from behave import when
import re
import urllib3
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from savetoken import *
VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'
PUBLISH_ENDPOINT= '/publish'


urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

#XXX Open-files tests need definition here


@when('The consumer requests with a valid token')
def step_imp(context):
    payload= (
           ("id","rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource.public"
            ),
    ('token', t),

)
    files = {
    'file': ('sample.txt', open('sample.txt', 'rb')),
    'metadata': ('meta.json', open('meta.json', 'rb')),
    }



    r=requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                    data=payload,
                    files=files,
                    verify=False)

    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)


@when('The consumer requests with invalid payload id')
def step_imp(context):
    payload= (
           ("id","rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource.publicggg"
            ),
    ('token', t),

)
    files = {
    'file': ('sample.txt', open('sample.txt', 'rb')),
    'metadata': ('meta.json', open('meta.json', 'rb')),
    }



    r=requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                    data=payload,
                    files=files,
                    verify=False)

    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)

@when('The consumer requests with empty payload id')
def step_imp(context):
    payload= (
           ("id",""
            ),
    ('token', t),

)
    files = {
    'file': ('sample.txt', open('sample.txt', 'rb')),
    'metadata': ('meta.json', open('meta.json', 'rb')),
    }



    r=requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                    data=payload,
                    files=files,
                    verify=False)

    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)





@when('The consumer requests with invalid payload token')
def step_imp(context):
    payload= (
           ("id","rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource.public"
            ),
    ('token', 'hsdbhsd'),

)
    files = {
    'file': ('sample.txt', open('sample.txt', 'rb')),
    'metadata': ('meta.json', open('meta.json', 'rb')),
    }



    r=requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                    data=payload,
                    files=files,
                    verify=False)

    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)

@when('The consumer requests with empty payload token')
def step_imp(context):
    payload= (
           ("id","rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource.public"
            ),
    ('token', ''),

)
    files = {
    'file': ('sample.txt', open('sample.txt', 'rb')),
    'metadata': ('meta.json', open('meta.json', 'rb')),
    }



    r=requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                    data=payload,
                    files=files,
                    verify=False)

    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)

@when('The consumer requests by removing file form parameter')
def step_imp(context):
    payload= (
           ("id","rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource.public"
            ),
    ('token', 'auth.local/consumer@iisc.ac.in/b3760ba7bef7b69ff7a8725ace94debd'),

)
    files = {
    #'file': ('sample.txt', open('sample.txt', 'rb')),
    'metadata': ('meta.json', open('meta.json', 'rb')),
    }



    r=requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                    data=payload,
                    files=files,
                    verify=False)

    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)

@when('The consumer requests by removing metadata form parameter')
def step_imp(context):
    payload= (
           ("id","rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource.public"
            ),
    ('token', 'auth.local/consumer@iisc.ac.in/b3760ba7bef7b69ff7a8725ace94debd'),

)
    files = {
    'file': ('sample.txt', open('sample.txt', 'rb')),
    #'metadata': ('meta.json', open('meta.json', 'rb')),
    }



    r=requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                    data=payload,
                    files=files,
                    verify=False)

    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)


@when('The consumer requests by using extraneous form parameter')
def step_imp(context):
    payload= (
           ("id","rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource.public"
            ),
    ('token', 'auth.local/consumer@iisc.ac.in/b3760ba7bef7b69ff7a8725ace94debd'),

)
    files = {
    'abc': ('samplecsv.csv', open('samplecsv.csv', 'rb')),
    'efg': ('samplepdf.pdf', open('samplepdf.pdf', 'rb')),
    }



    r=requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                    data=payload,
                    files=files,
                    verify=False)

    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)



@when('The consumer requests with empty form parameter')
def step_imp(context):
    payload= (
           ("id","rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource.public"
            ),
    ('token', 'auth.local/consumer@iisc.ac.in/b3760ba7bef7b69ff7a8725ace94debd'),

)
    files = {
    #'file': ('sample.txt', open('sample.txt', 'rb')),
    #'metadata': ('meta.json', open('meta.json', 'rb')),
    }



    r=requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                    data=payload,
                    files=files,
                    verify=False)

    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)


                    
