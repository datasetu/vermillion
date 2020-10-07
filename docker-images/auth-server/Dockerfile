FROM	node:latest

RUN	apt -y update		    &&  \
	apt -y install libcap2-bin  && \
	setcap CAP_NET_BIND_SERVICE=+eip /usr/local/bin/node

#WORKDIR /usr/src/app
#
#COPY	./src/package.json .
#COPY	./conf/ ../conf/
#COPY	./secret/ ../secret/
#
#RUN	npm install

#CMD	["node", "main.js"]

CMD tail -f /dev/null
