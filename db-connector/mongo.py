import pymongo
import pika
import json
import time
import os
import dateutil.parser

class dbconnector():

    def __init__(self, broker_username, broker_pwd, mongo_username, mongo_pwd):
	self.broker_username	=   broker_username
	self.broker_pwd		=   broker_pwd
	self.mongo_username	=   mongo_username
	self.mongo_pwd		=   mongo_pwd
	self.archive		=   None
	self.channel		=   None

	self.connect_to_mongo()
	self.connect_to_rabbit()

    def connect_to_mongo(self):
	connecton_str	=   "mongodb://"+self.mongo_username+":"+self.mongo_pwd+"@mongo"
	client		=   pymongo.MongoClient(connecton_str)
	db		=   client["resource_server"]
	self.archive	=   db.archive
	print("Connected to mongo")

    def connect_to_rabbit(self):

	credentials	=   pika.PlainCredentials(self.broker_username, self.broker_pwd)
	parameters	=   pika.ConnectionParameters(host='rabbit',
			    port=5672, credentials=credentials)
	connection	=   pika.BlockingConnection(parameters)
	self.channel	=   connection.channel()
	self.channel.confirm_delivery()
	print("Connected to rabbitmq")

    def is_json(self, body):
	try:
	   json.loads(body)
	   return True
	except Exception:
	    return False

    def push_to_mongo(self):
	start	=   time.time()

	while True:
	    try:
	        for (method_frame, properties, body) in self.channel.consume("DATABASE", inactivity_timeout=1):

		    if not method_frame:
		    	continue

		    else:
			self.channel.basic_ack(method_frame.delivery_tag)
			print("Body="+body)

			if not self.is_json(body):
			    print("Message needs to be a JSON. Rejecting...")
			    continue
			else:
			    body_dict   =   json.loads(body)

			    if "__time" in body_dict:
				time_str    =   body_dict["__time"]
				body_dict["__time"]   = dateutil.parser.parse(time_str)

			    if self.archive.find(body_dict).count() == 0:
				print("Mongo insert="+str(self.archive.insert_one(body_dict)))

	    except Exception as e:
	        print(e)
	        self.connect_to_mongo()
	        self.connect_to_rabbit()

	    time.sleep(10.0 - ((time.time() - start) % 10.0))

if __name__ ==	"__main__":

    broker_username =	"admin"
    broker_pwd	    =	os.getenv("ADMIN_PWD")
    mongo_username  =	"root"
    mongo_pwd	    =	os.getenv("MONGO_INITDB_ROOT_PASSWORD")
    db		    =   dbconnector(broker_username, broker_pwd, mongo_username, mongo_pwd)

    db.push_to_mongo()
