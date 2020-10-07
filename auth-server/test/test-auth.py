# vim: tabstop=8 expandtab shiftwidth=4 softtabstop=4

import os

from init import consumer
from init import provider
from init import resource_server

from init import expect_failure 

RS = "iisc.datasetu.org"

expect_failure(True)

r = consumer.get_policy()
assert r['success']	is False
assert r['status_code'] == 403

policy = "x can access *" # dummy policy
r = consumer.set_policy(policy)
assert r['success']	is False
assert r['status_code'] == 403

r = consumer.append_policy(policy)
assert r['success']	is False
assert r['status_code'] == 403

r = resource_server.get_policy()
assert r['success']	is False
assert r['status_code'] == 403

r = resource_server.set_policy(policy)
assert r['success']	is False
assert r['status_code'] == 403

r = resource_server.append_policy(policy)
assert r['success']	is False
assert r['status_code'] == 403

tokens		= ["dummy"]
token_hashes	= ["dummy"]

r = resource_server.revoke_tokens(tokens)
assert r['success']	is False
assert r['status_code'] == 403

r = resource_server.revoke_token_hashes(tokens)
assert r['success']	is False
assert r['status_code'] == 403

r = resource_server.revoke_all("invalid","invalid")
assert r['success']	is False
assert r['status_code'] == 403

r = resource_server.audit_tokens(10)
assert r['success']	is False
assert r['status_code'] == 403

r = resource_server.add_consumer_to_group("arun","confidential",20)
assert r['success']	is False
assert r['status_code'] == 403

r = resource_server.delete_consumer_from_group("arun","confidential")
assert r['success']	is False
assert r['status_code'] == 403

r = resource_server.list_group("confidential")
assert r['success']	is False
assert r['status_code'] == 403

token 		= {}
server_token	= {}

r = provider.introspect_token (token,server_token)
assert r['success']	is False
assert r['status_code'] == 403

# consumers

r = consumer.introspect_token (token,server_token)
assert r['success']	is False
assert r['status_code'] == 403

r = consumer.introspect_token (token,server_token)
assert r['success']	is False
assert r['status_code'] == 403

r = consumer.add_consumer_to_group("arun","confidential",20)
assert r['success']	is False
assert r['status_code'] == 403

r = consumer.delete_consumer_from_group("arun","confidential")
assert r['success']	is False
assert r['status_code'] == 403

r = consumer.list_group("confidential")
assert r['success']	is False
assert r['status_code'] == 403

expect_failure(False)
