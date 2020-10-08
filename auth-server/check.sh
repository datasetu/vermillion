#!/bin/sh

jshint --show-non-errors main.js
eslint main.js

jshint --show-non-errors crl.js
eslint crl.js

jshint --show-non-errors bot.js
eslint bot.js
