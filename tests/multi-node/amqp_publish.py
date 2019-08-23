import multiprocessing as mp
import urllib3
import time
import logging
import json
import random
import pika

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

logger = logging.getLogger(__name__)
logging.getLogger('urllib3').setLevel(logging.WARNING)

output = mp.Queue()

def publish(credentials):

    username	=   credentials[0]
    apikey	=   credentials[1]
    node	=   credentials[2]
    port	=   credentials[3]

    print("Started publisher for {0} at host={1} and port={2}".format(username, node, port))

    credentials	=   pika.PlainCredentials(username, apikey)
    parameters	=   pika.ConnectionParameters(host=node,
		    port=port, credentials=credentials)
    connection	=   pika.BlockingConnection(parameters)
    channel	=   connection.channel()

    channel.confirm_delivery()

    for _ in range(0, 10000):
	channel.basic_publish(exchange=username+".protected", routing_key="test", body="test payload")

    channel.close()
    connection.close()

if __name__ ==  "__main__":

    node_list	        =   ["node-1", "node-2", "node-3", "node-4"]
    node_index	        =   0
    device_keys	        =   json.loads(open("device_keys","r").read())
    publish_list        =   []
    device_name_list    =   [str(device) for device in device_keys]
    nodes               =   4


    for i in range(0, 100):

	dev_name	=   str(random.choice(device_name_list))
	bucket_number   =   (int(ord(dev_name[0]))-96)	%   nodes;
	broker_bucket   =   nodes if bucket_number == 0 else bucket_number
        node_index      =   node_index % len(node_list)
	broker_url	=   node_list[node_index]
        node_index      =   node_index  +   1
	port		=   5672	+   broker_bucket

	publish_list.append([dev_name, str(device_keys[dev_name]), broker_url, port])


    start	    =	time.time()
    pool	    =   mp.Pool(100)
    processes	    =   pool.map(publish, publish_list)
    time_taken	    =	time.time() - start
    print("Throughput = " +str(1000000/time_taken))
