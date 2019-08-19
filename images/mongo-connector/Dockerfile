from	alpine:latest

copy	mongo.py    /

run	apk update			    &&	\
	apk upgrade			    &&	\   
	apk add python py-pip		    &&	\
	pip install pika requests pymongo

cmd	python mongo.py
