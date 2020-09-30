import pika

credentials = pika.PlainCredentials("poornachandra@iisc.ac.in", 
        "auth.iudx.org.in/poornachandra@iisc.ac.in/c490ffb8a4fe66992bfb1d302eae8732")
parameters = pika.ConnectionParameters(host='localhost',port=5672, credentials=credentials)
connection = pika.BlockingConnection(parameters)
channel = connection.channel()
channel.confirm_delivery()

def callback(ch, method, properties, body):
    print(" [x] %r" % body)

try:
    #channel.basic_publish(exchange='EXCHANGE',
    #                  routing_key="data",
    #                  body="hello")
    #channel.exchange_declare(exchange='logs',
    #                     exchange_type='fanout')
    #channel.queue_declare(queue="test")
    #channel.queue_bind(exchange="amq.topic", queue="DATABASE", routing_key="#")

    channel.basic_consume(queue="DATABASE", on_message_callback=callback, auto_ack=True)
    channel.start_consuming()
except Exception as e:
    print(e)

