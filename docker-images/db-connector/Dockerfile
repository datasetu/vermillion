from	alpine:latest

add	https://github.com/ufoscout/docker-compose-wait/releases/download/2.7.3/wait /wait
run	chmod +x /wait

copy	requirements.txt /

run	apk update			    &&	\
	apk add python3 py3-pip		    &&	\
	pip3 install -r requirements.txt

cmd	/wait && python3 /db-connector/connector.py
