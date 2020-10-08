import os
import requests

home = os.path.expanduser("~") + "/"

verify = True

if "AUTH_SERVER" in os.environ and os.environ["AUTH_SERVER"] == "localhost":
#
	import urllib3
	urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
	verify = False
#

response = requests.get (
	url	= "https://auth.datasetu.org/marketplace/topup.html",
	verify	= verify,
	cert	= (home + "provider.pem", home + "provider.key.pem"),
)

assert response.headers["Content-Encoding"] == "gzip"
