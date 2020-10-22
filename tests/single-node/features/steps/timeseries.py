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

@when('The payload is empty')
def step_impl(context):
    context.type = 'nopayload'
    payload='{}'   
#    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","geo_distance":{"coordinates":[82.9739,25.3176],"distance":"10000m"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code

@when('A geo-spatial query is initiated')
def step_impl(context):

    context.type = 'geospatial'

    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","geo_distance":{"coordinates":[82.9739,25.3176],"distance":"10000m"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()


@when('A timeseries query is initiated')
def step_impl(context):

    context.type = 'timeseries'

    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public","time":{"start":"2020-03-01","end":"2020-03-27"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()


@when('An attribute value query is initiated')
def step_impl(context):

    context.type = 'attribute-value'

    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","attribute":{"term":"speed","min":30,"max":50}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()


@when('A complex query is initiated')
def step_impl(context):

    context.type = 'complex'

    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","attribute":{"term":"speed","min":30,"max":50},"time":{"start":"2020-01-01","end":"2020-06-01"},"geo_distance":{"coordinates":[82.9739,25.3176],"distance":"5000m"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()

@when('An attribute term query is initiated')
def step_impl(context):
    context.type = 'attribute-term'
    pass

@then('All matching records are returned')
def step_impl(context):
    assert context.failed is False

    if context.type == 'geospatial':
        assert len(context.response) == 5705
    if context.type == 'timeseries':
        assert len(context.response) == 2000
    if context.type == 'attribute-value':
        assert len(context.response) == 634
    if context.type == 'attribute-term':
        assert True == True
    if context.type == 'complex':
        assert len(context.response) == 305
@then('Return the status as Bad request 400')
def step_impl(context):
    assert context.failed is False
    
    if context.type == 'nopayload':
        assert context.status_code == 400

