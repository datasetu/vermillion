// Copyright (c) 2014, Joyent, Inc. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

var assert = require('assert-plus');
var errors = require('./errors.js');
var pluck = require('jsprim').pluck;



function parseRegex(literal) {
    var last = literal.lastIndexOf('/');
    var body = literal.substring(1, last);
    var flags = literal.substring(last + 1);
    return (new RegExp(body, flags));
}



function Evaluator(opts) {
    assert.object(opts, 'opts');
    assert.object(opts.types, 'opts.types');
    assert.optionalObject(opts.typeTable, 'opts.typeTable');

    this.types = opts.types;
    this.typeTable = opts.typeTable;
}


Evaluator.prototype.evaluateOne = function evaluateOne(policy, context) {
    var self = this;

    var PAR = ['principal', 'action', 'resource'].every(function (property) {
        var plural = property + 's';

        // undefined property -> implied property (skip checking it)
        if (!policy[plural]) {
            return (true);
        }

        // ALL/Everything/Anything
        if (policy[plural] === 1) {
            return (true);
        }

        // check exact matches
        if (policy[plural].exact[context[property]]) {
            return (true);
        }

        // check regexes
        return (policy[plural].regex.some(function (regex) {
            return (parseRegex(regex).test(context[property]));
        }));
    });

    if (!PAR) {
        return (false);
    }

    if (!policy.conditions || !policy.conditions.length) {
        if (policy.effect)
		return {"expiry" : policy.expiry, "amount" : policy.at["amount"], "currency" : policy.at["currency"]};
    }

    r = self.evaluateCondition(policy.conditions, context.conditions);
    if (r) {
        if (policy.effect)
	{
		const result = {
			"expiry"		: policy.expiry,
			"amount"		: policy.at["amount"],
			"currency"		: policy.at["currency"],
		};

		if (typeof(r) === "string")
			result["manual-authorization"] = r;

		return result;
	}
    }

    return (false);
};


Evaluator.prototype.evaluate = function evaluate(policies, context) {
    var self = this;
    var expiry = false;

    if (!Array.isArray(policies)) {
        policies = [ policies ];
    }


    (policies.some(function (policy) {
	expiry = (self.evaluateOne(policy, context));
	return expiry;
    }));

    return expiry;
};


Evaluator.prototype.evaluateCondition =
    function evaluateCondition(policy, context) {

    var self = this;
    var op = policy[0];
    var lhs = policy[1];
    var rhs = policy[2];
    var i;

    if (op === 'and') {
        if (!self.evaluateCondition(lhs, context)) {
            return (false);
        }
        return (self.evaluateCondition(rhs, context));
    }

    if (op === 'or') {
        if (self.evaluateCondition(lhs, context)) {
            return (true);
        }
        return (self.evaluateCondition(rhs, context));
    }

    if (op === 'not') {
        return (!self.evaluateCondition(lhs, context));
    }

    if (op === 'in') {
        for (i = 0; i < rhs.length; i++) {
            if (self.evaluateCondition(['=', lhs, rhs[i]], context)) {
                return (true);
            }
        }
        return (false);
    }

    if (op === 'match') {

	var lhs_value = context[lhs.name].split(",");

        for (i = 0; i < rhs.length; i++) {
		for (j = 0; j < lhs_value.length; ++j) {

	    		if (rhs[i] === lhs_value[j]) {
	                	return (true);
			}
		}
        }
        return (false);
    }

    if (op === 'authorized-by')
    {
	const split = rhs.split(":");

	const communication	= split[0].toLowerCase();
	const id		= split[1]; 

	switch (communication)
	{
		case 'telegram':
			return rhs;

		default:
        		throw new errors.UnknownTypeError('we do not support "%s"', communication);
	}
    }

    if (op === 'consumer-in-group') {

	var lhs_value = context['groups'].split(",");

        for (i = 0; i < rhs.length; i++) {
		for (j = 0; j < lhs_value.length; ++j) {

	    		if (rhs[i] === lhs_value[j]) {
	                	return (true);
			}
		}
        }
        return (false);
    }


    lhs.type = lhs.type || (self.typeTable && self.typeTable[lhs.name]) || 'string';

    if (!lhs.type) {
        throw new errors.MissingTypeError('no type for "%s"', lhs.name);
    }

    if (!self.types[lhs.type]) {
        throw new errors.UnknownTypeError('unknown type "%s"', lhs.type);
    }

    if (typeof (self.types[lhs.type][op]) !== 'function') {
        throw new errors.UnsupportedOperationError('unsupported '+
            'operation "%s" on type "%s"', op, lhs.type);
    }

    if (typeof (context[lhs.name]) === 'undefined' ||
        context[lhs.name] === null) {

        throw new errors.MissingConditionError(
            'missing condition in context: "%s"', lhs.name);
    }

    return (self.types[lhs.type][op](pluck(context, lhs.name), rhs));
};



module.exports = {
    Evaluator: Evaluator,

    createEvaluator: function createEvaluator(opts) {
        return (new Evaluator(opts));
    }
};
