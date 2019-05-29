#!/bin/bash

docker container rm -f $(docker container ls -a -q) && docker volume rm $(docker volume ls -q)
