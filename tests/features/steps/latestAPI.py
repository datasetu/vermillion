import requests
import urllib3
from behave import when
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from auth_vars import res, tokens
from utils import generate_random_chars, get_request

VERMILLION_URL = 'https://localhost'
LATEST_ENDPOINT = '/latest'
url = VERMILLION_URL + LATEST_ENDPOINT

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


@when('A latest API query is initiated')
def step_impl(context):
    params = ({
        "id": res[2],
        "token": tokens["2_5_write"]
    }
    )

    get_request(url, params, context)


@when('A latest API query is initiated for public resource id')
def step_impl(context):
    params = ({
        "id": res[0],

    }
    )

    get_request(url, params, context)


@when('A latest API query with invalid resource id')
def step_impl(context):
    params = ({
        "id":
            generate_random_chars(),
        "token": tokens["master"]
    }
    )

    get_request(url, params, context)


@when('A latest API query is with empty resource id')
def step_impl(context):
    params = ({
        "id": "",
        "token": tokens["master"]

    })

    get_request(url, params, context)


@when('A latest API query is without resource id')
def step_impl(context):
    params = ({

        "token": tokens["master"]

    })

    get_request(url, params, context)


@when('A latest API query is without token')
def step_impl(context):
    params = ({
        "id": res[1]

    })

    get_request(url, params, context)


@when('A latest API query is with empty token')
def step_impl(context):
    params = {
        "id": res[0],
        "token": ""

    }

    get_request(url, params, context)


@when('A latest API query is with invalid token')
def step_impl(context):
    params = ({
        "id": res[0],
        "token": generate_random_chars()

    })

    get_request(url, params, context)
