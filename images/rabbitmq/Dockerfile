FROM alpine:latest

COPY ./docker-entrypoint.sh /

RUN apk update									    &&	\
    apk upgrade									    &&	\
    echo "https://nl.alpinelinux.org/alpine/edge/testing/" >> /etc/apk/repositories &&	\
    apk update									    &&	\
    apk add rabbitmq-server curl tmux						    &&	\
    chmod -R 777 /usr/lib/rabbitmq						    &&	\
    chmod -R 777 /etc								    &&	\
    apk add git build-base openssl libbsd-dev bsd-compat-headers postgresql-dev	    &&	\
    apk add perl elixir python libxslt xmlto zip rsync				    &&	\
    git clone https://github.com/arun-babu/kore					    &&	\
    cd kore									    &&	\
    make NOTLS=1 PGSQL=1							    &&	\
    make install								    &&	\
    cd ..									    &&	\
    rm -rf kore									    &&	\
    apk del --purge git curl							    &&	\
    rabbitmq-plugins --offline enable rabbitmq_management			    &&	\
    rabbitmq-plugins --offline enable rabbitmq_auth_backend_http		    &&	\ 
    rabbitmq-plugins --offline enable rabbitmq_auth_backend_cache

CMD ./docker-entrypoint.sh

