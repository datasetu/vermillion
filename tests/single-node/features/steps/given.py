import requests
from behave import given, when, then, step
import urllib3
from requests.packages.urllib3.exceptions import InsecureRequestWarning

VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


@given('Vermillion is running')
def step_impl(context):

    try:
        r = requests.get(VERMILLION_URL, verify=False)
    except (requests.exceptions.ConnectionError, requests.exceptions.Timeout):
        context.failed = True
