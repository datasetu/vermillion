from	alpine:3.10.2

add	https://github.com/ufoscout/docker-compose-wait/releases/download/2.5.1/wait /wait
run	chmod +x /wait

run	apk update			    &&  \
	apk add python2-dev py-pip	    &&	\
	apk add build-base postgresql-dev   &&  \
	pip install psycopg2 pika	    &&	\
	apk del --purge build-base

cmd	/wait && python /unbind-daemon/daemon.py
