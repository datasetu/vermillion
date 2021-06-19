FROM ubuntu:18.04
RUN apt update
RUN apt upgrade -y
RUN apt install python3.7 -y

RUN apt-get -y install jq python3-setuptools
RUN apt-get -y install python3-setuptools
RUN python3 -m pip install behave

