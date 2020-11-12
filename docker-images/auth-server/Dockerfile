FROM	node:latest

RUN	apt -y update						&&  \
	apt -y install libcap2-bin				&&  \
	setcap CAP_NET_BIND_SERVICE=+eip /usr/local/bin/node	&&  \
	useradd -s /sbin/nologin -d /nonexistent _aaa

COPY	./datasetu-auth-server/package.json /auth-cache/
COPY	./node-aperture/package.json /aperture-cache/
COPY	./docker-images/auth-server/docker-entrypoint.sh /

RUN	cd /auth-cache && npm install				&&  \
	cd /aperture-cache && npm install

CMD	./docker-entrypoint.sh
