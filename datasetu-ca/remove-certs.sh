#!/bin/bash

rm resource-server/resource-server.csr resource-server/resource-server.key.pem resource-server/resource-server.pem
rm provider/provider.csr provider/provider.pem provider/provider.key.pem
rm consumer/consumer.csr consumer/consumer.pem consumer/consumer.key.pem
rm ../api-server/certs/auth/*
rm ../api-server/certs/ssl/*
