import time

import requests
import urllib3

from behave import when
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from auth_vars import res, tokens
from utils import check_publish, check_search, generate_random_chars

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

@when('The consumer publishes data with a valid token')
def step_impl(context):
    params = (
        ('id', res[3]),
        ('token', tokens["master"])
    )

    data = '{"data": {"hello": "world"}}'

    check_publish(params, data, context)


@when('The consumer publishes data without data field in body')
def step_impl(context):
    params = (
        ('id', res[4]),
        ('token', tokens["master"]),
    )

    check_publish(params, "", context)


@when('The consumer publishes data with invalid json data')
def step_impl(context):
    params = (
        ('id', res[0]),
        ('token', tokens["master"]),
    )

    data = "True"

    check_publish(params, data, context)


@when('The consumer publishes data with invalid body fields')
def step_impl(context):
    params = (
        ('id', res[0]),
        ('token', tokens["master"]),
        ('item', "value"),
    )

    # data = '{"data": {"hello": "world"}}'

    xyz = 'testing invalid'

    check_publish(params, xyz, context)


@when('The consumer publishes data with an invalid token')
def step_impl(context):
    params = (
        ('id', res[0]),
        ('token', generate_random_chars()),
    )

    data = '{"data": {"hello": "world"}}'

    check_publish(params, data, context)


@when('The consumer publishes data with an empty token')
def step_impl(context):
    params = (
        ('id', res[0]),
        ('token', ''),
    )

    data = '{"data": {"hello": "world"}}'

    check_publish(params, data, context)


@when('The consumer publishes data without a body')
def step_impl(context):
    params = (
        ('id', res[0]),
        ('token', tokens["master"]),
    )

    # data = '{"data": {"hello": "world"}}'

    check_publish(params, "", context)


@when('The consumer publishes data when body is null')
def step_impl(context):
    params = (
        ('id', res[0]),
        ('token', tokens["master"]),
    )

    data = ''

    check_publish(params, data, context)


@when('The consumer publishes data with an invalid resource id')
def step_impl(context):
    params = (
        ('id', generate_random_chars() + ".public"),
        ('token', tokens["master"]),
    )

    data = '{"data": {"hello": "world"}}'

    check_publish(params, data, context)


@when('The consumer publishes data with an empty resource id')
def step_impl(context):
    params = (
        ('id', ''),
        ('token', tokens["master"]),
    )

    data = '{"data": {"hello": "world"}}'

    check_publish(params, data, context)


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
    time.sleep(5)
    check_search(params, data, context)


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

    check_search(params, data, context)


@when('The consumer requests for multiple authorised IDs')
def step_impl(context):
    params = (
        ('token', tokens["6_7_read"]),

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

    check_search(params, data, context)


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

    check_search(params, data, context)


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

    check_search(params, data, context)


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

    check_search(params, data, context)


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
    check_search("", data, context)
