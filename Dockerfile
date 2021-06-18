FROM ubuntu-latest

RUN apt install python3.8 -y

RUN apt-get -y install jq python3-setuptools
RUN python3 -m pip install behave

