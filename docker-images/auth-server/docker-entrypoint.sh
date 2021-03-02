#!/bin/bash

echo "Symlinking node modules to respective directories"
rm -rf /home/datasetu-auth-server/node_modules && ln -sf /auth-cache/node_modules /home/datasetu-auth-server/
rm -rf /home/datasetu-auth-server/node_aperture/node_modules && ln -sf /aperture-cache/node_modules /home/datasetu-auth-server/node-aperture/

#Start the app
echo "Starting auth server"
cd /home/datasetu-auth-server && node main.js
