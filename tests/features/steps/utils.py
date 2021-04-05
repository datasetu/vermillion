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


def post_request(url, params, data, context):
    r = requests.post(url, headers=headers, params=params, data=data, verify=False)
    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


def get_request(url, params, context):
    r = requests.get(url, headers=headers, params=params, verify=False)
    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


def post_request_publish_secure(params, context):
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }
    r = requests.post(VERMILLION_URL + PUBLISH_ENDPOINT, params=params, files=files, verify=False)

    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)


def post_request_publish_public(data, files, context):
    r = requests.post(url=VERMILLION_URL + PUBLISH_ENDPOINT,
                      data=data,
                      files=files,
                      verify=False)

    context.response = r
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
