#!/usr/bin/env python
'''
/*
 * Copyright (c) 2019
 * Arun Babu {arun <dot> hbni <at> gmail}
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
'''

import re
import os
import pwd
import sys
import ssl
import time
import stat
import imaplib
import smtplib
import getpass
import requests
import tempfile
import datetime
import subprocess
import argparse
import traceback

from cryptography import x509
from cryptography.x509.oid import NameOID

from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.backends import default_backend

from cryptography.hazmat.primitives.serialization import load_pem_private_key

from conf.common import *
from conf.ca import *

if re.search(r'\s',CA_NAME):
	print("CA name cannot contain spaces")
	sys.exit(-1)

# get the CA's certificate
f = open("certs/ca.crt", "r")
ca_cert_pem = f.read().strip()
f.close()

f = open("certs/ca.private.key")
ca_private_key_pem = f.read().strip()
f.close()

print("")

ca_cert_password = None
if ca_private_key_pem.startswith("-----BEGIN ENCRYPTED "):
	ca_cert_password = getpass.getpass(prompt='---> Enter password for the CA\'s private key : ', stream=None)

ca_cert 	= x509.load_pem_x509_certificate(ca_cert_pem, default_backend())
ca_private_key 	= load_pem_private_key(ca_private_key_pem,password=ca_cert_password,backend=default_backend())

# TODO: This should go into a DB instead
cert_issued_time = {}

invalid_email_id_chars = [
	"/",
	"=",
	",",
	";",
	"+",
	"#",
	":",
	"\\",
]

is_resource_server	= False


parser = argparse.ArgumentParser()
parser.add_argument('-e', '--email', action='store', type=str, required=True)
parser.add_argument('-f', '--csr-file', action='store', type=str, required=True)
parser.add_argument('-d', '--domain', action='store', type=str)
parser.add_argument('-c', '--cert-class', action='store', type=int, choices=list(range(1,5)))

args = parser.parse_args()

if args.domain:
    is_resource_server = True
    resource_server_name = args.domain

    if not bool(re.match('^[-\.a-zA-Z0-9]+$', resource_server_name)):
        print("*** invalid resource server name from :"+from_email)
        sys.exit(1)
    
    if len(resource_server_name) > 256:
        print("*** resource server name too long by :"+from_email)
        sys.exit(1)

from_email = args.email

csr_data = open(args.csr_file, 'rb').read()

#if not csr_data.startswith("-----BEGIN CERTIFICATE REQUEST-----"):
#    print("*** Email sent by",from_email,"contains invalid CSR !")
#    sys.exit(1)
#
#if not csr_data.endswith("-----END CERTIFICATE REQUEST-----"):
#    print("*** Email sent by",from_email,"contains invalid CSR !")
#    sys.exit(1)

try:
    csr = x509.load_pem_x509_csr(csr_data, default_backend())
except Exception as e:
    print("*** Email sent by",from_email,"does not have a valid CSR !. Exception: ",e)
    traceback.print_exc()
    sys.exit(1)

if not csr.is_signature_valid:
    print("*** Email sent by",from_email,"does not have a valid signature !")
    sys.exit(1)

print("=== Request for certificate: ",from_email)

public_key = csr.public_key().public_bytes(PEM,PublicFormat.SubjectPublicKeyInfo).strip()

if not public_key.startswith("-----BEGIN "):
    print("*** Public key BEGINS with :"+public_key)
    sys.exit(1)

if not public_key.endswith(" PUBLIC KEY-----"):
    print("*** Public key ENDS with "+public_key+":")
    sys.exit(1)

now = datetime.datetime.now()
now = now - datetime.timedelta(days=1)

cb = x509.CertificateBuilder()				\
	.issuer_name(ca_cert.subject)			\
	.public_key(csr.public_key())			\
	.serial_number(x509.random_serial_number())	\
	.not_valid_before(now)				\

domain = from_email.split("@")[1]

if is_resource_server:
    cn = resource_server_name
    cl = "class:1"
    cert_class = 1

    cb = cb.subject_name(										\
			    x509.Name([										\
			    x509.NameAttribute(NameOID.COMMON_NAME, 	unicode(cn,'utf-8')), 		\
			    x509.NameAttribute(NameOID.EMAIL_ADDRESS, 	unicode(from_email,'utf-8')), 	\
			    x509.NameAttribute(x509.CertificatePoliciesOID. CPS_USER_NOTICE, unicode(cl, 'utf-8'))		\
			    ])											\
			)

    valid_till = now + datetime.timedelta(days=365)

else:
    cl = "class:" + str(args.cert_class)
    cert_class = args.cert_class

    cn = "Individual at " + domain
    cb = cb.subject_name (x509.Name([
					x509.NameAttribute(NameOID.COMMON_NAME, 	unicode(cn,'utf-8')), 		\
					x509.NameAttribute(NameOID.EMAIL_ADDRESS, 	unicode(from_email, 'utf-8')), 	\
					x509.NameAttribute(x509.CertificatePoliciesOID.CPS_USER_NOTICE, unicode(cl, 'utf-8'))		\
				    ])
			)

    valid_till = now + datetime.timedelta(days=365)

cb = cb.not_valid_after(valid_till)
cert = cb.sign(ca_private_key, hashes.SHA256(), default_backend())
certificate = cert.public_bytes(PEM).strip()
f = open('certificate.pem', 'w')
f.write(certificate)
f.close
