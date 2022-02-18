#TODO: setup automated build for this
FROM	node:latest

ADD	https://github.com/ufoscout/docker-compose-wait/releases/download/2.7.3/wait /wait
RUN	chmod +x /wait

RUN	apt -y update						&&  \
	apt -y install libcap2-bin				&&  \
	setcap CAP_NET_BIND_SERVICE=+eip /usr/local/bin/node	&&  \
	useradd -s /sbin/nologin -d /nonexistent _aaa

COPY	./datasetu-auth-server/package.json /auth-cache/
COPY	./node-aperture/package.json /aperture-cache/
COPY	./node-aperture/lib/ /aperture-cache/lib/
COPY	./node-aperture/gen/ /aperture-cache/gen/
COPY	./docker-images/auth-server/docker-entrypoint.sh /

RUN	 npm install /auth-cache				&&  \
	 npm install /aperture-cache

CMD	/wait && ./docker-entrypoint.sh
