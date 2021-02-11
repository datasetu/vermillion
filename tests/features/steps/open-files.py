import json
import requests
from behave import when
import os
import glob
import urllib3
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from auth_vars import *
VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'
PUBLISH_ENDPOINT= '/publish'


urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

#XXX Open-files tests need definition here


@when('The consumer publishes with a valid token')
def step_imp(context):
    payload= (
           ("id",res[0]
            ),
    ('token', tokens["master"]),

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


@when('The consumer publishes with invalid resource id')
def step_imp(context):
    payload= (
           ("id",id_prefix + generate_random_chars()
            ),
    ('token', tokens["master"]),

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

@when('The consumer publishes with empty resource id')
def step_imp(context):
    payload= (
           ("id",""
            ),
    ('token', tokens["master"]),

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





@when('The consumer publishes with invalid token')
def step_imp(context):
    payload= (
           ("id", res[0]
            ),
    ('token',generate_random_chars() ),

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

@when('The consumer publishes with empty token')
def step_imp(context):
    payload= (
           ("id",res[0]
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

@when('The consumer publishes by removing file form parameter')
def step_imp(context):
    payload= (
           ("id",res[0]
            ),
    ('token', tokens["master"]),

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

@when('The consumer publishes by removing metadata form parameter')
def step_imp(context):
    payload= (
           ("id",res[0]
            ),
    ('token', tokens["master"]),

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


@when('The consumer publishes by using extraneous form parameter')
def step_imp(context):
    payload= (
           ("id",res[0]
            ),
    ('token', tokens["master"]),

)
    files = {
    'abc': ('samplecsv.csv', open('samplecsv.csv', 'rb')),
    'efg': ('samplepdf.pdf', open('samplepdf.pdf', 'rb')),
    }

    
    fil=glob.glob('../api-server/file-uploads/*')
    
    for f in fil:
        os.remove(f)
    

    r=requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                    data=payload,
                    files=files,
                    verify=False)

    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)

@then('The uploaded files are deleted')
def step_imp(context):
    dirl= os.listdir('../api-server/file-uploads/')
    if(len(dirl)!=0):
        raise ValueError('Files are not deleted')
        
   
@when('The consumer publishes with empty form parameter')
def step_imp(context):
    payload= (
           ("id",res[0]
            ),
    ('token', tokens["master"]),

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

@when('The consumer publishes with more than 2 form parameters')
def step_imp(context):
    payload= (
           ("id",res[0]
            ),
    ('token', tokens["master"]),

)
    files = {
    'file': ('sample.txt', open('sample.txt', 'rb')),
    'metadata': ('meta.json', open('meta.json', 'rb')),
    'fille': ('samplecsv.csv', open('samplecsv.csv', 'rb')),
    'fie': ('samplepdf.pdf', open('samplepdf.pdf', 'rb'))

    }



    r=requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                    data=payload,
                    files=files,
                    verify=False)

    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)

@when('The consumer downloads the file')
def step_imp(context):
    urd='https://localhost/provider/public/'
    r = requests.get(url=urd+res[0], verify=False)                  
    context.response= r
    context.status_code=r.status_code
    print(context.status_code,context.response)
@then('The expected file is returned')
def step_imp(context):
    with open('download_open_file','wb') as f:
        f.write(r.content)

    if not os.path.exists("download_open_file"):
        raise ValueError("File doesnt exist")
    
        
