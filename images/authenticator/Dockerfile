FROM	openjdk:11.0.1-jre-slim

COPY	./docker-entrypoint.sh /

RUN	apt-get update	&& \
	apt-get install -y --no-install-recommends maven    &&	\
	apt-get clean	&&  \
	rm -rf /var/lib/apt/lists/*

ADD	https://github.com/ufoscout/docker-compose-wait/releases/download/2.5.0/wait /wait
RUN	chmod +x /wait

CMD	/wait && ./docker-entrypoint.sh
