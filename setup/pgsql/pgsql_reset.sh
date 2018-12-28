#!/bin/ash

psql -U postgres -c "delete from acl; delete from follow; delete from users where id not like 'admin'"
