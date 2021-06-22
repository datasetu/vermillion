import os
from os import path
import shutil
import requests
import urllib3
from behave import when


from requests.packages.urllib3.exceptions import InsecureRequestWarning

from auth_vars import res, tokens
from utils import generate_random_chars, post_files, get_request

VERMILLION_URL = 'https://localhost'
PUBLISH_ENDPOINT = '/publish'
url = VERMILLION_URL + PUBLISH_ENDPOINT
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


@when('The consumer publishes with a valid token')
def step_impl(context):
    params = (

        ("id", res[0]),
        ('token', tokens["master"]),

    )

    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }
    directory = "public"
    parent = "../setup/provider/"
    path_dir = os.path.join(parent, directory)
    if path.exists(path_dir):
        shutil.rmtree(path_dir)
    post_files(params, files, context)


@when('The consumer publishes without resource id')
def step_impl(context):
    params = (

        ('token', tokens["master"]),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    post_files(params, files, context)


@when('The consumer publishes without token')
def step_impl(context):
    params = (

        ("id", res[0]),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    post_files(params, files, context)


@when('The consumer publishes with invalid resource id')
def step_impl(context):
    params = (

        ("id", generate_random_chars() + ".public"),
        ('token', tokens["master"]),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    post_files(params, files, context)


@when('The consumer publishes with empty resource id')
def step_impl(context):
    params = (

        ("id", ""),
        ('token', tokens["master"]),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    post_files(params, files, context)


@when('The consumer publishes with invalid token')
def step_impl(context):
    params = (

        ("id", res[0]),
        ('token', generate_random_chars()),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    post_files(params, files, context)


@when('The consumer publishes with empty token')
def step_impl(context):
    params = (

        ("id", res[0]),
        ('token', ''),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    post_files(params, files, context)


@when('The consumer publishes by removing file form parameter')
def step_impl(context):
    params = (

        ("id", res[0]),
        ('token', tokens["master"]),
    )

    files = {
        # 'file': ('sample.txt', open('sample.txt', 'rb')),

        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    post_files(params, files, context)


@when('The consumer publishes with invalid json meta file')
def step_impl(context):
    params = (

        ("id", res[0]),
        ('token', tokens["master"]),
    )
    f = open("invalidmeta.json", "w")
    f.write("{ hi, ")
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),

        'metadata': ('invalidmeta.json', open('invalidmeta.json', 'rb')),
    }

    post_files(params, files, context)


@when('The consumer publishes by removing metadata form parameter')
def step_impl(context):
    params = (

        ("id", res[0]),
        ('token', tokens["master"]),
    )

    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        # 'metadata': ('meta.json', open('meta.json', 'rb')),

    }

    post_files(params, files, context)


@when('The consumer publishes by using extraneous form parameter')
def step_impl(context):
    params = (

        ("id", res[0]),
        ('token', tokens["master"]),

    )
    f = open("samplecsv.csv", "w")
    f = open("samplepdf.pdf", "w")

    files = {
        'abc': ('samplecsv.csv', open('samplecsv.csv', 'rb')),
        'efg': ('samplepdf.pdf', open('samplepdf.pdf', 'rb')),
    }

    post_files(params, files, context)


@when('The consumer publishes with empty form parameter')
def step_impl(context):
    params = (
        ("id", res[0]
         ),
        ('token', tokens["master"]),

    )
    files = {
        # 'file': ('sample.txt', open('sample.txt', 'rb')),
        # 'metadata': ('meta.json', open('meta.json', 'rb')),

    }

    post_files(params, files, context)


@when('The consumer publishes with more than 2 form parameters')
def step_impl(context):
    params = (
        ("id", res[0]),
        ('token', tokens["master"]),
    )

    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
        'fille': ('samplecsv.csv', open('samplecsv.csv', 'rb')),
        'fie': ('samplepdf.pdf', open('samplepdf.pdf', 'rb'))

    }

    post_files(params, files, context)


@when('The consumer downloads the file')
def step_impl(context):
    url = 'https://localhost/provider/public/' + res[0]
    get_request(url, None, context)
    open('test-resource.public', 'w').write('This is the downloaded file')


@when('The consumer publishes with a valid and invalid form parameter')
def step_impl(context):
    params = (
        ("id", res[0]
         ),
        ('token', tokens["master"]),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'fil': ('samplecsv.csv', open('samplecsv.csv', 'rb')),

    }

    post_files(params, files, context)


@when('The consumer publishes with more than 2 form parameters-1')
def step_impl(context):
    params = (
        ("id", res[0]),
        ('token', tokens["master"]),
    )

    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
        'fille': ('samplecsv.csv', open('samplecsv.csv', 'rb')),
        'fie': ('samplepdf.pdf', open('samplepdf.pdf', 'rb'))

    }

    post_files(params, files, context)
