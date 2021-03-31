import json
import string
import random

import requests

VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'
PUBLISH_ENDPOINT = '/publish'
LATEST_ENDPOINT = '/latest'

headers = {
    'Content-Type': 'application/json',
}

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
    print(context.status_code, context.response)


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


def generate_random_chars(n=32, letters=True, digits=True, special_chars=True):
    generate = ''
    if letters:
        generate += string.ascii_letters
    if digits:
        generate += string.digits
    if special_chars:
        generate += string.punctuation

    return ''.join([random.choice(generate) for _ in range(n)])
