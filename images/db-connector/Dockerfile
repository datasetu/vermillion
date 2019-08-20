from	alpine:latest

add	https://github.com/ufoscout/docker-compose-wait/releases/download/2.5.1/wait /wait
run	chmod +x /wait

run	apk update			    &&	\
	apk upgrade			    &&	\   
	apk add python py-pip		    &&	\
	pip install pika requests pymongo   &&	\
	pip install python-dateutil


cmd	/wait && python /db-connector/mongo.py
