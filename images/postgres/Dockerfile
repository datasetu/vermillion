from	alpine:3.10.2

copy	schema.db /
copy	pg_hba.conf /
copy	docker-entrypoint.sh /
copy	postgresql.conf /

run	apk update						&&  \
	apk upgrade						&&  \
	chmod +x /docker-entrypoint.sh				&&  \
	apk add postgresql postgresql-contrib postgresql-client	&&  \
	mkdir /run/postgresql					&&  \
	chown -R postgres /var/lib/postgresql/			&&  \
        chown -R postgres /run/postgresql/  

cmd	/docker-entrypoint.sh
