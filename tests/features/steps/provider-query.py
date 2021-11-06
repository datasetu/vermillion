import json
import time
import requests
import urllib3

from behave import when
from auth_vars import res, tokens
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from utils import post_files, generate_random_chars, download_query, get_request
url='https://localhost/providerByQuery'
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


# XXX Secure-files tests need definition here
@when('The consumer publishes public file data with valid token')
def step_impl(context):
    params = (
        ('id', res[0]),
        ('token', tokens["master"]),
    )
    # f = open("sample.txt", "w")
    # f.write("hi, welcome to datasetu!")
    # data = {"hello": "world"}
    # with open('meta.json', 'w') as f:
    #     json.dump(data, f)
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    post_files(params, files, context)

    time.sleep(4)


@when('The consumer downloads public file by query')
def step_impl(context):
    params = (
        ('id', res[0]),
        ('hello', 'world'),
    )

    download_query(url, params, context)


@when('The consumer downloads public file by query without id')
def step_impl(context):
    params = (

        ('hello', 'world'),
    )

    download_query(url, params, context)


@when('The consumer downloads public file by query without query')
def step_impl(context):
    params = (
        ('id', res[0]),

    )

    download_query(url, params, context)

@when('The consumer downloads public file by query with invalid query')
def step_impl(context):
    params = (
        ('id', res[0]),
        (generate_random_chars(), generate_random_chars()),
    )

    download_query(url, params, context)


@when('The consumer downloads secure file by query')
def step_impl(context):
    params = (
        ('id', res[8]),
        ('hello', 'world'),
    )

    download_query(url, params, context)
