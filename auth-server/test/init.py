# vim: tabstop=8 expandtab shiftwidth=4 softtabstop=4

import os
from auth import Auth

auth_server = "auth.datasetu.org"
home        = os.path.expanduser("~") + "/"

consumer		= Auth(home + "consumer.pem",	 home + "consumer.key.pem",	auth_server)
provider		= Auth(home + "provider.pem",	 home + "provider.key.pem",	auth_server)
alt_provider		= Auth(home + "alt-provider.pem",home + "alt-provider.key.pem",	auth_server)
delegate		= Auth(home + "delegated.pem",	 home + "delegated.key.pem",	auth_server)
untrusted		= Auth(home + "untrusted.pem",	 home + "untrusted.key.pem",	auth_server)
example_dot_com		= Auth(home + "e-server.pem",	 home + "e-server.key.pem",	auth_server)
restricted_consumer	= Auth(home + "restricted.pem",	 home + "restricted.key.pem",	auth_server)
fake_resource_server	= Auth(home + "f-server.pem",	 home + "f-server.key.pem",	auth_server)

if "AUTH_SERVER" in os.environ and os.environ["AUTH_SERVER"] == "localhost":
#
    import urllib3
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
#

resource_server		= Auth(home + "r-server.pem", home + "r-server.key.pem", auth_server)

def expect_failure(b):
	if b:
		os.environ["EXPECT_FAILURE"] = "1"
	else:
		del os.environ["EXPECT_FAILURE"]
