import requests
from behave import given
import urllib3
from requests.packages.urllib3.exceptions import InsecureRequestWarning

VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


@given('Vermillion is running')
def step_impl(context):
    context.type = 'running_server'

    try:
        requests.get(VERMILLION_URL, verify=False)
    except (requests.exceptions.ConnectionError, requests.exceptions.Timeout):
        context.failed = True
