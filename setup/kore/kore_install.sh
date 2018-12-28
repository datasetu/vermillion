#!/bin/ash
cd kore-publisher 

if [ -e "random.data" ]
then
    rm random.data
fi
head -c1024 < /dev/urandom > random.data 
chmod 400 random.data

kodev build > /dev/null 2> /dev/null 
tmux new-session -d -s kore 'cd /kore-publisher && kodev run'
