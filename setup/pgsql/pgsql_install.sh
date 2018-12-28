#!/bin/ash

set -e 

salt="$(tr -dc 'a-zA-Z0-9' < /dev/urandom | fold -w ${1:-32} | head -n 1)"

string=$ADMIN_PWD$salt"admin"
hash=$(echo -n $string | sha256sum | cut -d ' ' -f 1)

su postgres -c "postgres -D /var/lib/postgresql > /var/lib/postgresql/logfile 2>&1 &"

until su postgres -c 'pg_isready' >/dev/null 2>&1
do
sleep 0.1
done

echo $POSTGRES_PWD

psql -U postgres -c "alter user postgres with password '$POSTGRES_PWD'" > /dev/null 2>&1 
psql -U postgres -c "insert into users values('admin','$hash',NULL,'$salt','f', 't')" > /dev/null 2>&1
