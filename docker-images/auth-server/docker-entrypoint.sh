#!/bin/bash

#Copy node modules directory from the cache folder
echo "Symlinking node modules to respective directories"
ln -s /auth-cache/node_modules /home/datasetu-auth-server/node_modules
ln -s /aperture-cache/node_modules /home/datasetu-auth-server/node-aperture/node_modules

#Start the app
echo "Starting auth server"
cd /home/datasetu-auth-server && node main.js
