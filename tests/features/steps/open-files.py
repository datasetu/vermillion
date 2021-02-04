import json
import requests
from behave import when
import urllib3
from requests.packages.urllib3.exceptions import InsecureRequestWarning

VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'
PUBLISH_ENDPOINT= '/publish'


FilePath1='../datasetu-ca/consumer/sample.txt'
FilePath2='../datasetu-ca/consumer/meta.json'

FilePath3='../datasetu-ca/consumer/samplecsv.csv'
FilePath4='../datasetu-ca/consumer/samplepdf.pdf'

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

#XXX Open-files tests need definition here

@when('The consumer requests token')
def step_imp(context):
    payload= (
           ("id","rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource.public" 
            ),
    ('token', 'auth.local/consumer@iisc.ac.in/b3760ba7bef7b69ff7a8725ace94debd'),

)
    files = {
    'file': ('sample1.txt', open(FilePath1, 'rb')),
    'metadata': ('meta1.json', open(FilePath2, 'rb')),
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
           ("id","kjghiushoi soishgoishogi hsoh oioi soighoishg"
            ),
    ('token', 'auth.local/consumer@iisc.ac.in/b3760ba7bef7b69ff7a8725ace94debd'),

)
    files = {
    'file': ('sample.txt', open(FilePath1, 'rb')),
    'metadata': ('meta.json', open(FilePath2, 'rb')),
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
    ('token', 'auth.local/consumer@iisc.ac.in/b3760ba7bef7b69ff7a8725ace94debd'),

)
    files = {
    'file': ('sample.txt', open(FilePath1, 'rb')),
    'metadata': ('meta.json', open(FilePath2, 'rb')),
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
    ('token', 'ufsghushfus shfguhsoig sohg ohs gsh gshgouusg hosi9873598735895389'),

)
    files = {
    'file': ('sample.txt', open(FilePath1, 'rb')),
    'metadata': ('meta.json', open(FilePath2, 'rb')),
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
    'file': ('sample.txt', open(FilePath1, 'rb')),
    'metadata': ('meta.json', open(FilePath2, 'rb')),
    }



    r=requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                    data=payload,
                    files=files,
                    verify=False)

    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)

@when('The consumer requests with invalid payload')
def step_imp(context):
    payload= (
           ("id","kjfghkushf shgshgkhsgk sghlslhg lls"
            ),
    ('token', 'kjhfg jkhskghksj hk skfgsk gf ksf '),

)
    files = {
    'file': ('sample.txt', open(FilePath1, 'rb')),
    'metadata': ('meta.json', open(FilePath2, 'rb')),
    }



    r=requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                    data=payload,
                    files=files,
                    verify=False)

    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)

@when('The consumer requests with empty payload')
def step_imp(context):
    payload= (
           #("id","rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource.public"
            #),
    #('token', ''),

)
    files = {
    'file': ('sample.txt', open(FilePath1, 'rb')),
    'metadata': ('meta.json', open(FilePath2, 'rb')),
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
    #'file': ('sample.txt', open(FilePath1, 'rb')),
    'metadata': ('meta.json', open(FilePath2, 'rb')),
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
    'file': ('sample.txt', open(FilePath1, 'rb')),
    #'metadata': ('meta.json', open(FilePath2, 'rb')),
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
    'abc': ('samplecsv.csv', open(FilePath3, 'rb')),
    'efg': ('samplepdf.pdf', open(FilePath4, 'rb')),
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
    #'file': ('sample.txt', open(FilePath1, 'rb')),
    #'metadata': ('meta.json', open(FilePath2, 'rb')),
    }



    r=requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                    data=payload,
                    files=files,
                    verify=False)

    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)

                    
