FROM openjdk:11.0.1-jre-slim

COPY ./docker-entrypoint.sh /

RUN apt-get update              && \
    apt-get -y upgrade          && \
    apt-get install -y maven    && \
    apt-get install -y tmux

CMD ./docker-entrypoint.sh
