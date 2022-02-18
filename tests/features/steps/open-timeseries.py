import requests
import json
import urllib3
from behave import when
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from utils import generate_random_chars, post_request

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'
url = VERMILLION_URL+SEARCH_ENDPOINT


@when('Timeseries query body is empty')
def step_impl(context):
    payload = {}

    post_request(url, "", json.dumps(payload), context)


@when('Timeseries query body is invalid')
def step_impl(context):
    payload = generate_random_chars()

    post_request(url, "", json.dumps(payload), context)


@when('Timeseries query time has invalid json object')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public",
        "time": "True"
    }

    post_request(url, "", json.dumps(payload), context)


@when('Timeseries query start date is invalid')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public",
        "time": {
            "start": generate_random_chars(),
            "end": "2020-03-27"
        }
    }

    post_request(url, "", json.dumps(payload), context)


@when('Timeseries query end date is invalid')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public",
        "time": {
            "start": generate_random_chars(),
            "end": generate_random_chars()
        }
    }

    post_request(url, "", json.dumps(payload), context)


@when('Timeseries query date is empty')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public",
        "time": {
            "start": "",
            "end": ""
        }
    }

    post_request(url, "", json.dumps(payload), context)


@when('Timeseries query date is not present')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public",
        "time": {
            "start": "0000-00-00",
            "end": "0000-00-00"
        }
    }

    post_request(url, "", json.dumps(payload), context)


@when('Timeseries query start and end date is not present')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public",
        "time": {

        }
    }

    post_request(url, "", json.dumps(payload), context)


@when('Timeseries query has only resource id')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public"
    }

    post_request(url, "", json.dumps(payload), context)


@when('Timeseries query resource id is empty')
def step_impl(context):
    payload = {"id": "", "time": {"start": "2020-03-01", "end": "2020-03-27"}}

    post_request(url, "", json.dumps(payload), context)


@when('Timeseries query resource id is invalid')
def step_impl(context):
    payload = {
        "id": generate_random_chars() + ".public",
        "time": {
            "start": "2020-03-01",
            "end": "2020-03-27"
        }
    }

    post_request(url, "", json.dumps(payload), context)


@when('Timeseries query start and end date is not string')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public",
        "time": {
            "start": 202003 - 0,
            "end": 202003 - 27
        }
    }

    post_request(url, "", json.dumps(payload), context)

@when('Timeseries query start and end date is an integer')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public",
        "time": {
            "start": 20200320,
            "end": 20200327
        }
    }

    post_request(url, "", json.dumps(payload), context)

@when('Timeseries query start date greater than end date')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public",
        "time": {
            "start": "2020-09-01",
            "end": "2020-03-27"
        }
    }

    post_request(url, "", json.dumps(payload), context)

@when('Timeseries query year greater than 9999')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public",
        "time": {
            "start": "20201-09-01",
            "end": "2020-03-27"
        }
    }

    post_request(url, "", json.dumps(payload), context)

@when('Timeseries query month greater than 12')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public",
        "time": {
            "start": "2020-80-01",
            "end": "2020-03-27"
        }
    }

    post_request(url, "", json.dumps(payload), context)

@when('Timeseries query day greater than 31')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public",
        "time": {
            "start": "2020-09-100",
            "end": "2020-03-27"
        }
    }

    post_request(url, "", json.dumps(payload), context)


@when('Timeseries query date in invalid format')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public",
        "time": {
            "start": "202-039-100",
            "end": "2020-03-27"
        }
    }

    post_request(url, "", json.dumps(payload), context)

@when('A timeseries query is initiated')
def step_impl(context):
    context.type = 'timeseries'

    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public",
        "time": {
            "start": "2020-03-01",
            "end": "2020-03-27"
        }
    }

    post_request(url, "", json.dumps(payload), context)
