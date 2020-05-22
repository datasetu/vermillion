import pika
import json
import os
from elasticsearch import Elasticsearch
import logging

broker_host     =   "rabbit"
broker_port     =   5672
broker_username =   "admin"
broker_pwd	=   os.getenv("ADMIN_PWD")
broker_queue    =   "DATABASE"
es_host         =   "elasticsearch"

#Elastcisearch client object for inserting docs
es              =   None

#RabbitMQ channel for consuming messages
channel         =   None

def connect_to_es():

    global es

    try:
        es = Elasticsearch([es_host], retry_on_timeout=True, http_compress=True)
    except Exception as e:
        logging.error("Error while connecting to elasticsearch: ", e)

    logging.info("Connected to elasticsearch")

    mapping =   {
                    "mappings": {
                        "properties": {
                            "data": {
                                "type": "object"
                                },
                            "blob": {
                                "type": "binary"
                                },
                            "timestamp": {
                                "type": "date"
                                },
                            "resource-id": {
                                "type": "keyword"
                                },
                            "category": {
                                "type": "keyword"
                                },
                            "coordinates": {
                                "type": "geo_point"
                                },
                            "mime-type": {
                                "type": "keyword"
                                }
                            }
                        }
                }
                
    es.indices.create(index="archive", body=mapping, ignore=400)
    es.indices.create(index="latest", body=mapping, ignore=400)

    logging.info("Archive and latest indices are ready")

def connect_to_rabbit():
    global channel

    try:
        credentials =   pika.PlainCredentials(broker_username, broker_pwd)
        parameters  =   pika.ConnectionParameters(
                                                    host=broker_host,
            		                            port=broker_port, 
                                                    credentials=credentials
                                                )
        connection  =   pika.BlockingConnection(parameters)
        channel	    =   connection.channel()

    except Exception as e:
        logging.error("Errored while connecting to rabbitmq: ", e)

    logging.info("Connected to rabbitmq")

def callback(ch, method, properties, body):

    logging.info(body)

    '''
    Format of the message has to be the following

    {
        "data": <JSON Object>,
        "resource-id": <resource-id>,
        "resource-group": <resource-group>,
        "coordinates": <lat long coordinates if present>,
        "timestamp": <timestamp of the data>
    }
    '''

    try:
        body_dict   =   json.loads(body)
    except Exception as e:
        logging.error("Message is not a valid JSON. Rejecting...")
        return

    try:
        es.index(index="archive", body=body_dict)
    except Exception as e:
        logging.error("Error inserting document into elastic: ", e)
        connect_to_es()
        fetch_from_queue()

def fetch_from_queue():

    logging.info("Fetching from the database queue...")

    try:
        channel.basic_consume(queue=broker_queue, on_message_callback=callback, auto_ack=True)
        channel.start_consuming()
    except Exception as e:
        logging.error("Errored while consuming from queue: ", e)
        connect_to_rabbit()
        fetch_from_queue()

if __name__ == "__main__":

    logging.getLogger("pika").setLevel(logging.ERROR)
    logging.getLogger("elasticsearch").setLevel(logging.ERROR)
    logging.basicConfig(level=logging.INFO)

    connect_to_rabbit()
    connect_to_es()
    fetch_from_queue()
