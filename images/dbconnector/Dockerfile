FROM openjdk:11.0.1-jre-slim

COPY ./docker-entrypoint.sh /

RUN apt-get update              && \
    apt-get -y upgrade          && \
    apt-get install -y maven    && \
    apt-get install -y tmux

ADD https://github.com/ufoscout/docker-compose-wait/releases/download/2.5.0/wait /wait
RUN chmod +x /wait

CMD /wait && ./docker-entrypoint.sh
