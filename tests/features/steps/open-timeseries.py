from behave import when
from auth_vars import urllib3, generate_random_chars,requests, json
from requests.packages.urllib3.exceptions import InsecureRequestWarning

VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


@when('Timeseries query body is empty')
def step_impl(context):

    payload = {}

    r = requests.post(url=VERMILLION_URL + SEARCH_ENDPOINT,
                      headers={'content-type': 'application/json'},
                      data=json.dumps(payload),
                      verify=False)

    context.response = r.json()
    context.status_code = r.status_code


@when('Timeseries query body is invalid')
def step_impl(context):

    payload = generate_random_chars()

    r = requests.post(url=VERMILLION_URL + SEARCH_ENDPOINT,
                      headers={'content-type': 'application/json'},
                      data=json.dumps(payload),
                      verify=False)

    context.response = r.json()
    context.status_code = r.status_code


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

    r = requests.post(url=VERMILLION_URL + SEARCH_ENDPOINT,
                      headers={'content-type': 'application/json'},
                      data=json.dumps(payload),
                      verify=False)

    context.response = r.json()
    context.status_code = r.status_code


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

    r = requests.post(url=VERMILLION_URL + SEARCH_ENDPOINT,
                      headers={'content-type': 'application/json'},
                      data=json.dumps(payload),
                      verify=False)

    context.response = r.json()
    context.status_code = r.status_code


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

    r = requests.post(url=VERMILLION_URL + SEARCH_ENDPOINT,
                      headers={'content-type': 'application/json'},
                      data=json.dumps(payload),
                      verify=False)

    context.response = r.json()
    context.status_code = r.status_code


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

    r = requests.post(url=VERMILLION_URL + SEARCH_ENDPOINT,
                      headers={'content-type': 'application/json'},
                      data=json.dumps(payload),
                      verify=False)

    context.response = r.json()
    context.status_code = r.status_code

@when('Timeseries query start and end date is not present')
def step_impl(context):

    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public",
        "time": {

        }
    }

    r = requests.post(url=VERMILLION_URL + SEARCH_ENDPOINT,
                      headers={'content-type': 'application/json'},
                      data=json.dumps(payload),
                      verify=False)

    context.response = r.json()
    context.status_code = r.status_code

@when('Timeseries query has only resource id')
def step_impl(context):

    payload = {
        "id":
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public"
    }

    r = requests.post(url=VERMILLION_URL + SEARCH_ENDPOINT,
                      headers={'content-type': 'application/json'},
                      data=json.dumps(payload),
                      verify=False)

    context.response = r.json()
    context.status_code = r.status_code


@when('Timeseries query resource id is empty')
def step_impl(context):

    payload = {"id": "", "time": {"start": "2020-03-01", "end": "2020-03-27"}}

    r = requests.post(url=VERMILLION_URL + SEARCH_ENDPOINT,
                      headers={'content-type': 'application/json'},
                      data=json.dumps(payload),
                      verify=False)

    context.response = r.json()
    context.status_code = r.status_code


@when('Timeseries query resource id is invalid')
def step_impl(context):

    payload = {
        "id": generate_random_chars()+".public",
        "time": {
            "start": "2020-03-01",
            "end": "2020-03-27"
        }
    }

    r = requests.post(url=VERMILLION_URL + SEARCH_ENDPOINT,
                      headers={'content-type': 'application/json'},
                      data=json.dumps(payload),
                      verify=False)

    context.response = r.json()
    context.status_code = r.status_code

@when('Timeseries query start and end date is not string')
def step_impl(context):


    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public",
        "time": {
            "start": 202003-0,
            "end": 202003-27
        }
    }

    r = requests.post(url=VERMILLION_URL + SEARCH_ENDPOINT,
                      headers={'content-type': 'application/json'},
                      data=json.dumps(payload),
                      verify=False)

    context.response = r.json()
    context.status_code = r.status_code

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

    r = requests.post(url=VERMILLION_URL + SEARCH_ENDPOINT,
                      headers={'content-type': 'application/json'},
                      data=json.dumps(payload),
                      verify=False)

    context.response = r.json()

