import pymongo
import pika
import json
import time
import os
import dateutil.parser

broker_username =   "admin"
broker_pwd	=   os.getenv("ADMIN_PWD")
mongo_username  =   "root"
mongo_pwd       =   os.getenv("MONGO_INITDB_ROOT_PASSWORD")
archive         =   None
channel         =   None

def connect_to_mongo():
    global archive

    connecton_str   =   "mongodb://"+mongo_username+":"+mongo_pwd+"@mongo"
    client	    =   pymongo.MongoClient(connecton_str)
    db		    =   client["resource_server"]
    archive         =   db.archive
    archive.ensure_index([("__geoJsonLocation", pymongo.GEOSPHERE)])

    print("Connected to mongo")

def connect_to_rabbit():
    global channel

    credentials	=   pika.PlainCredentials(broker_username, broker_pwd)
    parameters	=   pika.ConnectionParameters(host='rabbit',
			    port=5672, credentials=credentials)
    connection	=   pika.BlockingConnection(parameters)
    channel	=   connection.channel()

    print("Connected to rabbitmq")

def is_json(body):
    try:
	json.loads(body)
	return True
    except Exception:
	return False

def callback(ch, method, properties, body):

    global archive

    #if not is_json(body):
	#print("Message needs to be JSON. Rejecting...")
    
    #else:

    print(body)
    
    body_dict   =   json.loads(body)

    if "__time" in body_dict:
	time_str    =   body_dict["__time"]
	body_dict["__time"]   = dateutil.parser.parse(time_str)

    try:
        print("Mongo insert="+str(archive.update(body_dict, body_dict, upsert=True)))
    except Exception as e:
        connect_to_mongo()
        fetch_from_queue()

def fetch_from_queue():

    print("Fetching from the database queue...")

    global channel

    try:
        channel.basic_consume(queue="DATABASE", on_message_callback=callback, auto_ack=True)
        channel.start_consuming()
    except Exception as e:
        connect_to_rabbit()
        fetch_from_queue()

connect_to_rabbit()
connect_to_mongo()
fetch_from_queue()
