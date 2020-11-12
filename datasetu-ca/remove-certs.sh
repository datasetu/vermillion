#!/bin/bash

#Delete resource server certificates
rm  resource-server/resource-server.csr	    \
    resource-server/resource-server.key.pem \
    resource-server/resource-server.pem	    \
    resource-server/resource-server-keystore.jks 2>/dev/null

#Remove provider certificates
rm  provider/provider.csr		    \
    provider/provider.pem		    \
    provider/provider.key.pem 2>/dev/null

#Remove consumer certificates
rm  consumer/consumer.csr		    \
    consumer/consumer.pem		    \
    consumer/consumer.key.pem 2>/dev/null

#Remove self-signed SSL certs and copies of rs certs
rm  ../api-server/certs/auth/*		    \
    ../api-server/certs/ssl/*		    \
    ../authenticator/certs/auth/* 2>/dev/null
