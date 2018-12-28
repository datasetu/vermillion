#!/bin/bash

cmd="curl -ik -X GET https://localhost:8888/subscribe -H 'apikey: $2' -H 'id: $1'"

if [ ! -z "$3" ]
then
cmd=$cmd" -H 'message-type: $3'"
fi

if [ ! -z "$4" ]
then
cmd=$cmd" -H 'num-messages: $4'"
fi

eval $cmd
