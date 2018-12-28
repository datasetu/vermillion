#!/bin/bash


cmd="curl -ik -X GET https://localhost:8888/follow -H 'apikey: $2' -H 'id: $1' -H 'permission: $4' -H 'to: $3' -H 'topic: test' -H 'validity: 24'"

if [ ! -z "$5" ]
then
cmd=$cmd" -H 'from: $5'"
fi

eval $cmd
