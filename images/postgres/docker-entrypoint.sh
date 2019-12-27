#!/bin/ash

#Move to environment variable
pgdata=/var/lib/postgresql/

if [ "$(ls -A $pgdata)" ]; then
    su postgres -c "postgres -D $pgdata"
else
    salt=`cat /dev/urandom | env LC_ALL=C tr -dc a-zA-Z0-9 | head -c 32; echo`
    string=$ADMIN_PWD$salt"admin"
    hash=`echo -n $string | sha256sum | cut -d ' ' -f 1)`
    su postgres -c 'initdb -D '$pgdata''
    mv /pg_hba.conf $pgdata
    mv /postgresql.conf $pgdata
    chown -R postgres $pgdata
    su postgres -c 'pg_ctl -D "'$pgdata'" -o "-c listen_addresses=''" -w start'
    psql -U  postgres < /schema.db
    psql -U postgres -c "alter user postgres with password '$POSTGRES_PWD'"
    psql -U postgres -c "insert into users values('admin','$hash',NULL,'$salt','f', 't')"
    su postgres -c 'pg_ctl -D "'$pgdata'" -m fast -w stop'
    su postgres -c "postgres -D $pgdata"
fi
