#!/bin/bash
openssl req -new -newkey rsa:2048 -nodes -out csr.pem -keyout private-key.pem -subj "/"
