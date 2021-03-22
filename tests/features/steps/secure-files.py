import requests
import urllib3

from behave import when
# from auth_vars import res, tokens
from down_vars import res, tokens
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from utils import check_pub_file, check_download, generate_random_chars

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


# XXX Secure-files tests need definition here
@when('The consumer publishes secure file with a valid token')
def step_impl(context):
    params = (
        ('id', res[0]),
        ('token', tokens["down"]),
    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }
    check_pub_file(params, files, context)


@when('The consumer publishes with a valid token(3)')
def step_impl(context):
    params = (

        ("id", res[1]),
        ('token', tokens["down"]),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    check_pub_file(params, files, context)


@when('The consumer publishes with a valid token(4)')
def step_impl(context):
    params = (

        ("id", res[2]),
        ('token', tokens["down"]),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    check_pub_file(params, files, context)


@when('The consumer downloads file by passing a valid reroute link')
def step_impl(context):
    param = tokens["down"]

    r = requests.get('https://localhost/consumer/' + param + '/', verify=False)
    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer downloads file by passing a valid reroute link(2)')
def step_impl(context):
    params = (
        ('token', tokens["down"]),

    )

    r = requests.get('https://localhost/download', params=params, verify=False)
    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer publishes secure file with a file and timeseries data')
def step_impl(context):
    params = (
        ('id', res[1]),
        ('token', tokens["down"]),
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
        ('id', res[1]),
        ('token', generate_random_chars()),
    )

    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }
    check_pub_file(params, files, context)


@when('The consumer publishes secure file with an empty token')
def step_impl(context):
    params = (
        ('id', res[1]),
        ('token', ''),
    )

    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    check_pub_file(params, files, context)


@when('The consumer publishes secure file with an invalid resource id')
def step_impl(context):
    params = (
        ('id', generate_random_chars() + ".public"),
        ('token', tokens["down"]),
    )

    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    check_pub_file(params, files, context)


@when('The consumer publishes secure file with an empty resource id')
def step_impl(context):
    params = (
        ('id', ''),
        ('token', tokens["down"]),
    )

    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    check_pub_file(params, files, context)


@when('The consumer downloads file by passing a valid token')
def step_impl(context):
    params = (
        ('id',
         res[0]
         ),
        ('token', tokens["down"]),
    )
    r = requests.get('https://localhost/download', params=params, verify=False)
    open('test-download', 'wb').write(r.content)
    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('The consumer downloads file by passing an invalid token')
def step_impl(context):
    params = (
        ('id', res[2]),
        ('token', generate_random_chars()),
    )

    check_download(params, context)


@when('The consumer downloads file by passing an empty token')
def step_impl(context):
    params = (
        ('id', res[2]),
        ('token', ''),
    )

    check_download(params, context)


@when('The consumer downloads file without passing token')
def step_impl(context):
    params = (
        ('id', res[2]),
    )

    check_download(params, context)


@when('The consumer downloads file by passing an invalid resource id')
def step_impl(context):
    params = (
        ('id', generate_random_chars()),
        ('token', tokens["down"]),

    )

    check_download(params, context)


@when('The consumer downloads file by passing an empty resource id')
def step_impl(context):
    params = (
        ('id', ''),
        ('token', tokens["down"]),
    )

    check_download(params, context)


@when('The consumer downloads file by passing multiple resource ids and a token')
def step_impl(context):
    r = requests.get('https://localhost/download?token=' + tokens["down"] + '&id=' + res[1] + ',' + res[2],
                     verify=False)
    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)
