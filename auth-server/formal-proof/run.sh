#!/bin/sh

cat header > input

grep prover9: ../main.js | cut -f2- -d':' >> input

echo end_of_list. >> input
echo  >> input
cat to-prove >> input
prover9 -f input
