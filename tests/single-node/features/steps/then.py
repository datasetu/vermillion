import requests
from behave import given, when, then, step
import urllib3
from requests.packages.urllib3.exceptions import InsecureRequestWarning

VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


@then('All matching records are returned')
def step_impl(context):
    assert context.failed is False

    if context.type == 'timeseries':
        assert len(context.response) == 2000

    #TODO: Add attribute term tests
    if context.type == 'attribute-term':
        assert True == True

    if context.type == 'geospatial':
        assert len(context.response) == 5705
    if context.type == 'attribute-value':
        assert len(context.response) == 634
    if context.type == 'complex':
        assert len(context.response) == 305


@then('The response status should be {expected_code}')
def step_impl(context, expected_code):
    assert context.status_code == int(expected_code)
