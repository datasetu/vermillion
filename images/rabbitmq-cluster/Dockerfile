FROM	iudx/rabbitmq-hipe	

COPY	./docker-entrypoint.sh /

ADD	https://github.com/ufoscout/docker-compose-wait/releases/download/2.5.0/wait /wait
RUN	chmod +x /wait

CMD	/wait && ./docker-entrypoint.sh	

