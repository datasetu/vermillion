# vim: tabstop=8 expandtab shiftwidth=4 softtabstop=4
import json
import time
import psycopg2
from cryptography import x509
from cryptography.x509.oid import NameOID
from cryptography.hazmat.primitives import hashes, hmac
from cryptography.hazmat.backends import default_backend

def topup_function(request, credentials):
#
        with open(credentials[0], "r") as f:
                cert_file = f.read().strip()

        cert = x509.load_pem_x509_certificate(cert_file, default_backend())

        email       = str(cert.subject.get_attributes_for_oid(NameOID.EMAIL_ADDRESS)[0].value)
        serial      = '%x' % cert.serial_number # convert to hex

        fingerprint = cert.fingerprint(hashes.SHA1()).encode('hex') # add colons in fingerprint
        fingerprint = ':'.join(a+b for a,b in zip(fingerprint[::2], fingerprint[1::2]))

        if 'serial' in request and 'fingerprint' in request:
                serial      = request['serial']
                fingerprint = request['fingerprint']

        amount      = int(request['amount'])
        now         = time.time()
        invoice_id  = 'inv_' + str(int(now)) 

        with open("../passwords/auth.db.password", "r") as f:
                pg_password = f.read().strip()

        conn_string = "host='localhost' dbname='postgres' user='auth' password='" + pg_password + "'" 

        try:
                conn = psycopg2.connect(conn_string)

        except psycopg2.DatabaseError as error:
                return {}

        cursor = conn.cursor()
        query = "INSERT INTO topup_transaction VALUES (%s,%s,%s,%s,to_timestamp(%s),%s,false,'{}'::jsonb);"
        params = (email,serial,fingerprint,amount,now,invoice_id)

        try:
                cursor.execute(query, params)
                conn.commit()

        except psycopg2.DatabaseError as error:
                return {}

        # form invoice, invoice signature
        with open("../rzpay.key.secret", "r") as f:
                key_secret = f.read().strip()

        resp = {'razorpay_invoice_id': invoice_id,'razorpay_invoice_status': 'paid', \
                'razorpay_payment_id':'pay_DaCTRWQeB2X5bI', 'razorpay_invoice_receipt':'TS1988'}

        challenge_string = '|'.join((resp['razorpay_invoice_id'],\
                            resp['razorpay_invoice_receipt'],\
                            resp['razorpay_invoice_status'],\
                            resp['razorpay_payment_id']))

        h = hmac.HMAC(key_secret, hashes.SHA256(), backend=default_backend())
        h.update(challenge_string)
        resp['razorpay_signature'] = h.finalize().encode('hex')

        return resp
#
