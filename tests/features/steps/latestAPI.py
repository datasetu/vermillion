import requests
import urllib3
from behave import when
import json
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from auth_vars import res, tokens
from utils import generate_random_chars, get_request, post_files

VERMILLION_URL = 'https://localhost'
LATEST_ENDPOINT = '/latest'
headers = {
    'Content-Type': 'application/json',
}
url = VERMILLION_URL + LATEST_ENDPOINT

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


@when('The consumer publishes secured files')
def step_impl(context):
    params = (

        ("id", res[4]),
        ('token', tokens["2_5_write"]),

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


@when('A latest API query is initiated')
def step_impl(context):
    context.type = 'latest_search'
    params = (
        ("id", res[4]),
        ("token", tokens["2_5_write"]),

    )
    r = requests.get(url, headers=headers, params=params, verify=False)
    context.response = r.json()
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('A latest API query is initiated for public resource id')
def step_impl(context):
    context.type = 'latest_public'
    params = (
        ("id",
         "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public"),

    )
    r = requests.get(url, headers=headers, params=params, verify=False)
    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('A latest API query with invalid resource id')
def step_impl(context):
    params = (
        ("id", generate_random_chars()),
        ("token", tokens["master"]),

    )

    get_request(url, params, context)


@when('A latest API query is with empty resource id')
def step_impl(context):
    params = (
        ("id", ""),
        ("token", tokens["master"]),

    )

    get_request(url, params, context)


@when('A latest API query is without resource id')
def step_impl(context):
    params = (

        ("token", tokens["master"]),

    )

    get_request(url, params, context)


@when('A latest API query is without token')
def step_impl(context):
    params = (
        ("id", res[1]),

    )

    get_request(url, params, context)


@when('A latest API query is with empty token')
def step_impl(context):
    params = (
        ("id", res[1]),
        ("token", ""),

    )

    get_request(url, params, context)


@when('A latest API query is with invalid token')
def step_impl(context):
    params = (
        ("id", res[1]),
        ("token", generate_random_chars()),

    )

    get_request(url, params, context)


@when('A latest API query is with expired token')
def step_impl(context):
    params = (
        ("id", res[1]),
        ("token", tokens["6_7_read"]),

    )

    get_request(url, params, context)
