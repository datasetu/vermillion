#!/bin/sh

mkdir /root/backups

cp backup.sh /root/
echo /root/backup.sh >> /etc/daily.local
chmod +x /root/backup.sh

mkdir /root/tarsnap-install
cd /root/tarsnap-install

ftp https://www.tarsnap.com/download/tarsnap-autoconf-1.0.39.tgz

tar -xzf tarsnap-autoconf-1.0.39.tgz
cd tarsnap-autoconf-1.0.39/

./configure
make all
make install

cp /usr/local/etc/tarsnap.conf.sample /usr/local/etc/tarsnap.conf

# TODO: please change the --user and --machine accordingly
tarsnap-keygen --keyfile /root/tarsnap.key --user auth@datasetu.org --machine auth.datasetu.org 
