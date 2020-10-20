#!/bin/bash

openssl req -newkey rsa:2048 -nodes -keyout https-key.pem -x509 -days 365 -out https-certificate.pem -subj "/CN=auth.local"
