#!/bin/bash

#Generate self-signed SSL certificate for auth server
openssl req -newkey rsa:2048 -nodes -keyout https-key.pem -x509 -days 365 -out https-certificate.pem -subj "/CN=auth.local"

#Generate self-signed CA certificate
openssl req -x509 -nodes -days 365 -subj "/CN=ca.datasetu.org/emailAddress=ca@datasetu.org" -newkey rsa:2048 -keyout ca.key -out ca.datasetu.org.crt

#Generate consumer's CSR
openssl req -new -newkey rsa:2048 -nodes -out consumer/consumer.csr -keyout consumer/consumer.key.pem -subj "/CN=individual/emailAddress=consumer@iisc.ac.in/id-qt-unotice=class:2"

#Generate provider's CSR
openssl req -new -newkey rsa:2048 -nodes -out provider/provider.csr -keyout provider/provider.key.pem -subj "/CN=employee/emailAddress=provider@rbccps.org/id-qt-unotice=class:3"

#Generate resource server's CSR
openssl req -new -newkey rsa:2048 -nodes -out resource-server/resource-server.csr -keyout resource-server/resource-server.key.pem -subj "/CN=example.com/id-qt-unotice=class:1/emailAddress=provider@rbccps.org"

#Sign consumer's CSR
openssl x509 -CA ca.datasetu.org.crt -CAkey ca.key -CAcreateserial -in consumer/consumer.csr -req -days 365 -sha256 -out consumer/consumer.pem

#Sign provider's CSR
openssl x509 -CA ca.datasetu.org.crt -CAkey ca.key -CAcreateserial -in provider/provider.csr -req -days 365 -sha256 -out provider/provider.pem 

#Sign resource server's CSR
openssl x509 -CA ca.datasetu.org.crt -CAkey ca.key -CAcreateserial -in resource-server/resource-server.csr -req -days 365 -sha256 -out resource-server/resource-server.pem 

#Prepare jks file from cert and key
cat resource-server/resource-server.pem resource-server/resource-server.key.pem > combined.pem

#Convert signed resource server certificate into jks
#TODO: Have dynamic passwords
openssl pkcs12 -export 		\
	-passin pass:password 	\
	-passout pass:password	\
	-in combined.pem 	\
	-out cert.p12

keytool -importkeystore						    \
	-srckeystore cert.p12			    		    \
	-srcstoretype pkcs12			    		    \
	-destkeystore resource-server/resource-server-keystore.jks  \
	-storepass password					    \
	-keypass password			    		    \
	-noprompt				    		    \
	-srcstorepass password

#Generate a self signed SSL certificate
keytool -genkey -noprompt \
 -alias selfsigned \
 -keyalg RSA \
 -dname "CN=localhost, OU=Datasetu, O=ARTPark, L=Bangalore, S=Karnataka, C=IN" \
 -keystore ssl-keystore.jks \
 -storepass password \
 -keypass password \
 -validity 30 \
 -keysize 2048

#Move certificates to appropriate directories
mv https-key.pem ../datasetu-auth-server/
mv https-certificate.pem ../datasetu-auth-server/

mv ca.key ../datasetu-auth-server/
mv ca.datasetu.org.crt ../datasetu-auth-server/

mkdir -p ../api-server/certs/auth && cp resource-server/resource-server-keystore.jks ../api-server/certs/auth/
mkdir -p ../authenticator/certs/auth && cp resource-server/resource-server-keystore.jks ../authenticator/certs/auth/

cp resource-server/resource-server.pem ../api-server/certs/auth/
cp resource-server/resource-server.key.pem ../api-server/certs/auth/

mkdir ../api-server/certs/ssl/ && mv ssl-keystore.jks ../api-server/certs/ssl/

#Remove unwanted files
rm -f ca.srl cert.p12 combined.pem
