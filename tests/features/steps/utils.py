"""This error is for cases when the expected status code

does not match the status code from the API

"""
from auth_vars import requests, json

VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'
PUBLISH_ENDPOINT = '/publish'
LATEST_ENDPOINT = '/latest'

headers = {
    'Content-Type': 'application/json',
}


class UnexpectedStatusCodeError(Exception):

    def __init__(self, expected, actual, response_object):
        self.expected = expected
        self.actual = actual
        self.message = "Expecting {0} from API call got {1} with response {2}".format(expected, actual,
                                                                                      str(response_object))
        super().__init__(self.message)


"""This is for cases when the expected number of data points
 
 do not match the received datapoints from the API

"""


class ResponseCountMismatchError(Exception):

    def __init__(self, expected, actual):
        self.expected = expected
        self.actual = actual
        self.message = "Expecting {0} responses from API call got {1}".format(expected, actual)
        super().__init__(self.message)


"""This error is for all kinds of unexpected behaviour of vermillion
 
 The exact error should be described in the message
 
"""


class UnexpectedBehaviourError(Exception):

    def __init__(self, message):
        self.message = message
        super().__init__(self.message)


def check_publish(params, data, context):
    r = requests.post(VERMILLION_URL + PUBLISH_ENDPOINT, headers=headers, params=params, data=data, verify=False)
    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


def check_search(params, data, context):
    r = requests.post(VERMILLION_URL + SEARCH_ENDPOINT, headers=headers, params=params, data=json.dumps(data),
                      verify=False)

    context.response = r
    context.status_code = r.status_code
    # print(context.status_code, context.response)


def check_pub_file(params, files, context):
    r = requests.post(VERMILLION_URL + PUBLISH_ENDPOINT, params=params, files=files, verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


def check_download(params, context):
    r = requests.get('https://localhost/download', params=params, verify=False)
    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


def check_openfiles_pub(data, files, context):
    r = requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                      data=data,
                      files=files,
                      verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


def check_latest(params, context):
    r = requests.get(url=VERMILLION_URL + LATEST_ENDPOINT,
                     headers={'content-type': 'application/json'},
                     params=params,
                     verify=False)

    context.response = r.json()
    context.status_code = r.status_code
    print(context.status_code, context.response)
