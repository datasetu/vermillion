# vim: tabstop=8 expandtab shiftwidth=4 softtabstop=4

from init import consumer
from init import provider
from init import expect_failure 
from init import resource_server

r                       = provider.delete_consumer_from_group("*","confidential")
assert r["success"]     is True

r       		= provider.list_group("confidential")
assert r["success"]     is True
assert 0		== len(r["response"])

provider.add_consumer_to_group("barun@iisc.ac.in","confidential",100)
provider.add_consumer_to_group("xyz@iisc.ac.in","confidential",100)

r                       = provider.list_group("confidential")
assert r["success"]     is True

m1_found = False
m2_found = False

members = r["response"]

for m in members:
#
	if m['consumer'] == 'barun@iisc.ac.in':
		m1_found = True

	elif m['consumer'] == 'xyz@iisc.ac.in':
		m2_found = True
#

assert m1_found is True and m2_found is True

r = provider.delete_consumer_from_group("barun@iisc.ac.in","confidential")
assert r["success"] is True

r                       = provider.list_group("confidential")
assert r["success"]     is True
assert 1		== len(r["response"])
assert "xyz@iisc.ac.in"	== r["response"][0]['consumer']

r                       = provider.delete_consumer_from_group("xyz@iisc.ac.in","confidential")
assert r["success"]     is True

r		        = provider.list_group("confidential")
assert r["success"]     is True
assert 0		== len(r["response"])

provider.set_policy('all can access iisc.datasetu.org/resource-xyz* if consumer-in-group(xyz,confidential)')

body = {
	"id"	: "rbccps.org/9cf2c2382cf661fc20a4776345a3be7a143a109c/iisc.datasetu.org/resource-xyz-yzz",
}

provider.add_consumer_to_group("barun@iisc.ac.in","confidential",100)

r       		= provider.list_group("confidential")
assert 1		== len(r["response"])

r       		= consumer.get_token(body)
assert r["success"]	is True
assert 60*60		== r["response"]["expires-in"]

assert provider.delete_consumer_from_group("barun@iisc.ac.in","confidential")["success"]	is True

expect_failure(True)
r       		= consumer.get_token(body)
expect_failure(False)

assert r["success"]     is False

