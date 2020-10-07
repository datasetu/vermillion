#!/bin/bash

openssl genrsa -out ca.private.key 2048
openssl req -new  -subj "/CN=ca.datasetu.org/emailAddress=ca@datasetu.org" -key ca.private.key -out ca.csr
openssl x509 -req -days 365 -in ca.csr -signkey ca.private.key -out ca.crt
