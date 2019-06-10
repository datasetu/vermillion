FROM	alpine:latest

COPY	./docker-entrypoint.sh /

RUN	apk update									    &&	\
   	apk upgrade									    &&	\
   	echo "https://nl.alpinelinux.org/alpine/edge/testing/" >> /etc/apk/repositories	    &&	\
   	apk update									    &&	\
   	apk add rabbitmq-server								    &&	\
   	chmod -R 777 /usr/lib/rabbitmq							    &&	\
   	chmod -R 777 /etc								    &&	\
   	rabbitmq-plugins --offline enable rabbitmq_management				    &&	\
   	rabbitmq-plugins --offline enable rabbitmq_auth_backend_http			    &&	\ 
   	rabbitmq-plugins --offline enable rabbitmq_shovel				    &&	\
   	rabbitmq-plugins --offline enable rabbitmq_shovel_management			    &&	\
   	rabbitmq-plugins --offline enable rabbitmq_auth_backend_cache

ADD	https://github.com/ufoscout/docker-compose-wait/releases/download/2.5.0/wait /wait
RUN	chmod +x /wait

CMD	/wait && ./docker-entrypoint.sh	

