FROM rabbitmq:3.7.18

COPY rabbitmq-cluster /usr/local/bin/
COPY pre-entrypoint.sh /

ENTRYPOINT ["/pre-entrypoint.sh"]
CMD ["rabbitmq-cluster"]
