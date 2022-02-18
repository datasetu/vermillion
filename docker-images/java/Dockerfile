FROM	maven:3.6.3-openjdk-14-slim

COPY	./docker-entrypoint.sh /

ADD	https://github.com/ufoscout/docker-compose-wait/releases/latest/download/wait /wait
RUN	chmod +x /wait

CMD	/wait && ./docker-entrypoint.sh
