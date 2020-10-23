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

@when('The payload id is wrong')
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
@when('The coordinates are changed')
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

@when('The distance is changed')
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

@when('Timeseries payload is empty')
def step_impl(context):

    payload='{}'
#    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public","time":{"start":"2020-03-01","end":"2020-03-27"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code

@when('Timeseries payload is random')
def step_impl(context):

    payload='{hbahbcbhaadhdhkdhbkhdb1334234124}'
#    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public","time":{"start":"2020-03-01","end":"2020-03-27"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code
@when('Timeseries start date is random')
def step_impl(context):

    
    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public","time":{"start":"fxxg","end":"2020-03-27"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code

@when('Timeseries end date is random')
def step_impl(context):

    
    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public","time":{"start":"fxxg","end":"jhvjv"}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code

@when('Timeseries date is empty')
def step_impl(context):

    
    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-workers/varanasi-swm-wardwiseAttendance.public","time":{"start":"","end":""}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code = r.status_code

@when('Timeseries id is wrong')
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

@when('The attribute value query payload is empty')
def step_impl(context):

    payload='{}'

#    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","attribute":{"term":"speed","min":30,"max":50}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code=r.status_code

@when('The attribute value query payload is random')
def step_impl(context):

    payload='{jsdbfksbfsbfjhwve24r24iyr29r}'

#    payload='{"id":"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public","attribute":{"term":"speed","min":30,"max":50}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code=r.status_code

@when('The attribute value query payload id is wrong')
def step_impl(context):

    

    payload='{"id":"hjsbdvhjsbvkhjsvskhdvkshbv378748242468246","attribute":{"term":"speed","min":30,"max":50}}'

    r = requests.post(url=VERMILLION_URL+SEARCH_ENDPOINT, headers = {'content-type': 'application/json'}, data=payload, verify=False)

    context.response = r.json()
    context.status_code=r.status_code

@when('The attribute value query payload attributes are wrong')
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
#@then('Return the status as Bad request 400')
#def step_impl(context):
#    assert context.failed is False
#    
#    if context.type == 'nopayload':
#        assert context.status_code == 400
#    if context.type == 'randompayload':
#        assert context.status_code == 400
#    if context.type == 'coordinateschange':
#        assert context.status_code == 200
#    
@then('The response will have "{text}"')
def step_impl(context, text):
    if text not in context.response:
       assert('%r not in %r' % (text, context.response))
       assert context.status_code == 400
