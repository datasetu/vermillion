#!/bin/ash

rm -r /tmp/tmux-* > /dev/null 2>&1
su postgres -c "postgres -D /var/lib/postgresql > /var/lib/postgresql/logfile 2>&1 &"

until su postgres -c 'pg_isready' >/dev/null 2>&1
do
sleep 0.1
done
