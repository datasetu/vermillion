import requests
import urllib3
from behave import when
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from auth_vars import res,tokens,generate_random_chars
from utils import check_latest

VERMILLION_URL = 'https://localhost'
LATEST_ENDPOINT = '/latest'

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


@when('A latest API query is initiated')
def step_impl(context):
    context.type = 'latest-api'

    params = ({
        "id": res[2],
        "token": tokens["2_5_write"]
    }
    )

    check_latest(params,context)

@when('A latest API query is initiated for public resource id')
def step_impl(context):

    params = ({
        "id": res[0],

    }
    )

    check_latest(params,context)

@when('A latest API query with invalid resource id')
def step_impl(context):
    params = ({
        "id":
            generate_random_chars(),
        "token": tokens["master"]
    }
    )

    check_latest(params,context)


@when('A latest API query is with empty resource id')
def step_impl(context):
    params = ({
        "id": "",
        "token": tokens["master"]

    })

    check_latest(params,context)

@when('A latest API query is without resource id')
def step_impl(context):
    params = ({

        "token": tokens["master"]

    })

    check_latest(params,context)

@when('A latest API query is without token')
def step_impl(context):
    params = ({
        "id": res[1]

    })

    check_latest(params,context)

@when('A latest API query is with empty token')
def step_impl(context):
    params = {
        "id": res[0],
        "token": ""

    }

    check_latest(params,context)

@when('A latest API query is with invalid token')
def step_impl(context):
    params = ({
        "id": res[0],
        "token": generate_random_chars()

    })

    check_latest(params,context)
