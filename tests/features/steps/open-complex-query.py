import json
import requests
import urllib3
from behave import when
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from utils import  generate_random_chars, post_request

VERMILLION_URL = 'https://localhost'
SEARCH_ENDPOINT = '/search'
url= VERMILLION_URL+SEARCH_ENDPOINT

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


@when('The complex query body is empty')
def step_impl(context):
    payload = '{}'

    post_request(url, "", json.dumps(payload), context)


@when('The complex query body is invalid')
def step_impl(context):
    payload = generate_random_chars()

    post_request(url, "", json.dumps(payload), context)


@when('The complex query resource id is empty')
def step_impl(context):
    payload = {
        "id": "",
        "attribute": {
            "term": "speed",
            "min": 30,
            "max": 50
        },
        "time": {
            "start": "2020-01-01",
            "end": "2020-06-01"
        },
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": "5000m"
        }
    }

    post_request(url, "", json.dumps(payload), context)


@when('The complex query resource id is invalid')
def step_impl(context):
    payload = {
        "id": generate_random_chars() + ".public",
        "attribute": {
            "term": "speed",
            "min": 30,
            "max": 50
        },
        "time": {
            "start": "2020-01-01",
            "end": "2020-06-01"
        },
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": "5000m"
        }
    }

    post_request(url, "", json.dumps(payload), context)


@when('The complex query has only resource id')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public"
    }

    post_request(url, "", json.dumps(payload), context)


@when('The complex query attributes are empty')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": "",
            "min": "",
            "max": ""
        },
        "time": {
            "start": "2020-01-01",
            "end": "2020-06-01"
        },
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": "5000m"
        }
    }

    post_request(url, "", json.dumps(payload), context)


@when('The complex query attributes are invalid')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": generate_random_chars(),
            "min": generate_random_chars(),
            "max": generate_random_chars()
        },
        "time": {
            "start": "2020-01-01",
            "end": "2020-06-01"
        },
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": "5000m"
        }
    }

    post_request(url, "", json.dumps(payload), context)


@when('The complex query time is empty')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": "speed",
            "min": 30,
            "max": 50
        },
        "time": {
            "start": "",
            "end": ""
        },
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": "5000m"
        }
    }

    post_request(url, "", json.dumps(payload), context)


@when('The complex query time is not present')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": "speed",
            "min": 30,
            "max": 50
        },
        "time": {
            "start": "0000-00-00",
            "end": "0000-00-00"
        },
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": "5000m"
        }
    }

    post_request(url, "", json.dumps(payload), context)


@when('The complex query time is invalid')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": "speed",
            "min": 30,
            "max": 50
        },
        "time": {
            "start": generate_random_chars(),
            "end": ""
        },
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": "5000m"
        }
    }

    post_request(url, "", json.dumps(payload), context)


@when('The complex query coordinates are empty')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": "speed",
            "min": 30,
            "max": 50
        },
        "time": {
            "start": "2020-01-01",
            "end": "2020-06-01"
        },
        "geo_distance": {
            "coordinates": ["", ""],
            "distance": "5000m"
        }
    }

    post_request(url, "", json.dumps(payload), context)


@when('The complex query coordinates are invalid')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": "speed",
            "min": 30,
            "max": 50
        },
        "time": {
            "start": "2020-01-01",
            "end": "2020-06-01"
        },
        "geo_distance": {
            "coordinates": [0.1232323, 26262.2],
            "distance": "5000m"
        }
    }

    post_request(url, "", json.dumps(payload), context)


@when('The complex query coordinates are strings')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": "speed",
            "min": 30,
            "max": 50
        },
        "time": {
            "start": "2020-01-01",
            "end": "2020-06-01"
        },
        "geo_distance": {
            "coordinates": [generate_random_chars(), generate_random_chars()],
            "distance": "5000m"
        }
    }

    post_request(url, "", json.dumps(payload), context)


@when('The complex query distance is empty')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": "speed",
            "min": 30,
            "max": 50
        },
        "time": {
            "start": "2020-01-01",
            "end": "2020-06-01"
        },
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": ""
        }
    }

    post_request(url, "", json.dumps(payload), context)


@when('The complex query distance is not present')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": "speed",
            "min": 30,
            "max": 50
        },
        "time": {
            "start": "2020-01-01",
            "end": "2020-06-01"
        },
        "geo_distance": {
            "coordinates": [82.9739, 25.3176]

        }
    }

    post_request(url, "", json.dumps(payload), context)


@when('The complex query distance is invalid')
def step_impl(context):
    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": "speed",
            "min": 30,
            "max": 50
        },
        "time": {
            "start": "2020-01-01",
            "end": "2020-06-01"
        },
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": generate_random_chars()
        }
    }

    post_request(url, "", json.dumps(payload), context)


@when('The complex query has only resource attributes')
def step_impl(context):
    payload = {

        "attribute": {
            "term": "speed",
            "min": 30,
            "max": 50
        },

    }

    post_request(url, "", json.dumps(payload), context)


@when('The complex query has only resource time')
def step_impl(context):
    payload = {

        "time": {
            "start": "2020-01-01",
            "end": "2020-06-01"
        },

    }

    post_request(url, "", json.dumps(payload), context)


@when('The complex query has only resource geo-distance')
def step_impl(context):
    payload = {

        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": "5000m"
        }
    }

    post_request(url, "", json.dumps(payload), context)


@when('A complex query is initiated')
def step_impl(context):
    context.type = 'complex'

    payload = {
        "id":
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live.public",
        "attribute": {
            "term": "speed",
            "min": 30,
            "max": 50
        },
        "time": {
            "start": "2020-01-01",
            "end": "2020-06-01"
        },
        "geo_distance": {
            "coordinates": [82.9739, 25.3176],
            "distance": "5000m"
        }
    }

    r = requests.post(url=VERMILLION_URL + SEARCH_ENDPOINT,
                      headers={'content-type': 'application/json'},
                      data=json.dumps(payload),
                      verify=False)

    context.response = r.json()['hits']
