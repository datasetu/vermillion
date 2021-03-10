from behave import when
from auth_vars import urllib3, res, tokens, generate_random_chars, requests
from requests.packages.urllib3.exceptions import InsecureRequestWarning

VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'

PUBLISH_ENDPOINT = '/publish'

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


# XXX Secure-files tests need definition here
@when('The consumer publishes secure file with a valid token')
def step_impl(context):
    params = (
        ('id', res[6]),
        ('token', tokens["6_7_read"]),
    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }
    r = requests.post('https://localhost/publish', params=params, files=files, verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes secure file with a file and timeseries data')
def step_impl(context):
    params = (
        ('id', res[2]),
        ('token', tokens["master"]),
    )
    data = {"data": {"hello": "world"}}
    headers = {'Content-type': 'multipart/form-data'}
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    r = requests.post('https://localhost/publish', data=data, params=params, files=files, verify=False, headers=headers)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes secure file with an invalid token')
def step_impl(context):
    params = (
        ('id', res[2]),
        ('token', generate_random_chars()),
    )

    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }
    r = requests.post('https://localhost/publish', params=params, files=files, verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes secure file with an empty token')
def step_impl(context):
    params = (
        ('id', res[2]),
        ('token', ''),
    )

    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    r = requests.post('https://localhost/publish', params=params, files=files, verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes secure file with an invalid resource id')
def step_impl(context):
    params = (
        ('id', generate_random_chars() + ".public"),
        ('token', tokens["master"]),
    )

    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    r = requests.post('https://localhost/publish', params=params, files=files, verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes secure file with an empty resource id')
def step_impl(context):
    params = (
        ('id', ''),
        ('token', tokens["master"]),
    )

    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    r = requests.post('https://localhost/publish', params=params, files=files, verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer downloads file by passing a valid token')
def step_impl(context):
    params = (
        ('id', res[6]),
        ('token', tokens["6_7_read"]),
    )

    r = requests.get('https://localhost/download', params=params, verify=False)
    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer downloads file by passing a valid reroute link')
def step_impl(context):
    param = tokens["6_7_read"]

    r = requests.get('https://localhost/consumer/'+ param, verify=False)
    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer downloads file by using public resource id')
def step_impl(context):
    params = (
        ('id', res[0]),
        ('token', tokens["master"]),
    )

    r = requests.get('https://localhost/download', params=params, verify=False)
    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer downloads file by passing an invalid token')
def step_impl(context):
    params = (
        ('id', res[2]),
        ('token', generate_random_chars()),
    )

    r = requests.get('https://localhost/download', params=params, verify=False)
    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer downloads file by passing an empty token')
def step_impl(context):
    params = (
        ('id', res[2]),
        ('token', ''),
    )

    r = requests.get('https://localhost/download', params=params, verify=False)
    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer downloads file without passing token')
def step_impl(context):
    params = (
        ('id', res[2]),
    )

    r = requests.get('https://localhost/download', params=params, verify=False)
    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer downloads file by passing an invalid resource id')
def step_impl(context):
    params = (
        ('id', generate_random_chars()),
        ('token', tokens["master"]),

    )

    r = requests.get('https://localhost/download', params=params, verify=False)
    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer downloads file by passing an empty resource id')
def step_impl(context):
    params = (
        ('id', ''),
        ('token', tokens["master"]),
    )

    r = requests.get('https://localhost/download', params=params, verify=False)
    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer downloads file by passing only token')
def step_impl(context):
    params = (

        ('token', tokens["master"]),
    )

    r = requests.get('https://localhost/download', params=params, verify=False)
    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer downloads file by passing multiple resource ids and a token')
def step_impl(context):
    params = {
        "id": [
            res[6],
            res[7]
        ],
        "token": tokens["6_7_read"]

    }

    r = requests.get('https://localhost/download', params=params, verify=False)
    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)
