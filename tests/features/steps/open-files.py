import os
import glob

import requests
import urllib3
from behave import when

from requests.packages.urllib3.exceptions import InsecureRequestWarning

from auth_vars import res, tokens, id_prefix
from utils import check_openfiles_pub, generate_random_chars

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

@when('The consumer publishes with a valid token')
def step_impl(context):
    data = (

        ("id", res[0]),
        ('token', tokens["master"]),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    check_openfiles_pub(data, files, context)


@when('The consumer publishes without resource id')
def step_impl(context):
    data = (

        ('token', tokens["master"]),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    check_openfiles_pub(data, files, context)


@when('The consumer publishes without token')
def step_impl(context):
    data = (

        ("id", res[0]),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    check_openfiles_pub(data, files, context)


@when('The consumer publishes with invalid resource id')
def step_impl(context):
    data = (

        ("id", id_prefix + generate_random_chars() + ".public"),
        ('token', tokens["master"]),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    check_openfiles_pub(data, files, context)


@when('The consumer publishes with empty resource id')
def step_impl(context):
    data = (

        ("id", ""),
        ('token', tokens["master"]),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    check_openfiles_pub(data, files, context)


@when('The consumer publishes with invalid token')
def step_impl(context):
    data = (

        ("id", res[0]),
        ('token', generate_random_chars()),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    check_openfiles_pub(data, files, context)


@when('The consumer publishes with empty token')
def step_impl(context):
    data = (

        ("id", res[0]),
        ('token', ''),

    )
    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    check_openfiles_pub(data, files, context)


@when('The consumer publishes by removing file form parameter')
def step_impl(context):
    data = (

        ("id", res[0]),
        ('token', tokens["master"]),
    )

    files = {
        # 'file': ('sample.txt', open('sample.txt', 'rb')),

        'metadata': ('meta.json', open('meta.json', 'rb')),
    }

    check_openfiles_pub(data, files, context)


@when('The consumer publishes with invalid json meta file')
def step_impl(context):
    data = (

        ("id", res[0]),
        ('token', tokens["master"]),
    )

    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),

        'metadata': ('invalidmeta.json', open('invalidmeta.json', 'rb')),
    }

    check_openfiles_pub(data, files, context)


@when('The consumer publishes by removing metadata form parameter')
def step_impl(context):
    data = (

        ("id", res[1]),
        ('token', tokens["master"]),
    )

    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        # 'metadata': ('meta.json', open('meta.json', 'rb')),

    }

    check_openfiles_pub(data, files, context)


@when('The consumer publishes by using extraneous form parameter')
def step_impl(context):
    data = (

        ("id", res[0]),
        ('token', tokens["master"]),

    )
    files = {
        'abc': ('samplecsv.csv', open('samplecsv.csv', 'rb')),
        'efg': ('samplepdf.pdf', open('samplepdf.pdf', 'rb')),
    }
    # This part of code removes the files present in the file-uploads folder that existed previously
    fil = glob.glob('../api-server/file-uploads/*')
    for f in fil:
        os.remove(f)

    check_openfiles_pub(data, files, context)


@when('The consumer publishes with empty form parameter')
def step_impl(context):
    data = (
        ("id", res[0]
         ),
        ('token', tokens["master"]),

    )
    files = {
        # 'file': ('sample.txt', open('sample.txt', 'rb')),
        # 'metadata': ('meta.json', open('meta.json', 'rb')),

    }

    check_openfiles_pub(data, files, context)


@when('The consumer publishes with more than 2 form parameters')
def step_impl(context):
    data = (
        ("id", res[0]),
        ('token', tokens["master"]),
    )

    files = {
        'file': ('sample.txt', open('sample.txt', 'rb')),
        'metadata': ('meta.json', open('meta.json', 'rb')),
        'fille': ('samplecsv.csv', open('samplecsv.csv', 'rb')),
        'fie': ('samplepdf.pdf', open('samplepdf.pdf', 'rb'))

    }

    check_openfiles_pub(data, files, context)


@when('The consumer downloads the file')
def step_impl(context):
    urd = 'https://localhost/provider/public/'
    r = requests.get(url=urd + res[0], verify=False)
    open('test-resource.public', 'w').write('This is the downloaded file')
    context.response = r
    context.status_code = r.status_code
    print(context.status_code, context.response)

