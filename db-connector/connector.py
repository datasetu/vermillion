import pika
import json
import os
from elasticsearch import Elasticsearch
import logging
import jsonschema
from jsonschema import validate

broker_host = os.getenv("RABBITMQ_HOSTNAME")
broker_port = 5672
broker_username = os.getenv("RABBITMQ_USER")
broker_pwd = os.getenv("RABBITMQ_ADMIN_PASS")
broker_queue = "DATABASE"
es_host = os.getenv("ES_HOSTNAME")
es_default_index = os.getenv("ES_DEFAULT_INDEX")
es_latest_index = os.getenv("ES_LATEST_INDEX")

#Elastcisearch client object for inserting docs
es = None

#RabbitMQ channel for consuming messages
channel = None


def connect_to_es():

    global es

    try:
        es = Elasticsearch([es_host],
                           retry_on_timeout=True,
                           http_compress=True)
    except Exception as e:
        logging.error("Error while connecting to elasticsearch: {}".format(e))

    logging.info("Connected to elasticsearch")

    mapping = {
        "settings": {
            "index.mapping.ignore_malformed": True
        },
        "mappings": {
            "properties": {
                "data": {
                    "type": "object"
                },
                "timestamp": {
                    "type": "date"
                },
                "coordinates": {
                    "type": "geo_point"
                }
            }
        }
    }

    es.indices.create(index=es_default_index, body=mapping, ignore=400)
    es.indices.create(index=es_latest_index, body=mapping, ignore=400)

    logging.info("Archive and latest indices are ready")


def connect_to_rabbit():
    global channel

    try:
        credentials = pika.PlainCredentials(broker_username, broker_pwd)
        parameters = pika.ConnectionParameters(host=broker_host,
                                               port=broker_port,
                                               credentials=credentials)
        connection = pika.BlockingConnection(parameters)
        channel = connection.channel()

    except Exception as e:
        logging.error("Errored while connecting to rabbitmq: {}".format(e))

    logging.info("Connected to rabbitmq")


def callback(ch, method, properties, body):

    logging.info(body)
    '''
    Format of the message has to be the following

    {
        "data": <JSON Object>,
        "id": <resource-id>,
        "category": <resource-group>,
        "coordinates": <lat long coordinates if present>,
        "timestamp": <timestamp of the data>
    }
    '''
    schema = {
        "$schema": "http://json-schema.org/draft-04/schema#",
        "type": "object",
        "properties": {
            "data": {
                "type": "object",
            },
            "id": {
                "type":
                "string",
                "pattern":
                "[a-z_.\-]+\/[a-f0-9]{40}\/[a-z_.\-]+\/[a-zA-Z0-9_.\-]+\/[a-zA-Z0-9_.\-]+"
            },
            "category": {
                "type": "string"
            },
            "coordinates": {
                "type":
                "array",
                "items": [{
                    "type": ["number", "integer"]
                }, {
                    "type": ["number", "integer"]
                }]
            },
            "timestamp": {
                "type": "string",
                "format": "date-time"
            }
        },
        "required": ["data", "id", "category", "timestamp"]
    }
    try:
        body_dict = json.loads(body)
    except Exception as e:
        logging.error("Message is not a valid JSON. Rejecting...")
        return
    
    try:
        validate(body_dict, schema, format_checker=jsonschema.FormatChecker())
    except ValidationError as e:
        logging.error("Data does not conform to the required format. Rejecting...")
        return
    
    if method.routing_key != body_dict['id']:
        logging.error("Resource IDs in routing key and body are different. Rejecting...")
        return

    try:
        es.index(index="archive", body=body_dict)
    except Exception as e:
        logging.error("Error inserting document into elastic: {}".format(e))
        connect_to_es()
        fetch_from_queue()


def fetch_from_queue():

    logging.info("Fetching from the database queue...")

    try:
        channel.basic_consume(queue=broker_queue,
                              on_message_callback=callback,
                              auto_ack=True)
        channel.start_consuming()
    except Exception as e:
        logging.error("Errored while consuming from queue: ".format(e))
        connect_to_rabbit()
        fetch_from_queue()


if __name__ == "__main__":

    logging.getLogger("pika").setLevel(logging.ERROR)
    logging.getLogger("elasticsearch").setLevel(logging.ERROR)
    logging.basicConfig(level=logging.INFO)

    connect_to_rabbit()
    connect_to_es()
    fetch_from_queue()
