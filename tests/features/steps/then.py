import requests
from behave import given, when, then, step
import urllib3
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from utils import ResponseCountMismatchError, UnexpectedStatusCodeError

VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


@then('All matching records are returned')
def step_impl(context):

    if context.type == 'timeseries':
        if len(context.response) != 2000:
            raise ResponseCountMismatchError(2000, len(context.response))

    #TODO: Add attribute term tests
    if context.type == 'attribute-term':
        pass

    if context.type == 'geospatial':
        if len(context.response) != 5705:
            raise ResponseCountMismatchError(5705, len(context.response))

    if context.type == 'attribute-value':
        if len(context.response) != 634:
            raise ResponseCountMismatchError(634, len(context.response))

    if context.type == 'complex':
        if len(context.response) != 305:
            raise ResponseCountMismatchError(305, len(context.response))


@then('The response status should be {expected_code}')
def step_impl(context, expected_code):
    if context.status_code != int(expected_code):
        raise UnexpectedStatusCodeError(int(expected_code), context.status_code, context.response)
