import os
import psycopg2
import pika
import time
import sys

conn    = None
cur     = None
channel = None

def run():

    admin_passwd 	= os.getenv("ADMIN_PWD")
    postgres_passwd 	= os.getenv("POSTGRES_PWD")

    conn = psycopg2.connect(database="postgres", user = "postgres", password = postgres_passwd, host = "127.0.0.1", port = "5432")

    cur = conn.cursor()
    credentials = pika.PlainCredentials('admin', admin_passwd)
    parameters = pika.ConnectionParameters('broker',5672, '/', credentials)

    while True:
	try:
	    print("Connecting to broker ...")
	    connection = pika.BlockingConnection(parameters)
	    print("Connected")
	    break
	except Exception:
            sys.stderr.write("Failed to connect to broker\n")

    channel = connection.channel()

    queue       = ""
    exchange    = ""
    topic       = ""

    while True:

         print("Looking for expired entries ...")
         cur.execute("SELECT from_id, exchange, topic FROM acl WHERE valid_till < NOW()")
         rows = cur.fetchall()

         if rows:
            for row in rows:
                queue       = row[0]
                exchange    = row[1]
                topic       = row[2]

            try:
                channel.queue_unbind(exchange=exchange, queue=queue, routing_key = topic)
                channel.queue_unbind(exchange=exchange, queue=queue+".priority", routing_key = topic)
                channel.queue_unbind(exchange=queue+".publish", queue=exchange, routing_key = exchange+"."+topic)
            except Exception:
                sys.stderr.write("Failed to unbind q="+queue+" e="+exchange+" t="+topic)

            cur.execute("DELETE FROM acl WHERE from_id = %s AND exchange = %s AND topic = %s",(queue, exchange, topic,))
            conn.commit()

            print("Deleted acl entry and binding between "+queue+" and "+exchange)

         time.sleep(60)

if __name__ == '__main__':
    run()
