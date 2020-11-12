#!/bin/bash

#Copy node modules directory from the cache folder
cp -r /auth-cache/node_modules /home/datasetu-auth-server/node_modules
cp -r /aperture-cache/node_modules /home/datasetu-auth-server/node-aperture/node_modules

#Start the app
cd /home/datasetu-auth-server && node main.js
