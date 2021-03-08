FROM	maven:3.6.3-openjdk-17-slim

COPY	./docker-entrypoint.sh /

#RUN	mkdir -p /usr/share/man/man1/
#RUN	apt-get update					    &&	\
#	apt-get install -y --no-install-recommends maven    &&	\
#	apt-get clean					    &&  \
#	rm -rf /var/lib/apt/lists/*

ADD	https://github.com/ufoscout/docker-compose-wait/releases/download/2.6.0/wait /wait
RUN	chmod +x /wait

CMD	/wait && ./docker-entrypoint.sh
