// Copyright (c) 2014, Joyent, Inc. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

%lex

// define regular expressions to be referenced in the next section
esc          "\\"

// https://github.com/cjihrig/jsparser
RegularExpressionNonTerminator [^\n\r]
RegularExpressionBackslashSequence \\{RegularExpressionNonTerminator}
RegularExpressionClassChar [^\n\r\]\\]|{RegularExpressionBackslashSequence}
RegularExpressionClass \[{RegularExpressionClassChar}*\]
RegularExpressionFlags [a-z]*
RegularExpressionFirstChar ([^\n\r\*\\\/\[])|{RegularExpressionBackslashSequence}|{RegularExpressionClass}
RegularExpressionChar ([^\n\r\\\/\[])|{RegularExpressionBackslashSequence}|{RegularExpressionClass}
RegularExpressionBody {RegularExpressionFirstChar}{RegularExpressionChar}*
RegularExpressionLiteral \/{RegularExpressionBody}\/{RegularExpressionFlags}

%options case-insensitive

%%

\s+             /* skip whritespace */
<<EOF>>         return 'EOF';

"AND"           return 'AND';
"OR"            return 'OR';
"NOT"           return 'NOT';

"CAN"           return 'CAN';
"TO"            return 'TO';

"IF"            return 'IF';
"WHEN"          return 'WHEN';
"WHERE"         return 'WHERE';

"ALL"           return 'ALL';
"EVERYTHING"    return 'EVERYTHING';
"ANYTHING"      return 'ANYTHING';

"IN"            return 'IN';
"MATCH"         return 'MATCH';
"FOR"         	return 'FOR';
"@"         	return 'AT';

"CONSUMER-IN-GROUP"         return 'CONSUMER-IN-GROUP';
"AUTHORIZED-BY"             return 'AUTHORIZED-BY';

"::"            return '::';
","             return ',';
"("             return '(';
")"             return ')';


// /RegexBody/flags::regex
{RegularExpressionLiteral}"::regex"p?
    {
        yytext = yytext.substr(0, yytext.lastIndexOf('::'));
        return "REGEX_LITERAL";
    }


// string enclosed in quotes
\"(?:{esc}["bfnrt/{esc}]|{esc}"u"[a-fA-F0-9]{4}|[^"{esc}])*\"
    {
        yytext = yytext.substr(1,yyleng-2);
        return 'STRING_LITERAL';
    }


// any string without parens or commas or double colon ('::')
([^\s,():](\:(?!\:))?)+
    {
        if (yytext === '*') {
            return '*';
        } else if (yytext.match(/[*]/)) {
            return 'FUZZY_STRING';
        } else {
            return 'STRING';
        }
    }

/lex


%start Rule

%%

Rule
    : List Effect List List For At Conditions EOF
        {
            return {
                principals: $1,
                effect: $2,
                actions: $3,
                resources: $4,
		expiry: $5,
                at: $6,
                conditions: $7
            };
        }

    | List Effect List For At Conditions EOF // implied resources
        {
            return {
                principals: $1,
                effect: $2,
                actions: $3,
		for: $4,
		at: $5,
                conditions: $6
            };
        }

    | Effect List List For At Conditions EOF // implied principals
        {
            return {
                effect: $1,
                actions: $2,
                resources: $3,
		for: $4,
		at: $5,
                conditions: $6
            };
        }

    | Effect List For At Conditions EOF // implied principals and resources
        {
            return {
                effect: $1,
                actions: $2,
		for: $3,
		at: $4,
                conditions: $5
            };
        }
    ;


// English lists:
// foo
// foo and bar
// foo, bar, and baz <-- serial or "Oxford" comma
// foo, bar and baz <-- no serial comma
List
    : Identifier
        {
            $$ = {
                exact: {},
                regex: []
            };

            if ($1 instanceof RegExp) {
                $$.regex.push($1.toString());
            } else {
                $$.exact[$1] = true;
            }
        }
    | Identifier AND Identifier
        {
            $$ = {
                exact: {},
                regex: []
            };

            if ($1 instanceof RegExp) {
                $$.regex.push($1.toString());
            } else {
                $$.exact[$1] = true;
            }

            if ($3 instanceof RegExp) {
                $$.regex.push($3.toString());
            } else {
                $$.exact[$3] = true;
            }
        }
    | LongList
    | All
        {
            $$ = 1;
        }
    ;

LongList
    : Identifier ',' Identifier ',' AND Identifier // serial comma
        {
            $$ = {
                exact: {},
                regex: []
            };

            if ($1 instanceof RegExp) {
                $$.regex.push($1.toString());
            } else {
                $$.exact[$1] = true;
            }

            if ($3 instanceof RegExp) {
                $$.regex.push($3.toString());
            } else {
                $$.exact[$3] = true;
            }

            if ($6 instanceof RegExp) {
                $$.regex.push($6.toString());
            } else {
                $$.exact[$6] = true;
            }
        }
    | Identifier ',' Identifier AND Identifier // no serial comma
        {
            $$ = {
                exact: {},
                regex: []
            };

            if ($1 instanceof RegExp) {
                $$.regex.push($1.toString());
            } else {
                $$.exact[$1] = true;
            }

            if ($3 instanceof RegExp) {
                $$.regex.push($3.toString());
            } else {
                $$.exact[$3] = true;
            }

            if ($5 instanceof RegExp) {
                $$.regex.push($5.toString());
            } else {
                $$.exact[$5] = true;
            }
        }
    | Identifier ',' LongList
        {
            if ($1 instanceof RegExp) {
                $3.regex.push($1.toString());
            } else {
                $3.exact[$1] = true;
            }
            $$ = $3;
        }
    ;


Effect
    : CAN
        {
            $$ = true;
        }
    | CAN NOT
        {
            $$ = false;
        }
    ;


Conditions
    : If OrCondition
        {
            $$ = $2;
        }
    | // conditions can be empty
        {
            $$ = [];
        }
    ;

If
    : IF
    | WHEN
    | WHERE
    ;

OrCondition
    : OrCondition OR AndCondition
        {
            $$ = ['or', $1, $3];
        }
    | AndCondition
    ;

AndCondition
    : AndCondition AND NotCondition
        {
            $$ = ['and', $1, $3];
        }
    | NotCondition
    ;

NotCondition
    : NOT NotCondition
        {
            $$ = ['not', $2];
        }
    | Condition
    ;

For
    : FOR String String 
        {
            var op = $1.toLowerCase();
            var time = parseInt($2,10); 
            var units = $3.toLowerCase();
            
            if (time < 0)
		CHECK_YOUR_RULE ("time must be > 0");	

            switch (units)
            {
		case 'second':
		case 'seconds':
			time = time;
			break;

		case 'minute':
		case 'minutes':
			time = time*60;
			break;

		case 'hour':
		case 'hours':
			time = time*60*60;
			break;

		case 'day':
		case 'days':
			time = time*60*60*24;
			break;

		case 'week':
		case 'weeks':
			time = time*60*60*24*7;
			break;

		case 'month':
		case 'months':
			time = time*60*60*24*30;
			break;

		case 'year':
		case 'years':
			time = time*60*60*24*365;
			break;

		default:
			PLEASE_CHECK_YOUR_RULE ("Unit for time must be: 'days', 'hours', 'minutes', etc.");
            }

	    if (time > 31536000)
		PLEASE_CHECK_YOUR_RULE("Maximum expiry is 1 year");

            $$ = time; 
        }
     |	// empty
	{
		$$ = 3600; // 1 hour 
	}
     ;

At
    : AT String String 
        {
            var amount		= $2; 
            var currency	= $3.toUpperCase();
            
            if (isNaN(amount))
		CHECK_YOUR_RULE ("amount must be a number");	

	    amount = parseFloat(amount);

            if (isNaN(amount) || amount < 0 || amount > 10000)
		CHECK_YOUR_RULE ("amount must be > 0 and < 10000");	

            if (currency !== "INR")
		CHECK_YOUR_RULE ("currency must be INR");	

            $$ = {"amount" : amount, "currency" : currency};
        }
     |	// empty
	{
		$$ = {"amount" : 0.00, "currency" : "INR"}; // 0 INR 
	}
     ;

Condition
    : Lhs String String
        {
            var lhs = $1;
            var op = $2.toLowerCase();
            var rhs = $3;
            yy.validate(op, lhs.name, rhs, lhs.type);
            $$ = [ op, lhs, rhs ];
        }
    | Lhs IN '(' CommaSeparatedList ')'
        {
            var lhs = $1;
            var op = $2.toLowerCase();
            var rhs = $4;
            rhs.forEach(function (i) {
                yy.validate('=', lhs.name, i, lhs.type);
            });
            $$ = [ op, lhs, rhs ];
        }
    | Lhs MATCH '(' CommaSeparatedList ')'
        {
            var lhs = $1;
            var op = $2.toLowerCase();
            var rhs = $4;
            rhs.forEach(function (i) {
                yy.validate('=', lhs.name, i, lhs.type);
            });
            $$ = [ op, lhs, rhs ];
        }
    | CONSUMER-IN-GROUP '(' CommaSeparatedList ')'
        {
            var lhs = '';
            var op = $1.toLowerCase();
            var rhs = $3;
            rhs.forEach(function (i) {
                yy.validate('=', 'groups', i, 'string');
            });
            $$ = [ op, lhs, rhs ];
        }
    | AUTHORIZED-BY '(' String ')'
        {
            var lhs = '';
            var op = $1.toLowerCase();
            var rhs = $3;

            $$ = [ op, lhs , rhs];
        }
    | '(' OrCondition ')'
        {
            $$ = $2;
        }
    ;

Lhs
    : String '::' String // with type
        {
            $$ = {name: $1, type: $3};
        }
    | String // no type
        {
            $$ = {name: $1};
        }
    ;

// Simple list
// foo
// foo, bar
// foo, bar, baz
CommaSeparatedList
    : String
        {
            $$ = [ $1 ];
        }
    | CommaSeparatedList ',' String
        {
            $1.push($3);
            $$ = $1;
        }
    ;

// Identifiers used for principals, actions, or resources
Identifier
    : STRING
    | STRING_LITERAL
    | FUZZY_STRING
        {
            $$ = new RegExp(fuzzyToRegex($1));
        }
    | REGEX_LITERAL
        {
            var literal = $1;
            var last = literal.lastIndexOf("/");
            var body = literal.substring(1, last);
            var flags = literal.substring(last + 1);
            $$ = new RegExp(body, flags);
        }
    ;


All
    : ALL
    | EVERYTHING
    | ANYTHING
    | '*'
    ;

String
    : STRING
    | STRING_LITERAL
    | FUZZY_STRING
    | '*'
    ;

%%

/**
 * Extremely roundabout way of processing escaped asterisks in a fuzzy string,
 * since Javascript doesn't support negative lookbehinds.
 *
 * First, escape all RegExp special characters. Unescaped * will now be '\*' and
 * escaped * will now be '\\\*'.
 * Replace '\\\*' with '*'. All literal * will have no backslashes before them.
 * Replace '\*' with '.*'. All fuzzy * will now be '.*'
 * Finally, replace all * with no . before them into '\*' using negative
 * lookahead.
 *
 * as\*d*f -> as\\\*d\*f -> as*d\*f -> as*d.*f -> as\*d.*f
 */
function fuzzyToRegex(str) {
    str = str.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&');
    str = str.replace('\\\\\\*', '*');
    str = str.replace('\\*', '.*');
    str = str.replace(/(?:([^\.]))\*/,'$1\\*');
    return (str);
}
