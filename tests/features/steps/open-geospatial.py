import json

import requests
import urllib3

from behave import when
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from utils import check_search, generate_random_chars

VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


@when('The geospatial query body is empty')
def step_impl(context):
    payload = {}

    check_search("", payload, context)


@when('The geospatial query resource id is empty')
def step_impl(context):
    payload = {
        "id": "",
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": "10000m"
        }
    }

    check_search("", payload, context)


@when('The geospatial query has only resource id')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public"
    }

    check_search("", payload, context)


@when('The geospatial query resource id is invalid')
def step_impl(context):
    payload = {
        "id": generate_random_chars() + ".public",
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": "10000m"
        }
    }

    check_search("", payload, context)

@when('The geospatial query resource id is number')
def step_impl(context):
    payload = {
        "id": 123,
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": "10000m"
        }
    }

    check_search("", payload, context)
@when('The geospatial query body is invalid')
def step_impl(context):
    payload = generate_random_chars()

    check_search("", payload, context)


@when('The geospatial query coordinates are not present')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "geo_distance": {

            "distance": "10000m"
        }
    }

    check_search("", payload, context)


@when('The geospatial query coordinates are invalid')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "geo_distance": {
            "coordinates": [generate_random_chars(), generate_random_chars()],
            "distance": "10000m"
        }
    }

    check_search("", payload, context)


@when('The geospatial query coordinates are empty')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "geo_distance": {
            "coordinates": [],
            "distance": "10000m"
        }
    }

    check_search("", payload, context)


@when('The geospatial query distance is invalid')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": generate_random_chars()
        }
    }

    check_search("", payload, context)


@when('The geospatial query distance is not present')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],

        }
    }

    check_search("", payload, context)


@when('The geospatial query distance is empty')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": ""
        }
    }

    check_search("", payload, context)


@when('A geo-spatial query with distance in cm')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": "10000cm"
        }
    }

    check_search("", payload, context)


@when('A geo-spatial query with distance in mm')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": "10000mm"
        }
    }

    check_search("", payload, context)


@when('A geo-spatial query with distance in km')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": "10000km"
        }
    }

    check_search("", payload, context)


@when('A geo-spatial query with distance not in string')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": 100
        }
    }

    check_search("", payload, context)


@when('A geo-spatial query coordinates size is 1')
def step_impl(context):
    context.type = 'geospatial'

    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "geo_distance": {
            "coordinates": [82.9739],
            "distance": "10000m"
        }
    }

    check_search("", payload, context)


@when('A geo-spatial query coordinates size is 3')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "geo_distance": {
            "coordinates": [82.9739, 25.3176, 73.737],
            "distance": "10000m"
        }
    }

    check_search("", payload, context)


@when('A geo-spatial query coordinates values are negative')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "geo_distance": {
            "coordinates": [-82.9739, -89.999],
            "distance": "10000m"
        }
    }

    check_search("", payload, context)


@when('A geo-spatial query coordinates with invalid json array')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "geo_distance": {
            "coordinates": "True",
            "distance": "10000m"
        }
    }

    check_search("", payload, context)


@when('A geo-spatial query distance with invalid json object')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": "True"
        }
    }

    check_search("", payload, context)


@when('A geo-spatial query geodistance with invalid json object')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "geo_distance": "True"

    }

    check_search("", payload, context)


@when('A geo-spatial query is initiated')
def step_impl(context):
    context.type = 'geospatial'

    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": "10000m",

        }
    }

    r = requests.post(url=VERMILLION_URL + SEARCH_ENDPOINT,
                      headers={'content-type': 'application/json'},
                      data=json.dumps(payload),
                      verify=False)

    context.response = r.json()


@when('A geo-spatial query is initiated for distance in M')
def step_impl(context):
    context.type = 'geospatial'

    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": "10000M",

        }
    }

    r = requests.post(url=VERMILLION_URL + SEARCH_ENDPOINT,
                      headers={'content-type': 'application/json'},
                      data=json.dumps(payload),
                      verify=False)

    context.response = r.json()
