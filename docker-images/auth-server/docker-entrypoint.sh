#!/bin/bash

#Copy node modules directory from the cache folder
cp -r /cache/node_modules /auth-server/node_modules

#Start the app
cd auth-server && node main.js
