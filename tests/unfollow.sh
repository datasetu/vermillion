#!/bin/bash

cmd="curl -ik -XGET https://localhost:8888/unfollow -H 'apikey: $2' -H 'id: $1' -H 'permission: $5' -H 'to: $3' -H 'topic: $4'"

if [ ! -z "$6" ]
then
cmd=$cmd" -H 'from: $6'"
fi

eval $cmd 
