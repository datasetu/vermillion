import json

import requests
import urllib3

from behave import when
from token_dbq import res, tokens
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from utils import post_files, generate_random_chars, download_query

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


# XXX Secure-files tests need definition here
@when('The consumer publishes secure file data with a valid token')
def step_impl(context):
    params = (
        ('id', res[13]),
        ('token', tokens["13_rw"]),
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


@when('The consumer downloads by query with a valid token')
def step_impl(context):
    params = (
        ('id', res[13]),
        ('token', tokens["13_rw"]),
        ('hello', 'world'),
    )

    download_query(params, context)


@when('The consumer downloads by query with empty token')
def step_impl(context):
    params = (
        ('id', res[13]),
        ('token', ''),
        ('hello', 'world'),
    )

    download_query(params, context)


@when('The consumer downloads by query without a token')
def step_impl(context):
    params = (
        ('id', res[13]),

        ('hello', 'world'),
    )

    download_query(params, context)


@when('The consumer downloads by query with empty id')
def step_impl(context):
    params = (
        ('id', ''),
        ('token', tokens["13_rw"]),
        ('hello', 'world'),
    )

    download_query(params, context)


# @when('The consumer downloads by query without id')
# def step_impl(context):
#     params = (
#
#         ('token', tokens["8_10_rw"]),
#         ('hello', 'world'),
#     )
#
#     download_query(params, context)


@when('The consumer downloads by query without query parameters')
def step_impl(context):
    params = (
        ('id', res[13]),
        ('token', tokens["13_rw"]),

    )

    download_query(params, context)


@when('The consumer downloads by query with invalid token')
def step_impl(context):
    params = (
        ('id', res[13]),
        ('token', generate_random_chars()),
        ('hello', 'world'),
    )

    download_query(params, context)


@when('The consumer downloads by query with invalid id')
def step_impl(context):
    params = (
        ('id', generate_random_chars()),
        ('token', tokens["13_rw"]),
        ('hello', 'world'),
    )

    download_query(params, context)

@when('The consumer downloads by query with invalid query parameter')
def step_impl(context):
    params = (
        ('id', res[13]),
        ('token', tokens["13_rw"]),
        (generate_random_chars(), generate_random_chars()),
    )

    download_query(params, context)