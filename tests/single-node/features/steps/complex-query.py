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
#
@when('The complex query payload is empty')
def step_impl(context):

    payload='{}'

#    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","attribute":{"term":"speed","min":30,"max":50},"time":{"start":"2020-01-01","end":"2020-06-01"},"geo_distance":{"coordinates":[82.9739,25.3176],"distance":"5000m"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code

@when('The complex query payload is invalid')
def step_impl(context):

    payload='{uhsdvjhyuwuyfywhfiy2487y7924yr7}'

#    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","attribute":{"term":"speed","min":30,"max":50},"time":{"start":"2020-01-01","end":"2020-06-01"},"geo_distance":{"coordinates":[82.9739,25.3176],"distance":"5000m"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code
@when('The complex query payload id is empty')
def step_impl(context):



    payload='{"id":"","attribute":{"term":"speed","min":30,"max":50},"time":{"start":"2020-01-01","end":"2020-06-01"},"geo_distance":{"coordinates":[82.9739,25.3176],"distance":"5000m"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code


@when('The complex query payload id is invalid')
def step_impl(context):



    payload='{"id":"hbjsdvhjbsvbsvbsvjbsjfbwy3747","attribute":{"term":"speed","min":30,"max":50},"time":{"start":"2020-01-01","end":"2020-06-01"},"geo_distance":{"coordinates":[82.9739,25.3176],"distance":"5000m"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code
@when('The complex query payload attributes are empty')
def step_impl(context):



    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","attribute":{"term":"","min":,"max":},"time":{"start":"2020-01-01","end":"2020-06-01"},"geo_distance":{"coordinates":[82.9739,25.3176],"distance":"5000m"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code


@when('The complex query payload attributes are invalid')
def step_impl(context):



    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","attribute":{"term":"&#&*#&*","min":^#^&#,"max":hjbdsbhjcds},"time":{"start":"2020-01-01","end":"2020-06-01"},"geo_distance":{"coordinates":[82.9739,25.3176],"distance":"5000m"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code
@when('The complex query payload time is empty')
def step_impl(context):



    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","attribute":{"term":"speed","min":30,"max":50},"time":{"start":"","end":""},"geo_distance":{"coordinates":[82.9739,25.3176],"distance":"5000m"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code


@when('The complex query payload time is invalid')
def step_impl(context):



    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","attribute":{"term":"speed","min":30,"max":50},"time":{"start":"jhbsdjbhdsbhj","end":""},"geo_distance":{"coordinates":[82.9739,25.3176],"distance":"5000m"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code

@when('The complex query payload coordinates are empty')
def step_impl(context):



    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","attribute":{"term":"speed","min":30,"max":50},"time":{"start":"2020-01-01","end":"2020-06-01"},"geo_distance":{"coordinates":[,],"distance":"5000m"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code


@when('The complex query payload coordinates are invalid')
def step_impl(context):



    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","attribute":{"term":"speed","min":30,"max":50},"time":{"start":"2020-01-01","end":"2020-06-01"},"geo_distance":{"coordinates":[hbsddhjbs,hbdhdfhb],"distance":"5000m"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code
@when('The complex query payload distance is empty')
def step_impl(context):



    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","attribute":{"term":"speed","min":30,"max":50},"time":{"start":"2020-01-01","end":"2020-06-01"},"geo_distance":{"coordinates":[82.9739,25.3176],"distance":""}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code


@when('The complex query payload distance is invalid')
def step_impl(context):



    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","attribute":{"term":"speed","min":30,"max":50},"time":{"start":"2020-01-01","end":"2020-06-01"},"geo_distance":{"coordinates":[82.9739,25.3176],"distance":"^&#^&#^&#"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code

@when('A complex query is initiated')
def step_impl(context):

    context.type = 'complex'

    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","attribute":{"term":"speed","min":30,"max":50},"time":{"start":"2020-01-01","end":"2020-06-01"},"geo_distance":{"coordinates":[82.9739,25.3176],"distance":"5000m"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
#@then('All matching records are returned')
#def step_impl(context):
#    assert context.failed is False
#    if  context.type == 'complex':
#        assert len(context.response) == 305
#
#@then('The response status should have "{text}"')
#
#def step_impl(context, text):
#    if text not in context.response:
#       assert('%r not in %r' % (text, context.response))
#       assert context.status_code == text
#
