from behave import then
import os
import requests
import urllib3
from auth_vars import tokens, res

from requests.packages.urllib3.exceptions import InsecureRequestWarning
from error_definitions import ResponseCountMismatchError, UnexpectedStatusCodeError, UnexpectedBehaviourError

VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


@then('All matching records are returned')
def step_impl(context):
    if context.type == 'timeseries':
        if len(context.response) != 2000:
            raise ResponseCountMismatchError(2000, len(context.response))

    if context.type == 'geospatial':
        if len(context.response) != 5705:
            raise ResponseCountMismatchError(5705, len(context.response))

    if context.type == 'attribute-value':
        if len(context.response) != 634:
            raise ResponseCountMismatchError(634, len(context.response))

    if context.type == 'complex':
        if len(context.response) != 305:
            raise ResponseCountMismatchError(305, len(context.response))

    if context.type == 'latest_search':
        if len(context.response) != 1:
            raise ResponseCountMismatchError(1, len(context.response))

    if context.type == 'latest_public':
        if len(context.response.json()['hits']) != 1:
            raise ResponseCountMismatchError(1, len(context.response.json()['hits']))

    if context.type == 'scroll-search':
        if len(context.response) != 500:
            raise ResponseCountMismatchError(500, len(context.response))


@then('The response status should be {expected_code}')
def step_impl(context, expected_code):
    if context.status_code != int(expected_code):
        raise UnexpectedStatusCodeError(int(expected_code), context.status_code, context.response)


# @then('The file permission is reset')
# def step_impl(context):
#     os.chmod("../setup/provider", 0o755)


@then('The expected file is returned')
def step_impl(context):
    l = "This is the downloaded file"
    if not os.path.exists('test-resource.public'):
        f = open('test-resource.public', 'rb')
        with f as read_obj:
            for line in read_obj:
                if l not in line:
                    raise UnexpectedBehaviourError('Files havent been downloaded')


@then('The uploaded files are deleted')
def step_impl(context):

    DIR = '../api-server/file-uploads'
    number_of_files = len([
        name for name in os.listdir(DIR)
        if os.path.isfile(os.path.join(DIR, name))
    ])

    print(number_of_files)
    if number_of_files > 1:
        raise UnexpectedBehaviourError('Files have not been deleted')


@then('The file gets uploaded in the provider public directory')
def step_impl(context):
    DIR = '../setup/provider/public/' + res[0] + '/'
    number_of_files = len([
        name for name in os.listdir(DIR)
        if os.path.isfile(os.path.join(DIR, name))
    ])
    print(number_of_files)
    if number_of_files != 1:
        raise UnexpectedBehaviourError('Files have not been created')


@then('The file gets uploaded in the provider secure directory')
def step_impl(context):
    DIR = '../setup/provider/secure/' + res[8] + '/'
    number_of_files = len([
        name for name in os.listdir(DIR)
        if os.path.isfile(os.path.join(DIR, name))
    ])
    print(number_of_files)
    if number_of_files != 1:
        raise UnexpectedBehaviourError('Files have not been created')


@then('The file gets uploaded in the consumer directory')
def step_impl(context):
    DIR = '../api-server/webroot/consumer/' + tokens[
        "8_10_rw"] + '/rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/'
    if not os.path.exists(DIR):
        raise UnexpectedBehaviourError('Files have not been created')


@then('The response should contain the secure timeseries data')
def step_impl(context):
    if context.type == 'authorised_id':
        dat = {"hello": "world"}

        re = context.response.json()
        print(re)
        print(len(re['hits']))
        if len(re['hits']) == 1:
            for value in re['hits']:
                print(value)
                if 'data' in value and value['data'] != dat:
                    # if dat != re['hits'][0]['data']:
                    raise UnexpectedBehaviourError('Secure Timeseries data not found in response')
        else:
            raise UnexpectedBehaviourError('Secure Timeseries data not found in response')

    if context.type == 'authorised_id_multiple':
        dat = {"hello": "india"}
        dat1 = {"hello": "world"}
        re = context.response.json()
        print(re)
        print(len(re['hits']))
        if len(re['hits']) == 2:
            for value in re['hits']:
                print(value)
                if 'data' in value and not (value['data'] == dat or value['data'] == dat1):
                    raise UnexpectedBehaviourError('Secure Timeseries data not found in response')

        else:
            raise UnexpectedBehaviourError('Secure Timeseries data not found in response')


@then('The response should contain the scroll id')
def step_impl(context):
    if context.type == 'geospatial-scroll':
        re = context.response.json()
        print(re['scroll_id'])
        for value in re['hits']:
            print(value)
            if 'scroll_id' not in re:
                raise UnexpectedBehaviourError('Scroll id not found in response')


@then('The response should contain an auth token')
def step_impl(context):
    if not context.response['token']:
        raise UnexpectedBehaviourError('Auth token not found in response')


@then('Introspect should succeed')
def step_impl(context):
    context.type = 'token_introspect'
    expected_response = {
        "consumer":
            "consumer@iisc.ac.in",
        "request": [{
            "id":
                "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource-1",
            "apis": ["/*"],
            "body": None,
            "scopes": ["read"],
            "methods": ["*"],
            "environments": ["*"]
        }, {
            "id":
                "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource-2",
            "apis": ["/*"],
            "body": None,
            "scopes": ["read"],
            "methods": ["*"],
            "environments": ["*"]
        }, {
            "id":
                "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource-3",
            "apis": ["/*"],
            "body": None,
            "scopes": ["read"],
            "methods": ["*"],
            "environments": ["*"]
        }, {
            "id":
                "rbccps.org/e096b3abef24b99383d9bd28e9b8c89cfd50be0b/example.com/test-category/test-resource.public",
            "apis": ["/*"],
            "body": None,
            "scopes": ["write"],
            "methods": ["*"],
            "environments": ["*"]
        }],
        "consumer-certificate-class": 2
    }

    context.response.pop('expiry', None)

    if ordered(expected_response) != ordered(context.response):
        print(context.response)
        raise UnexpectedBehaviourError('Introspect Response is not as expected')


def ordered(obj):
    if isinstance(obj, dict):
        return sorted((k, ordered(v)) for k, v in obj.items())
    if isinstance(obj, list):
        return sorted(ordered(x) for x in obj)
    else:
        return obj
