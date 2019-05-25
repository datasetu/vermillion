from	alpine:latest

copy	daemon.py / 

run	apk update			    &&  \
	apk upgrade			    &&  \
	apk add python2-dev py-pip	    &&	\
	apk add build-base postgresql-dev   &&  \
	pip install psycopg2 pika	    &&	\
	apk del --purge build-base

cmd	./daemon.py
