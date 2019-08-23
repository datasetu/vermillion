#!/bin/ash

set -e

mkdir -p /var/lib/rabbitmq/
cookie_file='/usr/lib/rabbitmq/.erlang.cookie'
echo $ERLANG_COOKIE > $cookie_file
chmod 600 "$cookie_file"
chown rabbitmq "$cookie_file"

hostname=$(hostname)

#TODO Use env variable
if [ $hostname == "rabbit" -o $hostname == "rabbit1" ]
then
    rabbitmq-server
else
    rabbitmq-server -detached
    rabbitmqctl stop_app
    rabbitmqctl join_cluster rabbit@rabbit1
    rabbitmqctl stop
    sleep 2s
    rabbitmq-server
fi


