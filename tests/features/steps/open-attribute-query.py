import json

import requests
import urllib3
from behave import when
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from utils import check_search, generate_random_chars

VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


@when('The attribute value query body is empty')
def step_impl(context):
    payload = {}

    check_search("", payload, context)


@when('The attribute value query body is invalid')
def step_impl(context):
    payload = {generate_random_chars(): generate_random_chars()}

    check_search("", payload, context)


@when('The attribute value query resource id is empty')
def step_impl(context):
    payload = {"id": "", "attribute": {"term": "speed", "min": 30, "max": 50}}

    check_search("", payload, context)


@when('The attribute value query resource id is invalid')
def step_impl(context):
    payload = {
        "id": generate_random_chars() + ".public",
        "attribute": {
            "term": "speed",
            "min": 30,
            "max": 50
        }
    }

    check_search("", payload, context)


@when('The attribute value query attributes are empty')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": "",
            "min": "",
            "max": ""
        }
    }

    check_search("", payload, context)


@when('The attribute value query payload has only resource id')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public"
    }

    check_search("", payload, context)


@when('The attribute value query with min value greater than max')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": "speed",
            "min": 100,
            "max": 0
        }
    }

    check_search("", payload, context)


@when('The attribute value query attributes are invalid')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": generate_random_chars(),
            "min": generate_random_chars(),
            "max": generate_random_chars()
        }
    }

    check_search("", payload, context)


@when('The attribute value query without term value')
def step_impl(context):
    context.type = 'attribute-value'

    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {

            "min": 30,
            "max": 50
        }
    }

    check_search("", payload, context)


@when('The attribute value query with empty term value')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": "",
            "min": 30,
            "max": 50
        }
    }

    check_search("", payload, context)


@when('The attribute value query with invalid term value')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": 0 + 1,
            "min": 30,
            "max": 50
        }
    }

    check_search("", payload, context)


@when('The attribute value query without min value')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": "speed",
            "max": 30,

        }
    }

    check_search("", payload, context)


@when('The attribute value query without max value')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": "speed",
            "min": 30,
        }
    }

    check_search("", payload, context)


@when('The attribute value query without min and max values')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": "speed",

        }
    }

    check_search("", payload, context)


@when('The attribute value query with invalid value')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": "speed",
            "value": 123
        }
    }

    check_search("", payload, context)


@when('The attribute value query with valid value')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": "speed",
            "value": "bus"
        }
    }

    check_search("", payload, context)


@when('The attribute value query with invalid json object')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": "True"
    }

    check_search("", payload, context)


@when('An attribute value query is initiated')
def step_impl(context):
    context.type = 'attribute-value'

    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": "speed",
            "min": 30,
            "max": 50
        }
    }

    r = requests.post(url=VERMILLION_URL + SEARCH_ENDPOINT,
                      headers={'content-type': 'application/json'},
                      data=json.dumps(payload),
                      verify=False)

    context.response = r.json()['hits']


@when('An attribute value query with empty resource id array')
def step_impl(context):
    payload = {
        "id":
            [
                "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
                ""],
        "attribute": {
            "term": "speed",
            "min": 30,
            "max": 50
        }
    }

    check_search("", payload, context)


@when('An attribute value query with invalid resource id array')
def step_impl(context):
    payload = {
        "id":
            [
                "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
                " "],
        "attribute": {
            "term": "speed",
            "min": 30,
            "max": 50
        }
    }

    check_search("", payload, context)


@when('An attribute value query resource id array without token')
def step_impl(context):
    payload = {
        "id":
            [
                "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
                "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"],
        "attribute": {
            "term": "speed",
            "min": 30,
            "max": 50
        }
    }

    check_search("", payload, context)


@when('An attribute value query resource id is not a list of string')
def step_impl(context):
    payload = {
        "id":
            [1, 2, 3],
        "attribute": {
            "term": "speed",
            "min": 30,
            "max": 50
        }
    }

    check_search("", payload, context)
