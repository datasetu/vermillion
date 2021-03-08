from auth_vars import urllib3, generate_random_chars, requests, json
from behave import when
from requests.packages.urllib3.exceptions import InsecureRequestWarning

VERMILLION_URL = 'https://localhost'
LATEST_ENDPOINT = '/latest'

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


@when('A latest API query is initiated')
def step_impl(context):
    context.type = 'latest-api'


    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public"
    }

    r = requests.post(url=VERMILLION_URL + LATEST_ENDPOINT,
                      headers={'content-type': 'application/json'},
                      data=json.dumps(payload),
                      verify=False)

    context.response = r.json()


@when('A latest API query with invalid resource id')
def step_impl(context):


    payload = {
        "id":
            generate_random_chars()


    }

    r = requests.post(url=VERMILLION_URL + LATEST_ENDPOINT,
                      headers={'content-type': 'application/json'},
                      data=json.dumps(payload),
                      verify=False)

    context.response = r.json()
    context.status_code=r.status_code
    print(context.status_code,context.response)

@when('A latest API query is with empty resource id')
def step_impl(context):


    payload = {
        "id": "",

    }

    r = requests.post(url=VERMILLION_URL + LATEST_ENDPOINT,
                      headers={'content-type': 'application/json'},
                      data=json.dumps(payload),
                      verify=False)

    context.response = r.json()
    context.status_code=r.status_code
    print(context.status_code,context.response)

