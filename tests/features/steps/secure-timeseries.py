import time
from behave import when
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from auth_vars import urllib3,requests,generate_random_chars,res,tokens,json

VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'
PUBLISH_ENDPOINT = '/publish'

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


# XXX Secure-timeseries tests need definition here

@when('The consumer publishes data with a valid token')
def step_impl(context):
    headers = {
        'Content-Type': 'application/json',
    }
    params = (
        ('id', res[3]),
        ('token', tokens["master"]),
    )

    data = '{"data": {"hello": "world"}}'

    r = requests.post(VERMILLION_URL + PUBLISH_ENDPOINT, headers=headers, params=params, data=data, verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes data with an invalid token')
def step_impl(context):
    headers = {
        'Content-Type': 'application/json',
    }
    params = (
        ('id', res[3]),
        ('token', generate_random_chars()),
    )

    data = '{"data": {"hello": "world"}}'

    r = requests.post(VERMILLION_URL + PUBLISH_ENDPOINT, headers=headers, params=params, data=data, verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes data with an empty token')
def step_impl(context):
    headers = {
        'Content-Type': 'application/json',
    }

    params = (
        ('id', res[3]),
        ('token', ''),
    )

    data = '{"data": {"hello": "world"}}'

    r = requests.post(VERMILLION_URL + PUBLISH_ENDPOINT, headers=headers, params=params, data=data, verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes data without a body')
def step_impl(context):
    headers = {
        'Content-Type': 'application/json',
    }

    params = (
        ('id', res[3]),
        ('token', tokens["master"]),
    )

    # data = '{"data": {"hello": "world"}}'

    r = requests.post(VERMILLION_URL + PUBLISH_ENDPOINT, headers=headers, params=params, verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes data when body is null')
def step_impl(context):
    headers = {
        'Content-Type': 'application/json',
    }

    params = (
        ('id', res[3]),
        ('token', tokens["master"]),
    )

    data = ''

    r = requests.post(VERMILLION_URL + PUBLISH_ENDPOINT, headers=headers, params=params, data=data, verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes data with an invalid resource id')
def step_impl(context):
    headers = {
        'Content-Type': 'application/json',
    }

    params = (
        ('id', generate_random_chars()),
        ('token', tokens["master"]),
    )

    data = '{"data": {"hello": "world"}}'

    r = requests.post(VERMILLION_URL + PUBLISH_ENDPOINT, headers=headers, params=params, data=data, verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes data with an empty resource id')
def step_impl(context):
    headers = {
        'Content-Type': 'application/json',
    }

    params = (
        ('id', ''),
        ('token', tokens["master"]),
    )

    data = '{"data": {"hello": "world"}}'

    r = requests.post(VERMILLION_URL + PUBLISH_ENDPOINT, headers=headers, params=params, data=data, verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer requests for a standalone authorised ID')
def step_impl(context):
    context.type = 'authorised_id'
    headers = {
        'Content-Type': 'application/json',
    }
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
    time.sleep(1)
    r = requests.post(VERMILLION_URL + SEARCH_ENDPOINT, headers=headers, params=params, data=json.dumps(data),
                      verify=False)
    print(r.text)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer requests for an unauthorised ID')
def step_impl(context):
    headers = {
        'Content-Type': 'application/json',
    }
    params = (
        ('token', tokens["master"]),

    )
    data = {
        "id":
            generate_random_chars(),

        "time": {
            "start": "2021-01-01",
            "end": "2021-11-01"
        }
    }

    r = requests.post(VERMILLION_URL + SEARCH_ENDPOINT, headers=headers, params=params, data=json.dumps(data),
                      verify=False)
    print(r.text)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer requests for multiple authorised IDs')
def step_impl(context):
    headers = {
        'Content-Type': 'application/json',
    }

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

    r = requests.post(VERMILLION_URL + SEARCH_ENDPOINT, headers=headers, params=params, data=json.dumps(data),
                      verify=False)
    print(r.text)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer requests for multiple unauthorised IDs')
def step_impl(context):
    headers = {
        'Content-Type': 'application/json',
    }

    params = (
        ('token', tokens["master"]),

    )
    data = {
        "id": [
            generate_random_chars(),
            generate_random_chars()
        ],
        "time": {
            "start": "2021-01-01",
            "end": "2021-11-01"
        }
    }

    r = requests.post(VERMILLION_URL + SEARCH_ENDPOINT, headers=headers, params=params, data=json.dumps(data),
                      verify=False)
    print(r.text)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer requests for unauthorised IDs among authorised IDs')
def step_impl(context):
    headers = {
        'Content-Type': 'application/json',
    }

    params = (
        ('token', tokens["master"]),

    )
    data = {
        "id": [
            res[3],
            generate_random_chars()
        ],
        "time": {
            "start": "2021-01-01",
            "end": "2021-11-01"
        }
    }

    r = requests.post(VERMILLION_URL + SEARCH_ENDPOINT, headers=headers, params=params, data=json.dumps(data),
                      verify=False)
    print(r.text)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)
