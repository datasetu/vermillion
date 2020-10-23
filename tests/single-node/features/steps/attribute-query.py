import requests
from behave import given, when, then, step
import urllib3
from requests.packages.urllib3.exceptions import InsecureRequestWarning

VERMILLION_URL='https://localhost'
SEARCH_ENDPOINT='/search'

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

#@given('Vermillion is running')
#def step_impl(context):
#
#    try:
#        r = requests.get(VERMILLION_URL, verify=False)
#    except (requests.exceptions.ConnectionError, requests.exceptions.Timeout):
#        context.failed = True
@when('The attribute value query payload is empty')
def step_impl(context):

    payload='{}'

#    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","attribute":{"term":"speed","min":30,"max":50}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code=r.status_code

@when('The attribute value query payload is invalid')
def step_impl(context):

    payload='{jsdbfksbfsbfjhwve24r24iyr29r}'

#    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","attribute":{"term":"speed","min":30,"max":50}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code=r.status_code
@when('The attribute value query payload id is empty')
def step_impl(context):



    payload='{"id":"","attribute":{"term":"speed","min":30,"max":50}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code=r.status_code


@when('The attribute value query payload id is invalid')
def step_impl(context):



    payload='{"id":"hjsbdvhjsbvkhjsvskhdvkshbv378748242468246","attribute":{"term":"speed","min":30,"max":50}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code=r.status_code
@when('The attribute value query payload attributes are empty')
def step_impl(context):



    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","attribute":{"term":"","min":,"max":}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code=r.status_code

@when('The attribute value query payload has only id')
def step_impl(context):



    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public"}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code=r.status_code



@when('The attribute value query payload attributes are invalid')
def step_impl(context):



    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","attribute":{"term":"^^","min":$$$,"max":%#%#%#%}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code=r.status_code

@when('An attribute value query is initiated')
def step_impl(context):

    context.type = 'attribute-value'

    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","attribute":{"term":"speed","min":30,"max":50}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()

#@then('All matching records are returned')
#def step_impl(context):
#    assert context.failed is False
#    if  context.type == 'attribute-value':
#        assert len(context.response) == 634
#@then('The response status should be "{text}"')
#def step_impl(context, text):
#    if text not in context.response:
#       assert('%r not in %r' % (text, context.response))
#       assert context.status_code == text   
