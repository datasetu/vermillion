import time
import requests
import json
import urllib3
from behave import when
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from auth_vars import res, tokens
from utils import generate_random_chars, post_request

VERMILLION_URL = 'https://localhost'
PUBLISH_ENDPOINT = '/publish'
SEARCH_ENDPOINT = '/search'
headers = {
    'Content-Type': 'application/json',
}
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


@when('The consumer publishes data with a valid token')
def step_impl(context):
    params = (
        ('id', res[3]),
        ('token', tokens["master"])
    )

    data = '{"data": {"hello": "world"}}'
    url = VERMILLION_URL + PUBLISH_ENDPOINT
    post_request(url, params, data, context)


@when('The consumer publishes data with a valid token-2')
def step_impl(context):
    params = (
        ('id', res[2]),
        ('token', tokens["master"])
    )

    data = '{"data": {"hello": "india"}}'
    url = VERMILLION_URL + PUBLISH_ENDPOINT
    post_request(url, params, data, context)


@when('The consumer publishes data without data field in body')
def step_impl(context):
    params = (
        ('id', res[4]),
        ('token', tokens["master"]),
    )
    data = '{"dat": {"hello": "india"}}'
    url = VERMILLION_URL + PUBLISH_ENDPOINT
    post_request(url, params, data, context)

@when('The consumer publishes data with invalid json body')
def step_impl(context):
    params = (
        ('id', res[0]),
        ('token', tokens["master"]),
    )

    data = '{"data" {"hello": "world"'
    url = VERMILLION_URL + PUBLISH_ENDPOINT
    post_request(url, params, data, context)

@when('The consumer publishes data with invalid json data')
def step_impl(context):
    params = (
        ('id', res[0]),
        ('token', tokens["master"]),
    )

    data = '{"data": "True"}'
    url = VERMILLION_URL + PUBLISH_ENDPOINT
    post_request(url, params, data, context)


@when('The consumer publishes data with invalid body fields')
def step_impl(context):
    params = (
        ('id', res[0]),
        ('token', tokens["master"]),
        ('item', "value"),
    )

    data = '{"data": {"hello": "world"}, "hello":"world"}'
    url = VERMILLION_URL + PUBLISH_ENDPOINT
    post_request(url, params, data, context)


@when('The consumer publishes data with an invalid token')
def step_impl(context):
    params = (
        ('id', res[0]),
        ('token', generate_random_chars()),
    )

    data = '{"data": {"hello": "world"}}'
    url = VERMILLION_URL + PUBLISH_ENDPOINT
    post_request(url, params, data, context)


@when('The consumer publishes data with an empty token')
def step_impl(context):
    params = (
        ('id', res[0]),
        ('token', ''),
    )

    data = '{"data": {"hello": "world"}}'
    url = VERMILLION_URL + PUBLISH_ENDPOINT
    post_request(url, params, data, context)


@when('The consumer publishes data without a body')
def step_impl(context):
    params = (
        ('id', res[0]),
        ('token', tokens["master"]),
    )

    # data = '{"data": {"hello": "world"}}'
    url = VERMILLION_URL + PUBLISH_ENDPOINT
    post_request(url, params, "", context)


@when('The consumer publishes data when body is null')
def step_impl(context):
    params = (
        ('id', res[0]),
        ('token', tokens["master"]),
    )

    data = ''
    url = VERMILLION_URL + PUBLISH_ENDPOINT
    post_request(url, params, data, context)


@when('The consumer publishes data with an invalid resource id')
def step_impl(context):
    params = (
        ('id', generate_random_chars() + ".public"),
        ('token', tokens["master"]),
    )

    data = '{"data": {"hello": "world"}}'
    url = VERMILLION_URL + PUBLISH_ENDPOINT
    post_request(url, params, data, context)


@when('The consumer publishes data with an empty resource id')
def step_impl(context):
    params = (
        ('id', ''),
        ('token', tokens["master"]),
    )

    data = '{"data": {"hello": "world"}}'
    url = VERMILLION_URL + PUBLISH_ENDPOINT
    post_request(url, params, data, context)


@when('The consumer requests for a standalone authorised ID')
def step_impl(context):
    context.type = 'authorised_id'

    params = (
        ('token', tokens["master"]),

    )
    data = {
        "id":
            res[3],

        "time": {
            "start": "2021-01-01",
            "end": "2021-11-01"
        }
    }

    # Allow one second for es segement refresh
    time.sleep(1)
    url = VERMILLION_URL + SEARCH_ENDPOINT
    post_request(url, params, json.dumps(data), context)

@when('The consumer requests for an unauthorised ID')
def step_impl(context):
    params = (
        ('token', tokens["2_5_write"]),

    )
    data = {
        "id":

            res[6],

        "time": {
            "start": "2021-01-01",
            "end": "2021-11-01"
        }
    }
    url = VERMILLION_URL + SEARCH_ENDPOINT

    post_request(url, params, json.dumps(data), context)


@when('The consumer requests for multiple authorised IDs')
def step_impl(context):
    context.type = 'authorised_id_multiple'
    params = (
        ('token', tokens["master"]),

    )
    data = {
        "id": [
            res[2],
            res[3]
        ],
        "time": {
            "start": "2021-01-01",
            "end": "2021-11-01"
        }
    }
    time.sleep(1)
    url = VERMILLION_URL + SEARCH_ENDPOINT
    post_request(url, params, json.dumps(data), context)


@when('The consumer requests for multiple unauthorised IDs')
def step_impl(context):
    params = (
        ('token', tokens["2_5_write"]),

    )
    data = {
        "id": [

            res[6],
            res[7]
        ],
        "time": {
            "start": "2021-01-01",
            "end": "2021-11-01"
        }
    }
    url = VERMILLION_URL + SEARCH_ENDPOINT
    post_request(url, params, json.dumps(data), context)


@when('The consumer requests for unauthorised IDs among authorised IDs')
def step_impl(context):
    params = (
        ('token', tokens["2_5_write"]),

    )
    data = {
        "id": [
            res[0],

            res[7]
        ],
        "time": {
            "start": "2021-01-01",
            "end": "2021-11-01"
        }
    }
    url = VERMILLION_URL + SEARCH_ENDPOINT
    post_request(url, params, json.dumps(data), context)


@when('The consumer requests for a standalone authorised ID with invalid token')
def step_impl(context):
    params = (
        ('token', generate_random_chars()),

    )
    data = {
        "id":
            res[3],

        "time": {
            "start": "2021-01-01",
            "end": "2021-11-01"
        }
    }
    time.sleep(1)
    url = VERMILLION_URL + SEARCH_ENDPOINT
    post_request(url, params, json.dumps(data), context)


@when('The consumer requests for a standalone authorised ID with unauthorized token')
def step_impl(context):
    params = (
        ('token', tokens["6_7_read"]),

    )
    data = {
        "id":
            res[3],

        "time": {
            "start": "2021-01-01",
            "end": "2021-11-01"
        }
    }
    time.sleep(1)
    url = VERMILLION_URL + SEARCH_ENDPOINT
    post_request(url, params, json.dumps(data), context)


@when('The consumer requests for a standalone authorised ID without token')
def step_impl(context):
    data = {
        "id":
            res[3],

        "time": {
            "start": "2021-01-01",
            "end": "2021-11-01"
        }
    }
    time.sleep(1)
    url = VERMILLION_URL + SEARCH_ENDPOINT
    post_request(url, "", json.dumps(data), context)
