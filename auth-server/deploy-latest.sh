ud#!/bin/sh

CRL_JS="https://raw.githubusercontent.com/datasetu/datasetu-auth-server/master/crl.js"
MAIN_JS="https://raw.githubusercontent.com/datasetu/datasetu-auth-server/master/main.js"

if ! ftp -o - $CRL_JS > crl.js
then
	echo "Failed to deploy crl.js!"
	exit
fi

if ! ftp -o - $MAIN_JS > main.js
then
	echo "Failed to deploy main.js!"
	exit
fi

echo "Success"

tmux kill-session -t crl
tmux kill-session -t node

./run.crl.tmux
./run.tmux
