from behave import when
from auth_vars import urllib3, generate_random_chars, requests, json
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from utils import check_search

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


@when('Timeseries query body is empty')
def step_impl(context):
    payload = {}

    check_search("", payload, context)


@when('Timeseries query body is invalid')
def step_impl(context):
    payload = generate_random_chars()

    check_search("", payload, context)


@when('Timeseries query time has invalid json object')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public",
        "time": "True"
    }

    check_search("", payload, context)


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

    check_search("", payload, context)


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

    check_search("", payload, context)


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

    check_search("", payload, context)


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

    check_search("", payload, context)


@when('Timeseries query start and end date is not present')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public",
        "time": {

        }
    }

    check_search("", payload, context)


@when('Timeseries query has only resource id')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public"
    }

    check_search("", payload, context)


@when('Timeseries query resource id is empty')
def step_impl(context):
    payload = {"id": "", "time": {"start": "2020-03-01", "end": "2020-03-27"}}

    check_search("", payload, context)


@when('Timeseries query resource id is invalid')
def step_impl(context):
    payload = {
        "id": generate_random_chars() + ".public",
        "time": {
            "start": "2020-03-01",
            "end": "2020-03-27"
        }
    }

    check_search("", payload, context)


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

    check_search("", payload, context)


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

    check_search("", payload, context)
