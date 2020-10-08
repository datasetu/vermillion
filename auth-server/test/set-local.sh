#!/bin/sh

echo "lookup file bind"    		>  /etc/resolv.conf
echo "nameserver 1.1.1.1"		>> /etc/resolv.conf

echo "127.0.0.1	auth.datasetu.org"	>> /etc/hosts
echo "127.0.0.1	iisc.datasetu.org"	>> /etc/hosts
echo "127.0.0.1	localhost"		>> /etc/hosts
echo "::1	localhost"		>> /etc/hosts
