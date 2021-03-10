from auth_vars import urllib3, generate_random_chars, requests, json, tokens, res
from behave import when
from requests.packages.urllib3.exceptions import InsecureRequestWarning

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

    r = requests.get(url=VERMILLION_URL + LATEST_ENDPOINT,
                     headers={'content-type': 'application/json'},
                     params=params,
                     verify=False)

    context.response = r.json()
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('A latest API query with invalid resource id')
def step_impl(context):
    params = ({
        "id":
            generate_random_chars(),
        "token": tokens["master"]
    }
    )

    r = requests.get(url=VERMILLION_URL + LATEST_ENDPOINT,
                     headers={'content-type': 'application/json'},
                     params=params,
                     verify=False)

    context.response = r.json()
    context.status_code = r.status_code
    print(context.status_code, context.response)


@when('A latest API query is with empty resource id')
def step_impl(context):
    params = ({
        "id": "",
        "token": tokens["master"]

    })

    r = requests.get(url=VERMILLION_URL + LATEST_ENDPOINT,
                     headers={'content-type': 'application/json'},
                     params=params,
                     verify=False)

    context.response = r.json()
    context.status_code = r.status_code
    print(context.status_code, context.response)

@when('A latest API query is without resource id')
def step_impl(context):
    params = ({

        "token": tokens["master"]

    })

    r = requests.get(url=VERMILLION_URL + LATEST_ENDPOINT,
                     headers={'content-type': 'application/json'},
                     params=params,
                     verify=False)

    context.response = r.json()
    context.status_code = r.status_code
    print(context.status_code, context.response)

@when('A latest API query is without token')
def step_impl(context):
    params = ({
        "id": res[1]


    })

    r = requests.get(url=VERMILLION_URL + LATEST_ENDPOINT,
                     headers={'content-type': 'application/json'},
                     params=params,
                     verify=False)

    context.response = r.json()
    context.status_code = r.status_code
    print(context.status_code, context.response)

@when('A latest API query is with empty token')
def step_impl(context):
    params = {
        "id": res[0],
        "token": ""

    }

    r = requests.get(url=VERMILLION_URL + LATEST_ENDPOINT,
                     headers={'content-type': 'application/json'},
                     params=params,
                     verify=False)

    context.response = r.json()
    context.status_code = r.status_code
    print(context.status_code, context.response)

@when('A latest API query is with invalid token')
def step_impl(context):
    params = ({
        "id": res[0],
        "token": generate_random_chars()

    })

    r = requests.get(url=VERMILLION_URL + LATEST_ENDPOINT,
                     headers={'content-type': 'application/json'},
                     params=params,
                     verify=False)

    context.response = r.json()
    context.status_code = r.status_code
    print(context.status_code, context.response)
