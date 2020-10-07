#!/bin/sh

export PGPASSWORD=`cat /home/postgresql/admin.db.password`

/usr/local/bin/pg_dumpall -U postgres > /root/backups/postgresql.sql

/usr/local/bin/tarsnap -cf "$(uname -n)-$(date +%Y-%m-%d_%H-%M-%S)" /root/backups

if [ "$?" != "0" ]
then
	# Tarsnap failed ... store a local zipped version of backup
	# we will re-attempt sending it to tarsnap tomorrow 

	date=`date | tr ' ' '-'`
	tar -cvzf "/root/backups/postgresql.$date.tgz" /root/backups/postgresql.sql
fi

rm -rf /root/backups/postgresql.sql
