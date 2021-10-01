import json
import time

import requests
import urllib3

from behave import when
from auth_vars import res, tokens
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from utils import post_files, generate_random_chars, download_query, get_request

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


# XXX Secure-files tests need definition here
@when('The consumer publishes secure file data with a valid token')
def step_impl(context):
    params = (
        ('id', res[8]),
        ('token', tokens["8_10_rw"]),
    )
    f = open("sample.txt", "w")
    f.write("hi, welcome to datasetu!")
    data = {"hello": "world"}
    with open('meta.json', 'w') as f:
        json.dump(data, f)
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    post_files(params, files, context)

    time.sleep(3)


@when('The consumer downloads by query with a valid token')
def step_impl(context):
    params = (
        ('id', res[8]),
        ('token', tokens["8_10_rw"]),
        ('hello', 'world'),
    )

    download_query(params, context)


@when('The consumer downloads by query with empty token')
def step_impl(context):
    params = (
        ('id', res[8]),
        ('token', ''),
        ('hello', 'world'),
    )

    download_query(params, context)


@when('The consumer downloads by query without a token')
def step_impl(context):
    params = (
        ('id', res[8]),

        ('hello', 'world'),
    )

    download_query(params, context)


@when('The consumer downloads by query with empty id')
def step_impl(context):
    params = (
        ('id', ''),
        ('token', tokens["8_10_rw"]),
        ('hello', 'world'),
    )

    download_query(params, context)


@when('The consumer downloads by query without id')
def step_impl(context):
    params = (

        ('token', tokens["8_10_rw"]),
        ('hello', 'world'),
    )

    download_query(params, context)


@when('The consumer downloads by query without query parameters')
def step_impl(context):
    params = (
        ('id', res[8]),
        ('token', tokens["8_10_rw"]),

    )

    download_query(params, context)


@when('The consumer downloads by query with invalid token')
def step_impl(context):
    params = (
        ('id', res[8]),
        ('token', generate_random_chars()),
        ('hello', 'world'),
    )

    download_query(params, context)


@when('The consumer downloads by query with invalid id')
def step_impl(context):
    params = (
        ('id', generate_random_chars()),
        ('token', tokens["8_10_rw"]),
        ('hello', 'world'),
    )

    download_query(params, context)


@when('The consumer downloads by query with invalid query parameter')
def step_impl(context):
    params = (
        ('id', res[8]),
        ('token', tokens["8_10_rw"]),
        (generate_random_chars(), generate_random_chars()),
    )

    download_query(params, context)


@when('The consumer downloads by query with unauthorized id')
def step_impl(context):
    params = (
        ('id', res[9]),
        ('token', tokens["8_10_rw"]),
        ('hello', 'world'),
    )

    download_query(params, context)


@when('The consumer publishes secure file data with a valid token for 20secs')
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

    time.sleep(22)


@when('The consumer downloads by query with expired token')
def step_impl(context):
    params = (
        ('id', res[14]),
        ('token', tokens["14_rw"]),
        ('hello', 'world'),
    )

    download_query(params, context)


@when('The consumer downloads by query with expired token via reroute link')
def step_impl(context):
    url = 'https://localhost/consumer/' + tokens['14_rw']
    get_request(url, None, context)
