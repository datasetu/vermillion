#!/bin/ash

#Move to environment variable
pgdata=/var/lib/postgresql/

if [ "$(ls -A $pgdata)" ]; then
    su postgres -c "postgres -D $pgdata"
else
    su postgres -c 'initdb -D '$pgdata''
    mv /pg_hba.conf $pgdata
    mv /postgresql.conf $pgdata
    chown -R postgres $pgdata
    su postgres -c 'pg_ctl -D "'$pgdata'" -o "-c listen_addresses=''" -w start'
    psql -U  postgres < /schema.db
    su postgres -c 'pg_ctl -D "'$pgdata'" -m fast -w stop'
    su postgres -c "postgres -D $pgdata"
fi
