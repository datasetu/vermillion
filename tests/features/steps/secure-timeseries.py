import time
from behave import when
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from auth_vars import urllib3, requests, generate_random_chars, res, tokens, json

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
        ('token', tokens["master"])
    )

    data = '{"data": {"hello": "world"}}'

    r = requests.post(VERMILLION_URL + PUBLISH_ENDPOINT, headers=headers, params=params, data=data, verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes data without data field in body')
def step_impl(context):
    headers = {
        'Content-Type': 'application/json',
    }
    params = (
        ('id', res[4]),
        ('token', tokens["master"]),
    )

    r = requests.post(VERMILLION_URL + PUBLISH_ENDPOINT, headers=headers, params=params, verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes data with invalid json data')
def step_impl(context):
    headers = {
        'Content-Type': 'application/json',
    }
    params = (
        ('id', res[0]),
        ('token', tokens["master"]),
    )

    data = "True"

    r = requests.post(VERMILLION_URL + PUBLISH_ENDPOINT, headers=headers, params=params, data=data, verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes data with invalid body fields')
def step_impl(context):
    headers = {
        'Content-Type': 'application/json',
    }
    params = (
        ('id', res[0]),
        ('token', tokens["master"]),
        ('item', "value"),
    )

    data = '{"data": {"hello": "world"}}'

    xyz= 'testing invalid'

    r = requests.post(VERMILLION_URL + PUBLISH_ENDPOINT, headers=headers, params=params, data=xyz, verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes data with an invalid token')
def step_impl(context):
    headers = {
        'Content-Type': 'application/json',
    }
    params = (
        ('id', res[0]),
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
        ('id', res[0]),
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
        ('id', res[0]),
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
        ('id', res[0]),
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
        ('id', generate_random_chars() + ".public"),
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

    r = requests.post(VERMILLION_URL + SEARCH_ENDPOINT, headers=headers, params=params, data=json.dumps(data),
                      verify=False)
    print(r.text)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer requests for a standalone authorised ID with invalid token')
def step_impl(context):
    headers = {
        'Content-Type': 'application/json',
    }
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
    r = requests.post(VERMILLION_URL + SEARCH_ENDPOINT, headers=headers, params=params, data=json.dumps(data),
                      verify=False)
    print(r.text)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer requests for a standalone authorised ID without token')
def step_impl(context):
    headers = {
        'Content-Type': 'application/json',
    }

    data = {
        "id":
            res[3],

        "time": {
            "start": "2021-01-01",
            "end": "2021-11-01"
        }
    }
    time.sleep(1)
    r = requests.post(VERMILLION_URL + SEARCH_ENDPOINT, headers=headers, data=json.dumps(data),
                      verify=False)
    print(r.text)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)
