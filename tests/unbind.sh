#!/bin/bash

cmd="curl -ik -X GET https://localhost:8888/unbind -H 'apikey: $2' -H 'id: $1' -H 'to: $3' -H 'topic: $4'"

if [ ! -z "$5" ]
then
cmd=$cmd" -H 'from: $5'"
fi

if [ ! -z "$6" ]
then
cmd=$cmd" -H 'message-type: $6'"
fi

eval $cmd
