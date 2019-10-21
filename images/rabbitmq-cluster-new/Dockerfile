FROM rabbitmq:3.7.18

COPY rabbitmq-cluster /usr/local/bin/
COPY pre-entrypoint.sh /

ADD	https://github.com/ufoscout/docker-compose-wait/releases/download/2.6.0/wait /wait
RUN	chmod +x /wait

ENTRYPOINT ["/pre-entrypoint.sh"]
CMD /wait && rabbitmq-cluster
