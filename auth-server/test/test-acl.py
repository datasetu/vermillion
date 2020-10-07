# vim: tabstop=8 expandtab shiftwidth=4 softtabstop=4

from init import provider

from init import expect_failure 

rules = [
	'x@x.com can access rs1.com/x/y/z/t/a/b/c for 2 days',
	'x@x.com can access rs1.com/_x/y/z/t/a/b/c for 2 days if country = "IN" AND api = "/latest"',
	'x@x.com can access rs1.com/x-t/y/z/t/a/b/c for 2 days if country = "IN" OR  api = "/latest"',
	'consumer1@domain.com and consumer2@domain.com can access rs1.com/x for 5 hours @ 5 INR',
	'a,b@b.com, and c can access x/y/z.a.b.c/t for 2 seconds @ 10.5 INR; all can access anything; x can access y',
        '* can access local_server/*/test if ip = "138.212.77.14" OR ip = "::ffff:ada0:d182"',
        '* can access test-server/test-resource/rs1 if body.operation = "select" AND body.on = "everything"',
        '* can access test-server/test-resource/rs2 if api = "/latest" AND method = "GET"',
        'someone@gmail.com can access test/test/* if cert.class = 2 AND cert.issuer.cn = "ca.datasetu.org"',
        '*@iisc.ac.in can access data/server1/* if cert.class = 3 AND ' +
        'cert.o = "Indian Institute of Science \(IISc\)" AND cert.issuer.cn = "DataSetu-sub-CA at iisc.ac.in"',
        '*@rbccps.org can access confidential/data/* if cert.title = "Member of Technical Staff" AND ' +
        'cert.ou = "Robert Bosch Centre for Cyber-Physical Systems \(RBCCPS\)"',
        'person@* can access local/test/1 if tokens_per_day = 300 AND cert.st = "Karnataka"'
]

for rule in rules:
	r = provider.set_policy(rule)
	assert r['success'] is True


policy = "x can access x"
r = provider.set_policy(policy)
assert r['success'] is True

new_policy = "y can access y"
r = provider.set_policy(policy)
assert r['success'] is True

r = provider.revert_policy()
assert r['success'] is True

r = provider.get_policy()
assert r['success'] is True
reverted_policy = r['response']['policy']

assert len(reverted_policy) == 1
assert reverted_policy[0] == policy

# reverting twice should not work

expect_failure(True)
r = provider.revert_policy()
expect_failure(False)

assert r['success'] is False

# RegEx should not work supported

r = provider.get_policy()
assert r['success'] is True
policy = r['response']['policy']

expect_failure(True)

regex_policy = "x can access y if z like /someregex*/i"

expect_failure(True)
r = provider.set_policy(regex_policy)
expect_failure(False)

assert r['success'] is False

regex_policy = "x can access /fred(dy)?/i::regex"

expect_failure(True)
r = provider.set_policy(regex_policy)
expect_failure(False)

assert r['success'] is False

r = provider.get_policy()
assert r['success'] is True
assert policy == r['response']['policy']
