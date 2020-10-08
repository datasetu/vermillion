FROM	node:latest

RUN	apt -y update		    &&  \
	apt -y install libcap2-bin  && \
	setcap CAP_NET_BIND_SERVICE=+eip /usr/local/bin/node

COPY	./auth-server/package.json /cache/
COPY	./docker-images/auth-server/docker-entrypoint.sh /

RUN	cd /cache && npm install

CMD	./docker-entrypoint.sh
