from behave import when
import os
import glob
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from auth_vars import urllib3, generate_random_chars, res, tokens, id_prefix
import requests

VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'
PUBLISH_ENDPOINT = '/publish'

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


# XXX Open-files tests need definition here


@when('The consumer publishes with a valid token')
def step_impl(context):
    payload = (

        ("id", res[0]),
        ('token', tokens["master"]),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    r = requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                      data=payload,
                      files=files,
                      verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes without resource id')
def step_impl(context):
    payload = (

        ('token', tokens["master"]),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    r = requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                      data=payload,
                      files=files,
                      verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes without token')
def step_impl(context):
    payload = (

        ("id", res[0]),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    r = requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                      data=payload,
                      files=files,
                      verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes with invalid resource id')
def step_impl(context):
    payload = (

        ("id", id_prefix + generate_random_chars() + ".public"),
        ('token', tokens["master"]),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    r = requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                      data=payload,
                      files=files,
                      verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes with empty resource id')
def step_impl(context):
    payload = (

        ("id", ""),
        ('token', tokens["master"]),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    r = requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                      data=payload,
                      files=files,
                      verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes with invalid token')
def step_impl(context):
    payload = (

        ("id", res[0]),
        ('token', generate_random_chars()),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    r = requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                      data=payload,
                      files=files,
                      verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes with empty token')
def step_impl(context):
    payload = (

        ("id", res[0]),
        ('token', ''),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    r = requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                      data=payload,
                      files=files,
                      verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes by removing file form parameter')
def step_impl(context):
    payload = (

        ("id", res[0]),
        ('token', tokens["master"]),
    )

    files = {
        # 'file': ('sample.txt', open('sample.txt', 'rb')),

        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    r = requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                      data=payload,
                      files=files,
                      verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes with invalid json meta file')
def step_impl(context):
    payload = (

        ("id", res[0]),
        ('token', tokens["master"]),
    )

    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),

        'metadata': ('invalidmeta.json', open('invalidmeta.json', 'rb')),
    }

    r = requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                      data=payload,
                      files=files,
                      verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes by removing metadata form parameter')
def step_impl(context):
    payload = (

        ("id", res[1]),
        ('token', tokens["master"]),
    )

    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        # 'metadata': ('meta.json', open('meta.json', 'rb')),

    }

    r = requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                      data=payload,
                      files=files,
                      verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes by using extraneous form parameter')
def step_impl(context):
    payload = (

        ("id", res[0]),
        ('token', tokens["master"]),

    )
    files = {
        'abc': ('samplecsv.csv', open('samplecsv.csv', 'rb')),
        'efg': ('samplepdf.pdf', open('samplepdf.pdf', 'rb')),
    }
    # This part of code removes the files present in the file-uploads folder that existed previously
    fil = glob.glob('../api-server/file-uploads/*')
    for f in fil:
        os.remove(f)

    r = requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                      data=payload,
                      files=files,
                      verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes with empty form parameter')
def step_impl(context):
    payload = (
        ("id", res[0]
         ),
        ('token', tokens["master"]),

    )
    files = {
        # 'file': ('sample.txt', open('sample.txt', 'rb')),
        # 'metadata': ('meta.json', open('meta.json', 'rb')),

    }

    r = requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                      data=payload,
                      files=files,
                      verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes with more than 2 form parameters')
def step_impl(context):
    payload = (
        ("id", res[0]),
        ('token', tokens["master"]),
    )

    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
        'fille': ('samplecsv.csv', open('samplecsv.csv', 'rb')),
        'fie': ('samplepdf.pdf', open('samplepdf.pdf', 'rb'))

    }

    r = requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                      data=payload,
                      files=files,
                      verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer downloads the file')
def step_impl(context):
    urd = 'https://localhost/provider/public/'
    r = requests.get(url=urd + res[0], verify=False)
    open('test-resource.public', 'w').write('This is the downloaded file')
    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)
