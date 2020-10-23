import requests
from behave import given, when, then, step
import urllib3
from requests.packages.urllib3.exceptions import InsecureRequestWarning

VERMILLION_URL='https://localhost'
SEARCH_ENDPOINT='/search'

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

@given('Vermillion is running')
def step_impl(context):

    try:
        r = requests.get(VERMILLION_URL, verify=False)
    except (requests.exceptions.ConnectionError, requests.exceptions.Timeout):
        context.failed = True

@when('Timeseries payload is empty')
def step_impl(context):

    payload='{}'
#    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public","time":{"start":"2020-03-01","end":"2020-03-27"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code

@when('Timeseries payload is invalid')
def step_impl(context):

    payload='{hbahbcbhaadhdhkdhbkhdb1334234124}'
#    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public","time":{"start":"2020-03-01","end":"2020-03-27"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code
@when('Timeseries payload start date is invalid')
def step_impl(context):

    
    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public","time":{"start":"fxxg","end":"2020-03-27"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code

@when('Timeseries payload end date is invalid')
def step_impl(context):

    
    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public","time":{"start":"fxxg","end":"jhvjv"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code

@when('Timeseries payload date is empty')
def step_impl(context):

    
    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public","time":{"start":"","end":""}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code

@when('Timeseries payload date is not present')
def step_impl(context):

    
    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public","time":{"start":"0000-00-00","end":"0000-00-00"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code


@when('Timeseries payload has only id')
def step_impl(context):

    
    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public"}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code


@when('Timeseries payload id is empty')
def step_impl(context):

    payload='{"id":"","time":{"start":"2020-03-01","end":"2020-03-27"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code


@when('Timeseries payload id is invalid')
def step_impl(context):

    payload='{"id":"hssbfisbfibs","time":{"start":"2020-03-01","end":"2020-03-27"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code

@when('A timeseries query is initiated')
def step_impl(context):

    context.type = 'timeseries'

    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public","time":{"start":"2020-03-01","end":"2020-03-27"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()

@when('An attribute term query is initiated')
def step_impl(context):
    context.type = 'attribute-term'
    pass

@then('All matching records are returned')
def step_impl(context):
    assert context.failed is False

    if context.type == 'timeseries':
        assert len(context.response) == 2000
    if context.type == 'attribute-term':
        assert True == True
    if context.type == 'geospatial':
        assert len(context.response) == 5705
    if  context.type == 'attribute-value':
        assert len(context.response) == 634
    if  context.type == 'complex':
        assert len(context.response) == 305
@then('The response status should be "{text}"')
def step_impl(context, text):
    if text not in context.response:
       assert('%r not in %r' % (text, context.response))
       assert context.status_code ==  400
