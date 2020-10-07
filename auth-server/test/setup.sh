openssl req -x509 -nodes -days 365 -subj "/CN=ca.datasetu.org/emailAddress=ca@datasetu.org" -newkey rsa:2048 -keyout ca.key -out ca.datasetu.org.crt

# let consumer certificate be of type ECDSA
openssl ecparam -genkey -name secp384r1 > consumer.key.pem
openssl req -new -key consumer.key.pem -out consumer.csr -sha256 -subj "/CN=individual/emailAddress=barun@iisc.ac.in/id-qt-unotice=class:2"

openssl ecparam -genkey -name secp384r1 > restricted.key.pem
openssl req -new -key restricted.key.pem -out restricted.csr -sha256 -subj '/CN=individual/emailAddress=barun@iisc.ac.in/id-qt-unotice=class:2;can-access:rbccps.org\/*\/rs1\/*'

# let provider certificate be of type RSA 
openssl req -new -newkey rsa:2048 -nodes -out provider.csr -keyout provider.key.pem -subj "/CN=employee/emailAddress=arun.babu@rbccps.org/id-qt-unotice=class:3"

openssl req -new -newkey rsa:2048 -nodes -out alt-provider.csr -keyout alt-provider.key.pem -subj "/CN=employee/emailAddress=abc.123@iisc.ac.in/id-qt-unotice=class:3"

openssl req -new -newkey rsa:2048 -nodes -out delegated.csr -keyout delegated.key.pem -subj "/CN=employee/emailAddress=abc.xyz@rbccps.org/id-qt-unotice=class:3;delegated-by:arun.babu@rbccps.org"

openssl req -new -newkey rsa:2048 -nodes -out untrusted.csr -keyout untrusted.key.pem -subj "/CN=employee/emailAddress=abc.xyz@rbccps.org/id-qt-unotice=class:3;untrusted:true"

openssl req -new -newkey rsa:2048 -nodes -out r-server.csr -keyout r-server.key.pem -subj "/CN=iisc.datasetu.org/id-qt-unotice=class:1/emailAddress=example@example.com"
openssl req -new -newkey rsa:2048 -nodes -out f-server.csr -keyout f-server.key.pem -subj "/CN=google.com/id-qt-unotice=class:1/emailAddress=arun.babu@rbccps.org"

openssl req -new -newkey rsa:2048 -nodes -out e-server.csr -keyout e-server.key.pem -subj "/CN=example.com/id-qt-unotice=class:1/emailAddress=arun.babu@rbccps.org"

openssl x509 -CA ca.datasetu.org.crt -CAkey ca.key -CAcreateserial -in consumer.csr -req -days 365 -sha256 -out consumer.pem 

openssl x509 -CA ca.datasetu.org.crt -CAkey ca.key -CAcreateserial -in restricted.csr -req -days 365 -sha256 -out restricted.pem 

openssl x509 -CA ca.datasetu.org.crt -CAkey ca.key -CAcreateserial -in provider.csr -req -days 365 -sha256 -out provider.pem 
openssl x509 -CA ca.datasetu.org.crt -CAkey ca.key -CAcreateserial -in alt-provider.csr -req -days 365 -sha256 -out alt-provider.pem 

openssl x509 -CA ca.datasetu.org.crt -CAkey ca.key -CAcreateserial -in delegated.csr -req -days 365 -sha256 -out delegated.pem 
openssl x509 -CA ca.datasetu.org.crt -CAkey ca.key -CAcreateserial -in untrusted.csr -req -days 365 -sha256 -out untrusted.pem 

openssl x509 -CA ca.datasetu.org.crt -CAkey ca.key -CAcreateserial -in r-server.csr -req -days 365 -sha256 -out r-server.pem 

openssl x509 -CA ca.datasetu.org.crt -CAkey ca.key -CAcreateserial -in f-server.csr -req -days 365 -sha256 -out f-server.pem 
openssl x509 -CA ca.datasetu.org.crt -CAkey ca.key -CAcreateserial -in e-server.csr -req -days 365 -sha256 -out e-server.pem 

rm *.csr
cp *.pem /home/build
