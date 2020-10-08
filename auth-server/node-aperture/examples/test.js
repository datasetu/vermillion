var aperture = require('../');
var assert = require('assert-plus');

var apertureOpts = {
    types: aperture.types,
    typeTable: {
        dirname: 'string',
        sourceip: 'ip'
    }
};
var parser = aperture.createParser(apertureOpts);
var evaluator = aperture.createEvaluator(apertureOpts);

var text = [
    'Fred can read *.js @ 10 INR if authorized-by(telegram:user-id)',
];

var policies = text.map(function (t) {
    return (parser.parse(t));
});

var context = {
    principal: 'Fred',
    action: 'read',
    resource: 'parser.example.js',
    conditions: {
        dirname: 'examples',
        sourceip: '10.0.0.1'
    }
};

console.log(evaluator.evaluate(policies, context));
