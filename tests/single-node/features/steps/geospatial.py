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

@when('The payload is empty')
def step_impl(context):
    context.type = 'nopayload'
    payload='{}'
#    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","geo_distance":{"coordinates":[82.9739,25.3176],"distance":"10000m"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code

@when('The payload id is empty')
def step_impl(context):
    context.type = 'nopayload'

    payload='{"id":"","geo_distance":{"coordinates":[82.9739,25.3176],"distance":"10000m"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code


@when('The payload id is invalid')
def step_impl(context):
    context.type = 'nopayload'

    payload='{"id":"jhkvsbhvdjhbfd","geo_distance":{"coordinates":[82.9739,25.3176],"distance":"10000m"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code
@when('The payload is random')
def step_impl(context):

    context.type = 'randompayload'
    payload='{hsbdsbdbsdfkhbsfhk}'
#    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","geo_distance":{"coordinates":[82.9739,25.3176],"distance":"10000m"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code
@when('The coordinates are invalid')
def step_impl(context):


    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","geo_distance":{"coordinates":[xyz,abc],"distance":"10000m"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code
@when('The coordinates are empty')
def step_impl(context):


    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","geo_distance":{"coordinates":[],"distance":"10000m"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code

@when('The distance is invalid')
def step_impl(context):


    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","geo_distance":{"coordinates":[82.9739,25.3176],"distance":"xyz"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code
@when('The distance is empty')
def step_impl(context):


    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","geo_distance":{"coordinates":[82.9739,25.3176],"distance":""}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code


@when('A geo-spatial query is initiated')
def step_impl(context):

    context.type = 'geospatial'

    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","geo_distance":{"coordinates":[82.9739,25.3176],"distance":"10000m"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
#@then('All matching records are returned')
#def step_impl(context):
#    assert context.failed is False
#
#    if context.type == 'geospatial':
#        assert len(context.response) == 5705
#@then('The response status should have "{text}"')
#def step_impl(context, text):
#    if text not in context.response:
#       assert('%r not in %r' % (text, context.response))
#       assert context.status_code == text

