/* vim: set ts=8 sw=4 tw=0 noet : */

/*
 * Copyright (c) 2020, Indian Institute of Science, Bengaluru
 *
 * Authors:
 * --------
 * Arun Babu    {barun       <at> iisc <dot> ac <dot> in}
 * Bryan Robert {bryanrobert <at> iisc <dot> ac <dot> in}
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

"use strict";

const fs			= require("fs");
const os			= require("os");
const dns			= require("dns");
const cors			= require("cors");
const ocsp			= require("ocsp");
const Pool			= require("pg").Pool;
const https			= require("https");
const assert			= require("assert").strict;
const chroot			= require("chroot");
const crypto			= require("crypto");
const logger			= require("node-color-log");
const lodash			= require("lodash");
const cluster			= require("cluster");
const express			= require("express");
const timeout			= require("connect-timeout");
const aperture			= require("./node-aperture");
const safe_regex		= require("safe-regex");
const geoip_lite		= require("geoip-lite");
const bodyParser		= require("body-parser");
const compression		= require("compression");
const http_request		= require("request");
const pgNativeClient		= require("pg-native");

const pg			= new pgNativeClient();

const TOKEN_LEN			= 16;
const TOKEN_LEN_HEX		= 2 * TOKEN_LEN;

const EUID			= process.geteuid();
const is_openbsd		= os.type() === "OpenBSD";
const pledge			= is_openbsd ? require("node-pledge")	: null;
const unveil			= is_openbsd ? require("openbsd-unveil"): null;

const NUM_CPUS			= os.cpus().length;
const SERVER_NAME		= fs.readFileSync ("server.name","ascii").trim();
const DOCUMENTATION_LINK	= fs.readFileSync ("documentation.link","ascii").trim();

const MAX_TOKEN_TIME		= 31536000; // in seconds (1 year)

const MIN_TOKEN_HASH_LEN	= 64;
const MAX_TOKEN_HASH_LEN	= 64;

const MAX_SAFE_STRING_LEN	= 512;

const MIN_CERT_CLASS_REQUIRED	= Object.freeze ({

/* resource server API */
	"/auth/v1/token/introspect"		: 1,
	"/auth/v1/certificate-info"		: 1,

/* data consumer's APIs */
	"/auth/v1/token"			: 2,

/* for credit topup */
	"/marketplace/topup-success"		: 2,

/* static files for marketplace */
	"/marketplace/topup.html"		: 2,
	"/marketplace/marketplace.js"		: 2,
	"/marketplace/marketplace.css"		: 2,

/* marketplace APIs */
	"/marketplace/v1/credit/info"		: 2,
	"/marketplace/v1/credit/topup"		: 2,
	"/marketplace/v1/confirm-payment"	: 2,
	"/marketplace/v1/audit/credits"		: 2,

	"/marketplace/v1/credit/transfer"	: 3,

/* data provider's APIs */
	"/auth/v1/audit/tokens"			: 3,

	"/auth/v1/token/revoke"			: 3,
	"/auth/v1/token/revoke-all"		: 3,

	"/auth/v1/acl"				: 3,
	"/auth/v1/acl/set"			: 3,
	"/auth/v1/acl/revert"			: 3,
	"/auth/v1/acl/append"			: 3,

	"/auth/v1/group/add"			: 3,
	"/auth/v1/group/delete"			: 3,
	"/auth/v1/group/list"			: 3,
});

const WHITELISTED_DOMAINS	= fs.readFileSync("whitelist.domains","ascii").trim().split("\n");
const WHITELISTED_ENDSWITH	= fs.readFileSync("whitelist.endswith","ascii").trim().split("\n");

const LAUNCH_ADMIN_PANEL	= fs.readFileSync("admin.panel","ascii").trim() === "yes";

/* --- API statistics --- */

const statistics = {

	"start_time"	: 0,

	"api"		: {
		"count" : {
			"invalid-api" : 0
		}
	}
};

/* --- environment variables--- */

process.env.TZ = "Asia/Kolkata";

/* --- dns --- */

dns.setServers ([
	"1.1.1.1",
	"4.4.4.4",
	"8.8.8.8",
	"[2001:4860:4860::8888]",
	"[2001:4860:4860::8844]",
]);

/* --- telegram --- */

const TELEGRAM		= "https://api.telegram.org";

const telegram_apikey	= fs.readFileSync ("telegram.apikey","ascii").trim();
const telegram_chat_id	= fs.readFileSync ("telegram.chatid","ascii").trim();

const telegram_url	= TELEGRAM + "/bot" + telegram_apikey +
				"/sendMessage?chat_id="	+ telegram_chat_id +
				"&text=";

/* --- postgres --- */

const DB_SERVER	= "postgres";

const password	= {
	//"DB"	: fs.readFileSync("passwords/auth.db.password","ascii").trim(),
	"DB"	: process.env.POSTGRES_PASSWORD
};

/* --- razorpay --- */

const rzpay_key_id	= fs.readFileSync("rzpay.key.id",	"ascii").trim();
const rzpay_key_secret	= fs.readFileSync("rzpay.key.secret",	"ascii").trim();

const rzpay_url		= "https://"					+
					rzpay_key_id			+
						":"			+
					rzpay_key_secret		+
				"@api.razorpay.com/v1/invoices/";

// async postgres connection
const pool = new Pool ({
	host		: DB_SERVER,
	port		: 5432,
	user		: "postgres",
	database	: "postgres",
	password	: password.DB,
});

pool.connect();

// sync postgres connection
pg.connectSync (
	"postgresql://postgres:"+ password.DB + "@" + DB_SERVER + ":5432/postgres",
		(err) =>
		{
			if (err) {
				throw err;
			}
		}
);

/* --- preload negotiator's encoding module for gzip compression --- */

const Negotiator = require("negotiator");
const negotiator = new Negotiator();

try		{ negotiator.encodings(); }
catch(x)	{ /* ignore */ }

/* --- express --- */

const app = express();

app.disable("x-powered-by");

app.use(timeout("5s"));
app.use(
	cors ({
		credentials	:	true,
		methods		:	["POST"],
		origin		:	(origin, callback) =>
					{
						callback (
							null,
							origin ? true : false
						);
					}
	})
);

app.use(compression());
app.use(bodyParser.raw({type:"*/*"}));

app.use(basic_security_check);
app.use(dns_check);
app.use(ocsp_check);

/* --- aperture --- */

const apertureOpts = {

	types		: aperture.types,
	typeTable	: {

		ip			: "ip",
		time			: "time",

		tokens_per_day		: "number",	// tokens issued today

		api			: "string",	// the API to be called
		method			: "string",	// the method for API

		"cert.class"		: "number",	// the certificate class
		"cert.cn"		: "string",
		"cert.o"		: "string",
		"cert.ou"		: "string",
		"cert.c"		: "string",
		"cert.st"		: "string",
		"cert.gn"		: "string",
		"cert.sn"		: "string",
		"cert.title"		: "string",

		"cert.issuer.cn"	: "string",
		"cert.issuer.email"	: "string",
		"cert.issuer.o"		: "string",
		"cert.issuer.ou"	: "string",
		"cert.issuer.c"		: "string",
		"cert.issuer.st"	: "string",

		groups			: "string",	// CSV actually

		country			: "string",
		region			: "string",
		timezone		: "string",
		city			: "string",
		latitude		: "number",
		longitude		: "number",
	}
};

const parser	= aperture.createParser		(apertureOpts);
const evaluator	= aperture.createEvaluator	(apertureOpts);

/* --- https --- */

const system_trusted_certs = is_openbsd ?
					"/etc/ssl/cert.pem" :
					"/etc/ssl/certs/ca-certificates.crt";

const trusted_CAs = [
	fs.readFileSync("datasetu-ca/certs/ca.crt"),
	fs.readFileSync(system_trusted_certs),
	fs.readFileSync("CCAIndia2015.cer"),
	fs.readFileSync("CCAIndia2014.cer")
];

//XXX
const https_options = Object.freeze ({
	key			: fs.readFileSync("https-key.pem"),
	cert			: fs.readFileSync("https-certificate.pem"),
	ca			: trusted_CAs,
	requestCert		: true,
	rejectUnauthorized	: true,
});

/* --- static pages --- */

const STATIC_PAGES = Object.freeze ({

/* GET end points */

	"/marketplace/topup.html":
				fs.readFileSync (
					"static/topup.html",		"ascii"
				),

	"/marketplace/marketplace.js":
				fs.readFileSync (
					"static/marketplace.js",	"ascii"
				),

	"/marketplace/marketplace.css":
				fs.readFileSync (
					"static/marketplace.css",	"ascii"
				),

/* templates */

	"topup-success-1.html"	: fs.readFileSync (
					"static/topup-success-1.html",	"ascii"
				),
	"topup-success-2.html"	: fs.readFileSync (
					"static/topup-success-2.html",	"ascii"
				),
	"topup-failure-1.html"	: fs.readFileSync (
					"static/topup-failure-1.html",	"ascii"
				),
	"topup-failure-2.html"	: fs.readFileSync (
					"static/topup-failure-2.html",	"ascii"
				),
});

const MIME_TYPE = Object.freeze({

	"js"	: "text/javascript",
	"css"	: "text/css",
	"html"	: "text/html"
});

const topup_success_1 = STATIC_PAGES["topup-success-1.html"];
const topup_success_2 = STATIC_PAGES["topup-success-2.html"];

const topup_failure_1 = STATIC_PAGES["topup-failure-1.html"];
const topup_failure_2 = STATIC_PAGES["topup-failure-2.html"];

/* --- functions --- */
function is_valid_token (token, user = null)
{
	if (! is_string_safe(token))
		return false;

	const split = token.split("/");

	if (split.length !== 3)
		return false;

	const issued_by		= split[0];
	const issued_to		= split[1];
	const random_hex	= split[2];

	if (issued_by !== SERVER_NAME)
		return false;

	if (random_hex.length !== TOKEN_LEN_HEX)
		return false;

	if (user && user !== issued_to)
		return false;		// token was not issued to this user

	if (! is_valid_email(issued_to))
		return false;

	return true;
}

function is_valid_tokenhash (token_hash)
{
	if (! is_string_safe(token_hash))
		return false;

	if (token_hash.length < MIN_TOKEN_HASH_LEN)
		return false;

	if (token_hash.length > MAX_TOKEN_HASH_LEN)
		return false;

	return true;
}

function is_valid_servertoken (server_token, hostname)
{
	if (! is_string_safe(server_token))
		return false;

	const split = server_token.split("/");

	if (split.length !== 2)
		return false;

	const issued_to		= split[0];
	const random_hex	= split[1];

	if (issued_to !== hostname)
		return false;

	if (random_hex.length !== TOKEN_LEN_HEX)
		return false;

	return true;
}

function sha1 (string)
{
	return crypto
		.createHash("sha1")
		.update(string)
		.digest("hex");
}

function sha256 (string)
{
	return crypto
		.createHash("sha256")
		.update(string)
		.digest("hex");
}

function base64 (string)
{
	return Buffer
		.from(string)
		.toString("base64");
}

function send_telegram_to_provider (consumer_id, provider_id, telegram_id, token_hash, request)
{
	pool.query ("SELECT chat_id FROM telegram WHERE id = $1::text LIMIT 1", [telegram_id], (error,results) =>
	{
		if (error)
			send_telegram ("Failed to get chat_id for : " + telegram_id + " : provider " + provider_id);
		else
		{
			const url		= TELEGRAM + "/bot" + telegram_apikey + "/sendMessage";

			const split		= request.id.split("/");
			const resource		= split.slice(2).join("/");

			const telegram_message	= {

				url		: url,
				form		: {
					chat_id		: results.rows[0].chat_id,

					text		: '[ DataSetu-AUTH ] #' + token_hash  + '#\n\n"'		+
									consumer_id				+
								'" wants to access "'				+
									resource + '"\n\n'			+
								"Request details:\n\n"				+
									JSON.stringify (request,null,"\t"),

					reply_markup	: JSON.stringify ({
						inline_keyboard	: [[
							{
								text		: "\u2714\ufe0f Allow",
								callback_data	: "allow"
							},
							{
								text		: "\u2716\ufe0f Deny",
								callback_data	: "deny"
							}
						]]
					})
				}
			};

			http_request.post (telegram_message, (error_1, response, body) => {

				if (error_1)
				{
					log ("yellow",
						"Telegram failed ! response = " +
							String(response)	+
						" body = "			+
							String(body)
					);
				}
			});
		}
	});
}

function send_telegram (message)
{
	http_request ( telegram_url + "[ AUTH ] : " + message, (error, response, body) =>
	{
		if (error)
		{
			log ("yellow",
				"Telegram failed ! response = " +
					String(response)	+
				" body = "			+
					String(body)
			);
		}
	});
}

function log(color, msg)
{
	const message = new Date() + " | " + msg;

	if (color === "red") {
		send_telegram(message);
	}

	logger.color(color).log(message);
}

function SERVE_HTML (req,res)
{
	const path	= req.url.split("?")[0];
	const page	= STATIC_PAGES[path];

	if (! page)
		return false;

	const split	= path.split(".");
	const extension	= split[split.length - 1].toLowerCase();

	const mime	= MIME_TYPE[extension] || "text/html";

	res.setHeader("Content-Type", mime);
	res.status(200).end(page);

	return true;
}

function SUCCESS (res, response = null)
{
	// if no response is given, just send success

	if (! response)
		response = {"success":true};

	res.setHeader("Content-Security-Policy",	"default-src 'none'");
	res.setHeader("Content-Type",			"application/json");

	res.status(200).end(JSON.stringify(response) + "\n");
}

function ERROR (res, http_status, error, exception = null)
{
	if (exception)
		log("red", String(exception).replace(/\n/g," "));

	res.setHeader("Content-Security-Policy",	"default-src 'none'");
	res.setHeader("Content-Type",			"application/json");
	res.setHeader("Connection",			"close");

	const response = {};

	if (typeof error === "string")
		response.error = {"message" : error};
	else
	{
		// error is already a JSON

		if (error["invalid-input"])
		{
			response["//"] ="Unsafe characters (if any) in"		+
					" 'invalid-input' field have been"	+
					" replaced with '*'";
		}

		response.error = error;
	}

	res.status(http_status).end(JSON.stringify(response) + "\n");

	res.socket.end();
	res.socket.destroy();

	delete res.socket;
	delete res.locals;
}

function show_statistics (req,res)
{
	const now	= Math.floor (Date.now() / 1000);
	const diff	= now - statistics.start_time;
	const time	= (new Date()).toJSON();

	const response = {
		time		: time,
		statistics	: []
	};

	for (const api in statistics.api.count)
	{
		const rate = statistics.api.count[api]/diff;

		response.statistics.push ({
			api	: api,
			count	: statistics.api.count[api],
			rate	: rate
		});
	}

	res.status(200).end(JSON.stringify(response,null,"\t") + "\n");
}

function is_valid_email (email)
{
	if (! email || typeof email !== "string")
		return false;

	if (email.length < 5 || email.length > 64)
		return false;

	// reject email ids starting with invalid chars
	const invalid_start_chars = ".-_@";

	if (invalid_start_chars.indexOf(email[0]) !== -1)
		return false;

	/*
		Since we use SHA1 (160 bits) for storing email hashes:

			the allowed chars in the email login is -._a-z0-9
			which is : 1 + 1 + 1 + 26 + 10 = ~40 possible chars

			the worst case brute force attack with 31 chars is
				40**31 > 2**160

			but for 30 chars it is
				40**30 < 2**160

			and since we have a good margin for 30 chars
				(2**160) - (40**30) > 2**157

			hence, as a precaution, limit the login length to 30.

		SHA1 has other attacks though, maybe we should switch to better
		hash algorithm in future.
	*/

	const split = email.split("@");

	if (split.length !== 2)
		return false;

	const user = split[0]; // the login email

	if (user.length === 0 || user.length > 30)
		return false;

	let num_dots = 0;

	for (const chr of email)
	{
		if (
			(chr >= "a" && chr <= "z") ||
			(chr >= "A" && chr <= "Z") ||
			(chr >= "0" && chr <= "9")
		)
		{
			// ok;
		}
		else
		{
			switch (chr)
			{
				case "-":
				case "_":
				case "@":
					break;

				case ".":
					++num_dots;
					break;

				default:
					return false;
			}
		}
	}

	if (num_dots < 1)
		return false;

	return true;
}

function is_certificate_ok (req, cert, validate_email)
{
	if (! cert || ! cert.subject)
		return "No subject found in the certificate";

	if (! cert.subject.CN)
		return "No CN found in the certificate";

	if (validate_email)
	{
		if (! is_valid_email(cert.subject.emailAddress))
			return "Invalid 'emailAddress' field in the certificate";

		if (! cert.issuer || ! cert.issuer.emailAddress)
			return "Certificate issuer has no 'emailAddress' field";

		const issuer_email = cert.issuer.emailAddress.toLowerCase();

		if (! is_valid_email(issuer_email))
			return "Certificate issuer's emailAddress is invalid";

		if (issuer_email.startsWith("datasetu.sub.ca@"))
		{
			const issued_to_domain	= cert.subject.emailAddress
							.toLowerCase()
							.split("@")[1];

			const issuer_domain	= issuer_email
							.toLowerCase()
							.split("@")[1];

			if (issuer_domain !== issued_to_domain)
			{
				// TODO
				// As this could be a fraud commited by a sub-CA
				// maybe revoke the sub-CA certificate

				log ("red",
					"Invalid certificate: issuer = "+
						issuer_domain		+
					" and issued to = "		+
						cert.subject.emailAddress
				);

				return "Invalid certificate issuer";
			}
		}
	}

	return "OK";
}

function is_secure (req, res, cert, validate_email = true)
{
	res.header("Referrer-Policy",		"no-referrer-when-downgrade");
	res.header("X-Frame-Options",		"deny");
	res.header("X-XSS-Protection",		"1; mode=block");
	res.header("X-Content-Type-Options",	"nosniff");

	if (req.headers.host && req.headers.host !== SERVER_NAME)
		return "Invalid 'host' field in the header";

	if (req.headers.origin)
	{
		const origin = req.headers.origin.toLowerCase();

		// e.g Origin = https://www.datasetu.org:8443/

		if (! origin.startsWith("https://"))
		{
			// allow the server itself to host "http"
			if (origin !== "http://" + SERVER_NAME)
				return "Insecure 'origin' field";
		}

		if ((origin.match(/\//g) || []).length < 2)
			return "Invalid 'origin' field";

		const origin_domain = String (
			origin
				.split("/")[2]	// remove protocol
				.split(":")[0]	// remove port number
		);

		let whitelisted = false;

		if (WHITELISTED_DOMAINS.indexOf(origin_domain) >= 0)
		{
			whitelisted = true;
		}
		else
		{
			for (const w of WHITELISTED_ENDSWITH)
			{
				if (origin_domain.endsWith(w))
				{
					whitelisted = true;
					break;
				}
			}
		}

		if (! whitelisted)
		{
			return "Invalid 'origin' header; this website is not"	+
				" whitelisted to call this API";
		}

		res.header("Access-Control-Allow-Origin", req.headers.origin);
		res.header("Access-Control-Allow-Methods","POST");
	}

	const error = is_certificate_ok (req,cert,validate_email);

	if (error !== "OK")
		return "Invalid certificate : " + error;

	return "OK";
}

function has_certificate_been_revoked (socket, cert, CRL)
{
	const cert_fingerprint	= cert.fingerprint
					.replace(/:/g,"")
					.toLowerCase();

	const cert_serial	= cert.serialNumber
					.toLowerCase()
					.replace(/^0+/,"");

	const cert_issuer	= cert.issuer.emailAddress.toLowerCase();

	for (const c of CRL)
	{
		c.issuer	= c.issuer.toLowerCase();
		c.serial	= c.serial.toLowerCase().replace(/^0+/,"");
		c.fingerprint	= c.fingerprint.toLowerCase().replace(/:/g,"");

		if (
			(c.issuer	=== cert_issuer)	&&
			(c.serial	=== cert_serial)	&&
			(c.fingerprint	=== cert_fingerprint)
		)
		{
			return true;
		}
	}

	// If it was issued by a sub-CA then check the sub-CA's cert too
	// Assuming depth is <= 3. ca@datasetu.org -> sub-CA -> user

	if (cert_issuer.startsWith("datasetu.sub.ca@"))
	{
		const ISSUERS = [];

		if (cert.issuerCertificate)
		{
			// both CA and sub-CA are the issuers
			ISSUERS.push(cert.issuerCertificate);

			if (cert.issuerCertificate.issuerCertificate)
			{
				ISSUERS.push (
					cert.issuerCertificate.issuerCertificate
				);
			}
		}
		else
		{
			/*
				if the issuerCertificate is empty,
				then the session must have been reused
				by the browser.
			*/

			if (! socket.isSessionReused())
				return true;
		}

		for (const issuer of ISSUERS)
		{
			if (issuer.fingerprint && issuer.serialNumber)
			{
				issuer.fingerprint = issuer
							.fingerprint
							.replace(/:/g,"")
							.toLowerCase();

				issuer.serialNumber = issuer
							.serialNumber
							.toLowerCase();

				for (const c of CRL)
				{
					if (c.issuer === "ca@datasetu.org")
					{
						const serial = c.serial
								.toLowerCase()
								.replace(/^0+/,"");

						const fingerprint = c.fingerprint
									.replace(/:/g,"")
									.toLowerCase();

						if (serial === issuer.serial && fingerprint === issuer.fingerprint)
							return true;
					}
				}
			}
			else
			{
				/*
					if fingerprint OR serial is undefined,
					then the session must have been reused
					by the browser.
				*/

				if (! socket.isSessionReused())
					return true;
			}
		}
	}

	return false;
}

function xss_safe (input)
{
	if (typeof input === "string")
		return input.replace(/[^-a-zA-Z0-9:/.@_]/g,"*");
	else
	{
		// we can only change string variables

		return input;
	}
}

function is_string_safe (str, exceptions = "")
{
	if (! str || typeof str !== "string")
		return false;

	if (str.length === 0 || str.length > MAX_SAFE_STRING_LEN)
		return false;

	exceptions = exceptions + "-/.@";

	for (const ch of str)
	{
		if (
			(ch >= "a" && ch <= "z") ||
			(ch >= "A" && ch <= "Z") ||
			(ch >= "0" && ch <= "9")
		)
		{
			// ok
		}
		else
		{
			if (exceptions.indexOf(ch) === -1)
				return false;
		}
	}

	return true;
}

function is_datasetu_certificate(cert)
{
	if (! cert.issuer.emailAddress)
		return false;

	const email = cert
			.issuer
			.emailAddress
			.toLowerCase();

	// certificate issuer should be DataSetu CA or a DataSetu sub-CA

	return (email ==="ca@datasetu.org" || email.startsWith("datasetu.sub.ca@"));
}

function body_to_json (body)
{
	if (! body)
		return {};

	let string_body;

	try
	{
		string_body = Buffer
				.from(body,"utf-8")
				.toString("ascii")
				.trim();

		if (string_body.length === 0)
			return {};
	}
	catch (x)
	{
		return {};
	}

	try
	{
		const json_body = JSON.parse (string_body);

		if (json_body)
			return json_body;
		else
			return {};
	}
	catch (x)
	{
		return null;
	}
}

/* ---
	A variable to indicate if a worker has started serving APIs.

	We will further drop privileges when a worker is about to
	serve its first API.
				--- */

let has_started_serving_apis = false;

/* --- basic security checks to be done at every API call --- */

function basic_security_check (req, res, next)
{
	if (! has_started_serving_apis)
	{
		if (is_openbsd) // drop "rpath" in worker
			pledge.init("error stdio tty prot_exec inet dns recvfd");

		has_started_serving_apis = true;
	}

	// replace all version with "/v1/"

	const endpoint			= req.url.split("?")[0];
	const api			= endpoint.replace(/\/v[1-2]\//,"/v1/");
	const min_class_required	= MIN_CERT_CLASS_REQUIRED[api];

	if (LAUNCH_ADMIN_PANEL)
		process.send(endpoint);

	if (! min_class_required)
	{
		return ERROR (
			res, 404,
				"No such page/API. Please visit : "	+
				DOCUMENTATION_LINK + " for documentation."
		);
	}

	if (! (res.locals.body = body_to_json(req.body)))
	{
		return ERROR (
			res, 400,
			"Body is not a valid JSON"
		);
	}

	const cert		= req.socket.getPeerCertificate(true);

	cert.serialNumber	= cert.serialNumber.toLowerCase();
	cert.fingerprint	= cert.fingerprint.toLowerCase();

	if ((res.locals.is_datasetu_certificate = is_datasetu_certificate(cert)))
	{
		// id-qt-unotice is in the format "key1:value1;key2:value2;..."

		const id_qt_notice	= cert.subject["id-qt-unotice"] || "";
		const split		= id_qt_notice.split(";");
		const user_notice	= {};

		for (const s of split)
		{
			const	ss	= s.split(":");	// ss = split of split

			let	key	= ss[0];
			let	value	= ss[1];

			if (key && value)
			{
				key	= key.toLowerCase();
				value	= value.toLowerCase();

				user_notice[key] = value;
			}
		}

		if (user_notice.untrusted)
		{
			res.locals.untrusted = true;

			if (api.startsWith("/marketplace/"))
			{
				return ERROR (
					res, 403,
					"Untrusted Apps cannot call "	+
					"marketplace APIs"
				);
			}
		}

		if (user_notice["delegated-by"])
		{
			return ERROR (
				res, 403,
					"Delegated certificates cannot"	+
					" be used to call auth/marketplace APIs"
			);
		}

		const	cert_class		= user_notice["class"];
		let	integer_cert_class	= 0;

		if (cert_class)
			integer_cert_class = parseInt(cert_class,10) || 0;

		if (integer_cert_class < 1)
			return ERROR(res, 403, "Invalid certificate class");

		if (integer_cert_class < min_class_required)
		{
			return ERROR (
				res, 403,
					"A class-" + min_class_required	+
					" or above certificate "	+
					"is required to call this API"
			);
		}

		if (min_class_required === 1 && integer_cert_class !== 1)
		{
			/*
				class-1 APIs are special,
				user needs a class-1 certificate

				except in case of "/certificate-info"
			*/

			if (! api.endsWith("/certificate-info"))
			{
				return ERROR (
					res, 403,
					"A class-1 certificate is required " +
					"to call this API"
				);
			}
		}

		const error = is_secure(req,res,cert,true); // validate emails

		if (error !== "OK")
			return ERROR (res, 403, error);

		pool.query("SELECT crl FROM crl LIMIT 1", [], (error, results) =>
		{
			if (error || results.rows.length === 0)
			{
				return ERROR (
					res, 500,
					"Internal error!", error
				);
			}

			const CRL = results.rows[0].crl;

			if (has_certificate_been_revoked(req.socket,cert,CRL))
			{
				return ERROR (
					res, 403,
					"Certificate has been revoked"
				);
			}

			res.locals.cert		= cert;
			res.locals.cert_class	= integer_cert_class;
			res.locals.email	= cert
							.subject
							.emailAddress
							.toLowerCase();

			if (user_notice["can-access"])
			{
				res.locals.can_access_regex	= [];

				const can_access_regex		= user_notice["can-access"]
									.split(";");
				let regex_number		= 0;

				for (const r of can_access_regex)
				{
					++regex_number;

					const regex = r.trim();

					if (regex === "")
						continue;

					/*
						allow '^' '*' and '$' characters
						but not unsafe RegEx
					*/

					if (! is_string_safe(regex,"^*$"))
					{
						const error_response = {
							"message"	: "Unsafe 'can-access' RegEx in certificate",
							"invalid-input"	: "RegEx no. " + regex_number,
						};

						return ERROR (
							res, 400,
								error_response
						);
					}

					/*
						We don't support ".", replace:
							"."	with	"\."
							"*"	with	".*"
					*/

					const final_regex = regex
								.replace(/\./g,"\\.")
								.replace(/\*/g,".*");

					if (! safe_regex(final_regex))
					{
						const error_response = {
							"message"	: "Unsafe 'can-access' RegEx in certificate",
							"invalid-input"	: "RegEx no. " + regex_number,
						};

						return ERROR (
							res, 400,
								error_response
						);
					}

					res.locals.can_access_regex.push (
						new RegExp(final_regex)
					);
				}
			}

			Object.freeze(res.locals);
			Object.freeze(res.locals.body);
			Object.freeze(res.locals.cert);

			return next();
		});
	}
	else
	{
		/*
			Certificates issued by other CAs
			may not have an "emailAddress" field.
			By default consider them as a class-1 certificate
		*/

		const error = is_secure(req,res,cert,false);

		if (error !== "OK")
			return ERROR (res, 403, error);

		res.locals.cert_class	= 1;
		res.locals.email	= "";
		res.locals.cert		= cert;

		/*
			But if the certificate has a valid "emailAddress"
			field then we consider it as a class-2 certificate
		*/

		if (is_valid_email(cert.subject.emailAddress))
		{
			res.locals.cert_class	= 2;
			res.locals.email	= cert
							.subject
							.emailAddress
							.toLowerCase();
		}

		/*
			class-1 APIs are special,
			user needs a class-1 certificate

			except in case of "/certificate-info"

			if user is trying to call a class-1 API,
			then downgrade his certificate class
		*/

		if (min_class_required === 1)
		{
			if (! api.endsWith("/certificate-info"))
			{
				res.locals.cert_class = 1;
			}
		}

		if (res.locals.cert_class < min_class_required)
		{
			return ERROR (
				res, 403,
				"A class-" + min_class_required	+
				" or above certificate is"	+
				" required to call this API"
			);
		}

		Object.freeze(res.locals);
		Object.freeze(res.locals.body);
		Object.freeze(res.locals.cert);

		return next();
	}
}

function dns_check (req, res, next)
{
	const cert		= res.locals.cert;
	const cert_class	= res.locals.cert_class;

	// No dns check required if certificate is class-2 or above

	if (cert_class > 1)
		return next();

	if (! cert.subject || ! is_string_safe(cert.subject.CN))
		return ERROR (res, 400, "Invalid 'CN' in the certificate");

	const	ip			= req.connection.remoteAddress;
	let	ip_matched		= false;
	const	hostname_in_certificate	= cert.subject.CN.toLowerCase();

	dns.lookup (hostname_in_certificate, {all:true}, (error, ip_addresses) =>
	{
		/*
			No dns checks for "example.com"
			this for developer's testing purposes.
		*/

		if (hostname_in_certificate === "example.com")
		{
			error		= null;
			ip_matched	= true;
			ip_addresses	= [];
		}

		if (error)
		{
			const error_response = {
				"message"	: "Invalid 'hostname' in certificate",
				"invalid-input"	: xss_safe(hostname_in_certificate)
			};

			return ERROR (res, 400, error_response);
		}

		for (const a of ip_addresses)
		{
			if (a.address === ip)
			{
				ip_matched = true;
				break;
			}
		}

		if (! ip_matched)
		{
			return ERROR (res, 403,
				"Your certificate's hostname in CN "	+
				"and your IP does not match!"
			);
		}

		return next();  // dns check passed
	});
}

function ocsp_check (req, res, next)
{
	const cert = res.locals.cert;

	// Skip ocsp check if an DataSetu certificate was presented

	if (res.locals.is_datasetu_certificate)
		return next();

	if (! cert.issuerCertificate || ! cert.issuerCertificate.raw)
	{
		if (req.socket.isSessionReused())
		{
			return next(); // previously ocsp check was passed !
		}
		else
		{
			return ERROR (
				res, 400,
				"Something is wrong with your client/browser !"
			);
		}
	}

	const ocsp_request = {
		cert	: cert.raw,
		issuer	: cert.issuerCertificate.raw
	};

	ocsp.check (ocsp_request, (ocsp_error, ocsp_response) =>
	{
		if (ocsp_error)
		{
			return ERROR (
				res, 403,
				"Your certificate issuer did "	+
				"NOT respond to an OCSP request"
			);
		}

		if (ocsp_response.type !== "good")
		{
			return ERROR (
				res, 403,
				"Your certificate has been "	+
				"revoked by your certificate issuer"
			);
		}

		return next();	// ocsp check passed
	});
}

function to_array (o)
{
	if (o instanceof Object)
	{
		if (o instanceof Array)
			return o;
		else
			return [o];
	}
	else
	{
		return [o];
	}
}

/* --- Auth APIs --- */

app.post("/auth/v[1-2]/token", (req, res) => {

	const cert				= res.locals.cert;
	const cert_class			= res.locals.cert_class;
	const body				= res.locals.body;
	const consumer_id			= res.locals.email;

	const resource_id_dict			= {};
	const resource_server_token		= {};
	const sha256_of_resource_server_token	= {};

	const request_array			= to_array(body.request);
	const processed_request_array		= [];
	const manual_authorization_array	= [];

	if (! request_array || request_array.length < 1)
	{
		return ERROR (
			res, 400,
				"'request' must be a valid JSON array " +
				"with at least 1 element"
		);
	}

	let requested_token_time;		// as specified by the consumer
	let token_time = MAX_TOKEN_TIME;	// to be sent along with token

	if (body["token-time"])
	{
		requested_token_time = parseInt(body["token-time"],10);

		if (
			isNaN(requested_token_time)		||
			requested_token_time < 1		||
			requested_token_time > MAX_TOKEN_TIME
		)
		{
			return ERROR (
				res, 400,
				"'token-time' should be > 0 and < " +
				MAX_TOKEN_TIME
			);
		}
	}

	const rows = pg.querySync (

		"SELECT COUNT(*)/60.0"		+
		" AS rate"			+
		" FROM token"			+
		" WHERE id = $1::text"		+
		" AND issued_at >= (NOW() - interval '60 seconds')",
		[
			consumer_id,		// 1
		]
	);

	// in last 1 minute
	const tokens_rate_per_second = parseFloat (rows[0].rate);

	if (tokens_rate_per_second > 1) // tokens per second
	{
		log ("red",
			"Too many requests from user : " + consumer_id +
			", from ip : " + String (req.connection.remoteAddress)
		);

		return ERROR (res, 429, "Too many requests");
	}

	const ip	= req.connection.remoteAddress;
	const issuer	= cert.issuer;

	const geoip	= geoip_lite.lookup(ip) || {ll:[]};

	// these fields are not necessary

	delete geoip.eu;
	delete geoip.area;
	delete geoip.metro;
	delete geoip.range;

	Object.freeze(geoip);

	const context = {

		principal	: consumer_id,
		action		: "access",

		conditions	: {
			ip			: ip,
			time			: new Date(),

			"cert.class"		: cert_class,
			"cert.cn"		: cert.subject.CN	|| "",
			"cert.o"		: cert.subject.O	|| "",
			"cert.ou"		: cert.subject.OU	|| "",
			"cert.c"		: cert.subject.C	|| "",
			"cert.st"		: cert.subject.ST	|| "",
			"cert.gn"		: cert.subject.GN	|| "",
			"cert.sn"		: cert.subject.SN	|| "",
			"cert.title"		: cert.subject.title	|| "",

			"cert.issuer.cn"	: issuer.CN		|| "",
			"cert.issuer.email"	: issuer.emailAddress	|| "",
			"cert.issuer.o"		: issuer.O		|| "",
			"cert.issuer.ou"	: issuer.OU		|| "",
			"cert.issuer.c"		: issuer.C		|| "",
			"cert.issuer.st"	: issuer.ST		|| "",

			country			: geoip.country		|| "",
			region			: geoip.region		|| "",
			timezone		: geoip.timezone	|| "",
			city			: geoip.city		|| "",
			latitude		: geoip.ll[0]		|| 0,
			longitude		: geoip.ll[1]		|| 0,
		}
	};

	const providers			= {};

	let num_rules_passed		= 0;
	let total_data_cost_per_second	= 0.0;

	const payment_info		= {
		amount		: 0.0,
		providers	: {}
	};

	const can_access_regex = res.locals.can_access_regex;

	for (let r of request_array)
	{
		let resource;
		let requires_manual_authorization = false;

		if (typeof r === "string")
		{
			resource = r;

			// request is a string, make it an object

			r = {
				"id" : resource,
			};
		}
		else if (r instanceof Object)
		{
			if (! r.id)
			{
				const error_response = {
					"message"	: "no resource 'id' found in request",
					"invalid-input"	: xss_safe(r),
				};

				return ERROR (res, 400, error_response);
			}

			resource = r.id;
		}
		else
		{
			const error_response = {
				"message"	: "Invalid resource 'id' found in request",
				"invalid-input"	: xss_safe(String(r)),
			};

			return ERROR (res, 400, error_response);
		}

		// allow some chars but not ".."

		if (! is_string_safe(resource, "*_") || resource.indexOf("..") >= 0)
		{
			const error_response = {
				"message"	: "'id' contains unsafe characters",
				"invalid-input"	: xss_safe(resource),
			};

			return ERROR (res, 400, error_response);
		}

		if (typeof r.method === "string")
			r.methods = [r.method];

		if (! r.methods)
			r.methods = ["*"];

		if (! (r.methods instanceof Array))
		{
			const error_response = {
				"message"	: "'methods' must be a valid JSON array",
				"invalid-input"	: {
					"id"		: xss_safe(resource),
					"methods"	: xss_safe(r.methods)
				}
			};

			return ERROR (res, 400, error_response);
		}

		if (r.api && typeof r.api === "string")
			r.apis = [r.api];

		if (! r.apis)
			r.apis = ["/*"];

		if (! r.body)
			r.body = null;

		if ( ! (r.apis instanceof Array))
		{
			const error_response = {
				"message"	: "'apis' must be a valid JSON array",
				"invalid-input"	: {
					"id"	: xss_safe(resource),
					"apis"	: xss_safe(r.apis)
				}
			};

			return ERROR (res, 400, error_response);
		}

		if ((resource.match(/\//g) || []).length < 3)
		{
			const error_response = {
				"message"	: "'id' must have at least 3 '/' characters.",
				"invalid-input"	: xss_safe(resource)
			};

			return ERROR (res, 400, error_response);
		}

		// if body is given but is not a valid object
		if (r.body && (! (r.body instanceof Object)))
		{
			const error_response = {
				"message"	: "'body' must be a valid JSON object",
				"invalid-input"	: {
					"id"	: xss_safe(resource),
					"body"	: xss_safe(r.body)
				}
			};

			return ERROR (res, 400, error_response);
		}

		if (can_access_regex)
		{
			let access_denied = true;

			for (const regex of can_access_regex)
			{
				if (resource.match(regex))
				{
					access_denied = false;
					break;
				}
			}

			if (access_denied)
			{
				const error_response = {
					"message"	: "Your certificate does not allow access to this 'id'",
					"invalid-input"	: {
						"id"	: xss_safe(resource),
					}
				};

				return ERROR (res, 403, error_response);
			}
		}

		const split			= resource.split("/");

		const email_domain		= split[0].toLowerCase();
		const sha1_of_email		= split[1].toLowerCase();

		const provider_id_hash		= email_domain + "/" + sha1_of_email;

		const resource_server		= split[2].toLowerCase();
		const resource_name		= split.slice(3).join("/");

		providers			[provider_id_hash]	= true;

		// to be generated later
		resource_server_token		[resource_server]	= true;

		// to be generated later
		sha256_of_resource_server_token	[resource_server]	= true;

		const rows = pg.querySync (

			"SELECT policy,policy_in_json"	+
			" FROM policy"			+
			" WHERE id = $1::text"		+
			" LIMIT 1",
			[
				provider_id_hash,	// 1
			]
		);

		if (rows.length === 0)
		{
			const error_response = {

				"message"	:"Invalid 'id'; no access"	+
						" control policies have been"	+
						" set for this 'id'"		+
						" by the data provider",

				"invalid-input"	: xss_safe(resource)
			};

			return ERROR (res, 400, error_response);
		}

		const policy_lowercase = Buffer.from (
						rows[0].policy, "base64"
					)
					.toString("ascii")
					.toLowerCase();

		const policy_in_json	= rows[0].policy_in_json;

		// full name of resource eg: bangalore.domain.com/streetlight-1
		context.resource = resource_server + "/" + resource_name;

		context.conditions.groups = "";

		if (policy_lowercase.search(" consumer-in-group") >= 0)
		{
			const rows = pg.querySync (

				"SELECT DISTINCT group_name"	+
				" FROM groups"			+
				" WHERE id = $1::text"		+
				" AND consumer = $2::text"	+
				" AND valid_till > NOW()",
				[
					provider_id_hash,	// 1
					consumer_id		// 2
				]
			);

			const group_array = [];
			for (const g of rows)
				group_array.push(g.group_name);

			context.conditions.groups = group_array.join();
		}

		context.conditions.tokens_per_day = 0;

		if (policy_lowercase.search(" tokens_per_day ") >= 0)
		{
			const resource_true = {};
				resource_true [resource] = true;

			const rows = pg.querySync (

				"SELECT COUNT(*) FROM token"		+
				" WHERE id = $1::text"			+
				" AND resource_ids @> $2::jsonb"	+
				" AND issued_at >= DATE_TRUNC('day',NOW())",
				[
					consumer_id,			// 1
					JSON.stringify(resource_true),	// 2
				]
			);

			context.conditions.tokens_per_day = parseInt (
				rows[0].count, 10
			);
		}

		let CTX = context;

		if (r.body && policy_lowercase.search(" body.") >= 0)
		{
			// deep copy
			CTX = JSON.parse(JSON.stringify(context));

			for (const key in r.body)
				CTX.conditions["body." + key] = r.body[key];
		}

		for (const api of r.apis)
		{
			if (typeof api !== "string")
			{
				const error_response = {
					"message"	: "'api' must be a string",
					"invalid-input"	: {
						"id"	: xss_safe(resource),
						"api"	: xss_safe(api)
					}
				};

				return ERROR (res, 400, error_response);
			}

			CTX.conditions.api = api;

			for (const method of r.methods)
			{
				if (typeof method !== "string")
				{
					const error_response = {
						"message"	: "'method' must be a string",
						"invalid-input"	: {
							"id"		: xss_safe(resource),
							"method"	: xss_safe(method)
						}
					};

					return ERROR (res, 400, error_response);
				}

				CTX.conditions.method = method;

				try
				{
					// token expiry time as specified by
					// the provider in the policy

					const result = evaluator.evaluate (
						policy_in_json,
						CTX
					);

					const token_time_in_policy	= result.expiry || 0;
					const payment_amount		= result.amount || 0.0;

					requires_manual_authorization	= requires_manual_authorization	||
										result["manual-authorization"];

					if (token_time_in_policy < 1 || payment_amount < 0.0)
					{
						const error_response = {
							"message"	: "Unauthorized",
							"invalid-input"	: {
								"id"		: xss_safe(resource),
								"api"		: xss_safe(api),
								"method"	: xss_safe(method)
							}
						};

						return ERROR (res, 403, error_response);
					}

					const cost_per_second		= payment_amount / token_time_in_policy;

					total_data_cost_per_second	+= cost_per_second;

					if (! payment_info.providers[provider_id_hash])
						payment_info.providers[provider_id_hash] = 0.0;

					payment_info.providers[provider_id_hash] += cost_per_second;

					token_time = Math.min (
						token_time,
						token_time_in_policy
					);
				}
				catch (x)
				{
					const error_response = {
						"message"	: "Unauthorized",
						"invalid-input"	: {
							"id"		: xss_safe(resource),
							"api"		: xss_safe(api),
							"method"	: xss_safe(method)
						}
					};

					return ERROR (res, 403, error_response);
				}
			}
		}

		if (requested_token_time)
			token_time = Math.min(requested_token_time,token_time);

		if (token_time < 1)
		{
			const error_response = {
				"message" : "token validity is less than 1 second"
			};

			return ERROR (res, 400, error_response);
		}

		if (requires_manual_authorization)
		{
			manual_authorization_array.push ({
				"id"			: resource,
				"methods"		: r.methods,
				"apis"			: r.apis,
				"body"			: r.body,
				"manual-authorization"	: requires_manual_authorization,
			});
		}
		else
		{
			processed_request_array.push ({
				"id"			: resource,
				"methods"		: r.methods,
				"apis"			: r.apis,
				"body"			: r.body,
			});
		}

		resource_id_dict[resource] = true;

		++num_rules_passed;
	}

	if (num_rules_passed < 1 || num_rules_passed < request_array.length)
		return ERROR (res, 403, "Unauthorized!");

	const random_hex = crypto
				.randomBytes(TOKEN_LEN)
				.toString("hex");

	/* Token format = issued-by / issued-to / random-hex-string */

	const token	= SERVER_NAME + "/" + consumer_id + "/" + random_hex;

	const response	= {

		"token"			: token,
		"token-type"		: "DATASETU",
		"expires-in"		: token_time,

		"//"			: "",
		"is_token_valid"	: true,

		"payment-info"	: {
			"amount"	: 0.0,
			"currency"	: "INR",
		},
	};

	if (manual_authorization_array.length > 0)
	{
		response["//"]		+= "This token requires manual authorization from the provider. ";
		response.is_token_valid	= false;
	}

	const total_payment_amount = total_data_cost_per_second * token_time;

	if (total_payment_amount > 0)
	{
		if (res.locals.untrusted)
		{
			return ERROR (
				res, 403,
				"Untrusted Apps cannot get tokens requiring credits"
			);
		}

		const query	= "SELECT amount FROM credit"			+
					" WHERE id = $1::text"			+
					" AND cert_serial = $2::text"		+
					" AND cert_fingerprint = $3::text"	+
					" LIMIT 1";

		const params	= (cert_class > 2) ?
					[consumer_id, "*", "*"]	:
					[consumer_id, cert.serialNumber, cert.fingerprint];

		const rows	= pg.querySync (query, params);
		const credits	= (rows.length === 1) ? rows[0].amount : 0.0;

		if (total_payment_amount > credits)
		{
			return ERROR (
				res, 402,
					"Not enough balance in credits for : "	+
					total_payment_amount			+
					" Rupees"
			);
		}

		payment_info.amount		= total_payment_amount;
		response["payment-info"].amount	= total_payment_amount;

		response["//"]		+= "This token requires payment authorization;"	+
						"please use the 'confirm-payment' API to approve";
		response.is_token_valid	= false;
	}

	const num_resource_servers = Object
					.keys(resource_server_token)
					.length;

	if (num_resource_servers > 1)
	{
		for (const key in resource_server_token)
		{
			/* server-token format = issued-to / random-hex-string */

			resource_server_token[key] = key + "/" +
							crypto
							.randomBytes(TOKEN_LEN)
							.toString("hex");

			sha256_of_resource_server_token[key] = sha256 (
				resource_server_token[key]
			);
		}
	}

	response["server-token"]	= resource_server_token;
	const sha256_of_token		= sha256(token);

	const paid = total_payment_amount > 0.0 ? false : true;

	const query = "INSERT INTO token VALUES("		+
			"$1::text,"				+
			"$2::text,"				+
			"NOW() + $3::interval,"			+ // expiry
			"$4::jsonb,"				+
			"$5::text,"				+
			"$6::text,"				+
			"NOW(),"				+ // issued_at
			"$7::jsonb,"				+
			"false,"				+ // introspected
			"false,"				+ // revoked
			"$8::int,"				+
			"$9::jsonb,"				+
			"$10::jsonb,"				+
			"$11::jsonb,"				+
			"$12::jsonb,"				+
			"$13::boolean,"				+
			"NULL,"					+ // paid_at
			"$14::text,"				+ // api_called_from
			"$15::jsonb"				+ // manual_authorization_array
	")";

	const params = [
		consumer_id,					//  1
		sha256_of_token,				//  2
		token_time + " seconds",			//  3
		JSON.stringify(processed_request_array),	//  4
		cert.serialNumber,				//  5
		cert.fingerprint,				//  6
		JSON.stringify(resource_id_dict),		//  7
		cert_class,					//  8
		JSON.stringify(sha256_of_resource_server_token),//  9
		JSON.stringify(providers),			// 10
		JSON.stringify(geoip),				// 11
		JSON.stringify(payment_info),			// 12
		paid,						// 13
		req.headers.origin,				// 14
		JSON.stringify(manual_authorization_array)	// 15
	];

	pool.query (query, params, (error,results) =>
	{
		if (error || results.rowCount === 0)
		{
			return ERROR (
				res, 500,
				"Internal error!", error
			);
		}

		for (const m of manual_authorization_array)
		{
			const split		= m.id.split("/");

			const email_domain	= split[0].toLowerCase();
			const sha1_of_email	= split[1].toLowerCase();

			const provider_id_hash	= email_domain + "/" + sha1_of_email;

			const telegram_id = m["manual-authorization"].split("telegram:")[1];

			send_telegram_to_provider (
				consumer_id,
				provider_id_hash,
				telegram_id,
				sha256_of_token,
				m
			);
		}

		return SUCCESS (res,response);
	});
});

app.post("/auth/v[1-2]/token/introspect", (req, res) => {

	const cert			= res.locals.cert;
	const body			= res.locals.body;

	const hostname_in_certificate	= cert.subject.CN.toLowerCase();

	if (! body.token)
		return ERROR (res, 400, "No 'token' found in the body");

	if (! is_valid_token(body.token))
		return ERROR (res, 400, "Invalid 'token'");

	const token		= body.token.toLowerCase();
	let server_token	= body["server-token"] || true;

	if (server_token === true || server_token === "" || server_token === "true")
	{
		server_token = true;
	}
	else
	{
		server_token = server_token.toLowerCase();

		if (! is_valid_servertoken(server_token, hostname_in_certificate))
			return ERROR (res, 400, "Invalid 'server-token'");
	}

	const consumer_request = body.request;

	if (consumer_request)
	{
		if (! (consumer_request instanceof Array))
		{
			return ERROR (
				res, 400,
				"'request' must be an valid JSON array"
			);
		}

		Object.freeze(consumer_request);
	}

	const split		= token.split("/");
	const issued_to		= split[1];

	const sha256_of_token	= sha256(token);

	pool.query (

		"SELECT expiry,request,cert_class,"		+
		" server_token,providers"			+
		" FROM token"					+
		" WHERE id = $1::text"				+
		" AND token = $2::text"				+
		" AND revoked = false"				+
		" AND paid = true"				+
		" AND expiry > NOW()"				+
		" LIMIT 1",
		[
			issued_to,				// 1
			sha256_of_token				// 2
		],

		(error, results) =>
		{
			if (error)
			{
				return ERROR (
					res, 500, "Internal error!", error
				);
			}

			if (results.rows.length === 0)
				return ERROR (res, 403, "Invalid 'token'");

			const expected_server_token = results
							.rows[0]
							.server_token[hostname_in_certificate];

			// if token doesn't belong to this server
			if (! expected_server_token)
				return ERROR (res, 403, "Invalid 'token'");

			const num_resource_servers = Object.keys (
				results.rows[0].server_token
			).length;

			if (num_resource_servers > 1)
			{
				if (server_token === true) // should be a real token
				{
					return ERROR (
						res, 403,
						"Invalid 'server-token'"
					);
				}

				const sha256_of_server_token = sha256(server_token);

				if (sha256_of_server_token !== expected_server_token)
				{
					return ERROR (
						res, 403,
						"Invalid 'server-token'"
					);
				}
			}
			else
			{
				// token belongs to only 1 server

				if (server_token === true && expected_server_token === true)
				{
					// ok
				}
				else if (typeof expected_server_token === "string")
				{
					const sha256_of_server_token = sha256(server_token);

					if (sha256_of_server_token !== expected_server_token)
					{
						return ERROR (
							res, 403,
							"Invalid 'server-token'"
						);
					}
				}
				else
				{
					return ERROR (
						res, 500,
						"Invalid 'expected_server_token' in DB"
					);
				}
			}

			const request	= results.rows[0].request;
			const providers	= results.rows[0].providers;

			const request_for_resource_server = [];

			for (const r of request)
			{
				const split		= r.id.split("/");

				const email_domain	= split[0].toLowerCase();
				const sha1_of_email	= split[1].toLowerCase();

				const provider_id_hash	= email_domain + "/" + sha1_of_email;

				const resource_server	= split[2].toLowerCase();

				// if provider exists
				if (providers[provider_id_hash])
				{
					if (resource_server === hostname_in_certificate)
						request_for_resource_server.push (r);
				}
			}

			Object.freeze(request_for_resource_server);

			if (request_for_resource_server.length === 0)
				return ERROR (res, 403, "Invalid 'token'");

			if (consumer_request)
			{
				const l1 = Object.keys(
					consumer_request
				).length;

				const l2 = Object.keys(
					request_for_resource_server
				).length;

				// more number of requests than what is allowed

				if (l1 > l2)
				{
					return ERROR (
						res, 403, "Unauthorized !"
					);
				}

				for (const r1 of consumer_request)
				{
					if (! (r1 instanceof Object))
					{
						const error_response = {
							"message"	: "'request' must be a valid JSON object",
							"invalid-input"	: xss_safe(r1)
						};

						return ERROR (res, 400,
							error_response
						);
					}

					// default values

					if (! r1.methods)
						r1.methods = ["*"];

					if (! r1.apis)
						r1.apis = ["/*"];

					if (! r1.body)
						r1.body = null;

					Object.freeze(r1);

					let resource_found = false;

					for (const r2 of request_for_resource_server)
					{
						Object.freeze(r2);

						if (r1.id === r2.id)
						{
							if (! lodash.isEqual(r1,r2))
							{
								const error_response = {
									"message"	: "Unauthorized",
									"invalid-input"	: xss_safe(r1.id)
								};

								return ERROR (res, 403, error_response);
							}

							resource_found = true;
							break;
						}
					}

					if (! resource_found)
					{
						const error_response = {
							"message"	: "Unauthorized",
							"invalid-input"	: xss_safe(r1.id),
						};

						return ERROR (res, 403, error_response);
					}
				}
			}

			const response = {
				"consumer"			: issued_to,
				"expiry"			: results.rows[0].expiry,
				"request"			: request_for_resource_server,
				"consumer-certificate-class"	: results.rows[0].cert_class,
			};

			pool.query (

				"UPDATE token SET introspected = true"	+
				" WHERE token = $1::text"		+
				" AND introspected = false"		+
				" AND revoked = false"			+
				" AND expiry > NOW()",
				[
					sha256_of_token,		// 1
				],

				(update_error) =>
				{
					if (update_error)
					{
						return ERROR (
							res, 500,
							"Internal error!",
							update_error
						);
					}

					return SUCCESS (res,response);
				}
			);
		}
	);
});

app.post("/auth/v[1-2]/token/revoke", (req, res) => {

	const id		= res.locals.email;
	const body		= res.locals.body;

	const tokens		= body.tokens;
	const token_hashes	= body["token-hashes"];

	if (tokens && token_hashes)
	{
		return ERROR (
			res, 400,
			"Provide either 'tokens' or 'token-hashes'; but not both"
		);
	}

	if ( (! tokens) && (! token_hashes))
	{
		return ERROR (
			res, 400,
			"No 'tokens' or 'token-hashes' found"
		);
	}

	let num_tokens_revoked = 0;

	if (tokens)
	{
		// user is a consumer

		if (! (tokens instanceof Array))
			return ERROR (res, 400, "'tokens' must be a valid JSON array");

		for (const token of tokens)
		{
			if (! is_valid_token(token, id))
			{
				const error_response = {
					"message"		: "Invalid 'token'",
					"invalid-input"		: xss_safe(token),
					"num-tokens-revoked"	: num_tokens_revoked
				};

				return ERROR (res, 400, error_response);
			}

			const sha256_of_token = sha256(token);

			const rows = pg.querySync (

				"SELECT 1 FROM token"		+
				" WHERE id = $1::text "		+
				" AND token = $2::text "	+
				" AND expiry > NOW()"		+
				" LIMIT 1",
				[
					id,			// 1
					sha256_of_token		// 2
				]
			);

			if (rows.length === 0)
			{
				const error_response = {
					"message"		: "Invalid 'token'",
					"invalid-input"		: xss_safe(token),
					"num-tokens-revoked"	: num_tokens_revoked
				};

				return ERROR (res, 400, error_response);
			}

			pg.querySync (

				"UPDATE token SET revoked = true"	+
				" WHERE id = $1::text"			+
				" AND token = $2::text"			+
				" AND revoked = false"			+
				" AND expiry > NOW()",
				[
					id,				// 1
					sha256_of_token			// 2
				]
			);

			// querySync returns empty object for UPDATE
			num_tokens_revoked += 1;
		}
	}
	else
	{
		// user is a provider

		if (! (token_hashes instanceof Array))
			return ERROR (res, 400, "'token-hashes' must be a valid JSON array");

		const email_domain	= id.split("@")[1];
		const sha1_of_email	= sha1(id);

		const provider_id_hash	= email_domain + "/" + sha1_of_email;

		for (const token_hash of token_hashes)
		{
			if (! is_valid_tokenhash(token_hash))
			{
				const error_response = {
					"message"		: "Invalid 'token-hash'",
					"invalid-input"		: xss_safe(token_hash),
					"num-tokens-revoked"	: num_tokens_revoked
				};

				return ERROR (res, 400, error_response);
			}

			const rows = pg.querySync (

				"SELECT 1 FROM token"			+
				" WHERE token = $1::text"		+
				" AND providers-> $2::text = 'true'"	+
				" AND expiry > NOW()"			+
				" LIMIT 1",
				[
					token_hash,			// 1
					provider_id_hash		// 2
				]
			);

			if (rows.length === 0)
			{
				const error_response = {
					"message"		: "Invalid 'token hash'",
					"invalid-input"		: xss_safe(token_hash),
					"num-tokens-revoked"	: num_tokens_revoked
				};

				return ERROR (res, 400, error_response);
			}

			const provider_false = {};
				provider_false[provider_id_hash] = false;

			pg.querySync (

				"UPDATE token SET"			+
				" providers = providers || $1::jsonb"	+
				" WHERE token = $2::text"		+
				" AND providers-> $3::text = 'true'"	+
				" AND expiry > NOW()",
				[
					JSON.stringify(provider_false),	// 1
					token_hash,			// 2
					provider_id_hash		// 3
				]
			);

			// querySync returns empty object for UPDATE
			num_tokens_revoked += 1;
		}
	}

	const response = {
		"num-tokens-revoked" : num_tokens_revoked
	};

	return SUCCESS (res, response);
});

app.post("/auth/v[1-2]/token/revoke-all", (req, res) => {

	const id		= res.locals.email;
	const body		= res.locals.body;

	if (! body.serial)
		return ERROR (res, 400, "No 'serial' found in the body");

	if (! is_string_safe(body.serial))
		return ERROR (res, 400, "Invalid 'serial'");

	const serial = body.serial.toLowerCase();

	if (! body.fingerprint)
	{
		return ERROR (
			res, 400,
			"No 'fingerprint' found in the body"
		);
	}

	if (! is_string_safe(body.fingerprint,":")) // fingerprint contains ':'
		return ERROR (res, 400, "Invalid 'fingerprint'");

	const fingerprint	= body.fingerprint.toLowerCase();

	const email_domain	= id.split("@")[1];
	const sha1_of_email	= sha1(id);

	const provider_id_hash	= email_domain + "/" + sha1_of_email;

	pool.query (

		"UPDATE token"				+
		" SET revoked = true"			+
		" WHERE id = $1::text"			+
		" AND cert_serial = $2::text"		+
		" AND cert_fingerprint = $3::text"	+
		" AND expiry > NOW()"			+
		" AND revoked = false",
		[
			id,				// 1
			serial,				// 2
			fingerprint			// 3
		],

		(error,results) =>
		{
			if (error)
			{
				return ERROR (
					res, 500, "Internal error!", error
				);
			}

			const response = {
				"num-tokens-revoked" : results.rowCount
			};

			const provider_false = {};
			provider_false[provider_id_hash] = false;

			pool.query (

				"UPDATE token SET"			+
				" providers = providers || $1::jsonb"	+
				" WHERE cert_serial = $2::text"		+
				" AND cert_fingerprint = $3::text"	+
				" AND expiry > NOW()"			+
				" AND revoked = false"			+
				" AND providers-> $4::text = 'true'",
				[
					JSON.stringify(provider_false),	// 1
					serial,				// 2
					fingerprint,			// 3
					provider_id_hash		// 4
				],

				(update_error, update_results) =>
				{
					if (update_error)
					{
						return ERROR (
							res, 500,
							"Internal error!",
							update_error
						);
					}

					response["num-tokens-revoked"] += update_results.rowCount;

					return SUCCESS (res,response);
				}
			);
		}
	);
});

app.post("/auth/v[1-2]/acl/set", (req, res) => {

	const body		= res.locals.body;
	const provider_id	= res.locals.email;

	if (! body.policy)
		return ERROR (res, 400, "No 'policy' found in request");

	if (typeof body.policy !== "string")
		return ERROR (res, 400, "'policy' must be a string");

	const policy		= body.policy.trim();
	const policy_lowercase	= policy.toLowerCase();

	if (
		(policy_lowercase.search(" like ")  >= 0) ||
		(policy_lowercase.search("::regex") >= 0)
	)
	{
		return ERROR (res, 400, "RegEx in 'policy' is not supported");
	}

	const rules = policy.split(";");

	let policy_in_json;

	try
	{
		policy_in_json = rules.map (
			(r) => {
				return (parser.parse(r.trim()));
			}
		);
	}
	catch (x)
	{
		const err = String(x);
		return ERROR (res, 400, "Syntax error in policy. " + err);
	}

	const email_domain	= provider_id.split("@")[1];
	const sha1_of_email	= sha1(provider_id);

	const provider_id_hash	= email_domain + "/" + sha1_of_email;

	const base64policy	= base64(policy);

	pool.query (

		"SELECT 1 FROM policy WHERE id = $1::text LIMIT 1",
		[
			provider_id_hash,	// 1
		],

	(error, results) =>
	{
		if (error)
			return ERROR (res, 500, "Internal error!", error);

		let query;
		let params;

		if (results.rows.length > 0)
		{
			query	= "UPDATE policy"			+
					" SET policy = $1::text,"	+
					" policy_in_json = $2::jsonb,"	+
					" previous_policy = policy,"	+
					" last_updated = NOW(),"	+
					" api_called_from = $3::text"	+
					" WHERE id = $4::text";

			params	= [
				base64policy,				// 1
				JSON.stringify(policy_in_json),		// 2
				req.headers.origin,			// 3
				provider_id_hash			// 4
			];
		}
		else
		{
			query	= "INSERT INTO policy VALUES("	+
					"$1::text,"		+
					"$2::text,"		+
					"$3::jsonb,"		+
					"NULL,"			+
					"NOW(),"		+
					"$4::text"		+
			")";

			params	= [
				provider_id_hash,			// 1
				base64policy,				// 2
				JSON.stringify(policy_in_json),		// 3
				req.headers.origin			// 4
			];
		}

		pool.query (query, params, (error_1, results_1) =>
		{
			if (error_1 || results_1.rowCount === 0)
			{
				return ERROR (
					res, 500,
						"Internal error!",
						error_1
				);
			}

			return SUCCESS (res);
		});
	});
});

app.post("/auth/v[1-2]/acl/append", (req, res) => {

	const body		= res.locals.body;
	const provider_id	= res.locals.email;

	if (! body.policy)
		return ERROR (res, 400, "No 'policy' found in request");

	if (typeof body.policy !== "string")
		return ERROR (res, 400, "'policy' must be a string");

	const policy		= body.policy.trim();
	const policy_lowercase	= policy.toLowerCase();

	if (
		(policy_lowercase.search(" like ")  >= 0) ||
		(policy_lowercase.search("::regex") >= 0)
	)
	{
		return ERROR (res, 400, "RegEx in 'policy' is not supported");
	}

	const rules = policy.split(";");

	let policy_in_json;

	try
	{
		policy_in_json = rules.map (
			(r) => {
				return (parser.parse(r.trim()));
			}
		);
	}
	catch (x)
	{
		const err = String(x);
		return ERROR (res, 400, "Syntax error in policy. " + err);
	}

	const email_domain	= provider_id.split("@")[1];
	const sha1_of_email	= sha1(provider_id);

	const provider_id_hash	= email_domain + "/" + sha1_of_email;

	pool.query (

		"SELECT policy FROM policy WHERE id = $1::text LIMIT 1",
		[
			provider_id_hash	// 1
		],

	(error, results) =>
	{
		if (error)
			return ERROR (res,500,"Internal error!",error);

		let query;
		let params;

		if (results.rows.length === 1)
		{
			const old_policy	= Buffer.from (
							results.rows[0].policy,
							"base64"
						).toString("ascii");

			const new_policy	= old_policy + ";" + policy;
			const new_rules		= new_policy.split(";");

			try
			{
				policy_in_json = new_rules.map (
					(r) => {
						return (parser.parse(r.trim()));
					}
				);
			}
			catch (x)
			{
				const err = String(x);

				return ERROR (
					res, 400,
					"Syntax error in policy. " + err
				);
			}

			const base64policy = Buffer
						.from(new_policy)
						.toString("base64");

			query	= "UPDATE policy"			+
					" SET policy = $1::text,"	+
					" policy_in_json = $2::jsonb,"	+
					" previous_policy = policy,"	+
					" last_updated = NOW(),"	+
					" api_called_from = $3::text"	+
					" WHERE id = $4::text";

			params	= [
					base64policy,			// 1
					JSON.stringify(policy_in_json),	// 2
					req.headers.origin,		// 3
					provider_id_hash		// 4
			];
		}
		else
		{
			const base64policy = Buffer
						.from(policy)
						.toString("base64");

			query	= "INSERT INTO policy VALUES("	+
					"$1::text,"		+
					"$2::text,"		+
					"$3::jsonb,"		+
					"NULL,"			+
					"NOW(),"		+
					"$4::text"		+
			")";

			params	= [
				provider_id_hash,			// 1
				base64policy,				// 2
				JSON.stringify(policy_in_json),		// 3
				req.headers.origin			// 4
			];
		}

		pool.query (query, params, (error_1, results_1) =>
		{
			if (error_1 || results_1.rowCount === 0)
			{
				return ERROR (
					res, 500,
						"Internal error!",
						error_1
				);
			}

			return SUCCESS (res);
		});
	});
});

app.post("/auth/v[1-2]/acl", (req, res) => {

	const provider_id	= res.locals.email;

	const email_domain	= provider_id.split("@")[1];
	const sha1_of_email	= sha1(provider_id);

	const provider_id_hash	= email_domain + "/" + sha1_of_email;

	pool.query (

		"SELECT policy, previous_policy, last_updated, api_called_from"	+
		" FROM policy"							+
		" WHERE id = $1::text "						+
		" LIMIT 1",
		[
			provider_id_hash			// 1
		],

	(error, results) =>
	{
		if (error)
			return ERROR (res, 500, "Internal error!", error);

		if (results.rows.length === 0)
			return ERROR (res, 400, "No policies set yet!");

		const policy	= Buffer
					.from(results.rows[0].policy,"base64")
					.toString("ascii")
					.split(";");

		let previous_policy = [];

		if (results.rows[0].previous_policy)
		{
			previous_policy = Buffer
						.from(results.rows[0].previous_policy,"base64")
						.toString("ascii")
						.split(";");
		}

		const response = {
			"policy"		: policy,
			"previous-policy"	: previous_policy,
			"last-updated"		: results.rows[0].last_updated,
			"api-called-from"	: results.rows[0].api_called_from
		};

		return SUCCESS (res,response);
	});
});

app.post("/auth/v[1-2]/acl/revert", (req, res) => {

	const provider_id	= res.locals.email;

	const email_domain	= provider_id.split("@")[1];
	const sha1_of_email	= sha1(provider_id);

	const provider_id_hash	= email_domain + "/" + sha1_of_email;

	pool.query (

		"SELECT previous_policy FROM policy"	+
		" WHERE id = $1::text"			+
		" AND previous_policy IS NOT NULL"	+
		" LIMIT 1",
		[
			provider_id_hash		// 1
		],

	(error, results) =>
	{
		if (error)
			return ERROR (res, 500, "Internal error!", error);

		if (results.rows.length === 0)
			return ERROR (res, 400, "No previous policies found!");

		const previous_policy = Buffer
					.from(results.rows[0].previous_policy,"base64")
					.toString("ascii")
					.split(";");

		let policy_in_json;

		try
		{
			policy_in_json = previous_policy.map (
				(r) => {
					return (parser.parse(r.trim()));
				}
			);
		}
		catch (x)
		{
			const err = String(x);

			return ERROR (
				res, 400,
				"Syntax error in previous-policy. " + err
			);
		}

		const query	= "UPDATE policy"			+
					" SET policy = previous_policy,"+
					" policy_in_json = $1::jsonb,"	+
					" previous_policy = NULL,"	+
					" last_updated = NOW(),"	+
					" api_called_from = $2::text"	+
					" WHERE id = $3::text";

		const params	= [
				JSON.stringify(policy_in_json),		// 1
				req.headers.origin,			// 2
				provider_id_hash			// 3
		];

		pool.query (query, params, (error_1, results_1) =>
		{
			if (error_1 || results_1.rowCount === 0)
			{
				return ERROR (
					res, 500,
						"Internal error!",
						error_1
				);
			}

			return SUCCESS (res);
		});
	});
});

app.post("/auth/v[1-2]/audit/tokens", (req, res) => {

	const id		= res.locals.email;
	const body		= res.locals.body;

	if (! body.hours)
		return ERROR (res, 400, "No 'hours' found in the body");

	const hours = parseInt (body.hours,10);

	// 5 yrs max
	if (isNaN(hours) || hours < 1 || hours > 43800) {
		return ERROR (res, 400, "'hours' must be a positive number");
	}

	const as_consumer = [];
	const as_provider = [];

	pool.query (

		"SELECT issued_at,expiry,request,cert_serial,"	+
		" cert_fingerprint,introspected,revoked,"	+
		" expiry < NOW() as expired,geoip,paid,"	+
		" api_called_from"				+
		" FROM token"					+
		" WHERE id = $1::text"				+
		" AND issued_at >= (NOW() - $2::interval)"	+
		" ORDER BY issued_at DESC",
		[
			id,					// 1
			hours + " hours"			// 2
		],

	(error, results) =>
	{
		if (error)
			return ERROR (res, 500, "Internal error!", error);

		for (const row of results.rows)
		{
			as_consumer.push ({
				"token-issued-at"		: row.issued_at,
				"introspected"			: row.introspected,
				"revoked"			: row.revoked,
				"expiry"			: row.expiry,
				"expired"			: row.expired,
				"certificate-serial-number"	: row.cert_serial,
				"certificate-fingerprint"	: row.cert_fingerprint,
				"request"			: row.request,
				"geoip"				: row.geoip,
				"paid"				: row.paid,
				"api-called-from"		: row.api_called_from
			});
		}

		const email_domain	= id.split("@")[1];
		const sha1_of_email	= sha1(id);

		const provider_id_hash	= email_domain + "/" + sha1_of_email;

		pool.query (

			"SELECT id,token,issued_at,expiry,request,"	+
			" cert_serial,cert_fingerprint,"		+
			" revoked,introspected,"			+
			" providers-> $1::text"				+
			" AS is_valid_token_for_provider,"		+
			" expiry < NOW() as expired,geoip,paid,"	+
			" api_called_from"				+
			" FROM token"					+
			" WHERE providers-> $1::text"			+
			" IS NOT NULL"					+
			" AND issued_at >= (NOW() - $2::interval)"	+
			" ORDER BY issued_at DESC",
			[
				provider_id_hash,			// 1
				hours + " hours"			// 2
			],

		(error, results) =>
		{
			if (error)
			{
				return ERROR (
					res, 500, "Internal error!", error
				);
			}

			for (const row of results.rows)
			{
				const revoked = (
					row.revoked || (! row.is_valid_token_for_provider)
				);

				/* return only resource IDs belonging to provider
				   who requested audit */

				let filtered_request = [];

				for (const r of row.request)
				{
					const split		= r.id.split("/");

					const email_domain	= split[0].toLowerCase();
					const sha1_of_email	= split[1].toLowerCase();

					const provider		= email_domain + "/" + sha1_of_email;

					if (provider === provider_id_hash)
						filtered_request.push(r);
				}

				as_provider.push ({
					"consumer"			: row.id,
					"token-hash"			: row.token,
					"token-issued-at"		: row.issued_at,
					"introspected"			: row.introspected,
					"revoked"			: revoked,
					"expiry"			: row.expiry,
					"expired"			: row.expired,
					"certificate-serial-number"	: row.cert_serial,
					"certificate-fingerprint"	: row.cert_fingerprint,
					"request"			: filtered_request,
					"geoip"				: row.geoip,
					"paid"				: row.paid,
					"api-called-from"		: row.api_called_from
				});
			}

			const response = {
				"as-consumer"	: as_consumer,
				"as-provider"	: as_provider,
			};

			return SUCCESS (res,response);
		});
	});
});

app.post("/auth/v[1-2]/group/add", (req, res) => {

	const body		= res.locals.body;
	const provider_id	= res.locals.email;

	if (! body.consumer)
		return ERROR (res, 400, "No 'consumer' found in the body");

	if (! is_valid_email(body.consumer))
		return ERROR (res, 400, "'consumer' must be an e-mail");

	const consumer_id = body.consumer.toLowerCase();

	if (! body.group)
		return ERROR (res, 400, "No 'group' found in the body");

	if (! is_string_safe (body.group))
		return ERROR (res, 400, "Invalid 'group'");

	const group = body.group.toLowerCase();

	if (! body["valid-till"])
		return ERROR (res, 400, "No 'valid-till' found in the body");

	const valid_till = parseInt(body["valid-till"],10);

	// 1 year max
	if (isNaN(valid_till) || valid_till < 1 || valid_till > 8760)
	{
		return ERROR (
			res, 400, "'valid-till' must be a positive number"
		);
	}

	const email_domain	= provider_id.split("@")[1];
	const sha1_of_email	= sha1(provider_id);

	const provider_id_hash	= email_domain + "/" + sha1_of_email;

	pool.query (

		"INSERT INTO groups"			+
		" VALUES ($1::text, $2::text, $3::text, NOW() + $4::interval)",
		[
			provider_id_hash,		// 1
			consumer_id,			// 2
			group,				// 3
			valid_till + " hours"		// 4
		],

	(error, results) =>
	{
		if (error || results.rowCount === 0)
			return ERROR (res, 500, "Internal error!", error);

		return SUCCESS (res);
	});
});

app.post("/auth/v[1-2]/group/list", (req, res) => {

	const body		= res.locals.body;
	const provider_id	= res.locals.email;

	if (body.group)
	{
		if (! is_string_safe (body.group))
			return ERROR (res, 400, "Invalid 'group'");
	}

	const group		= body.group ? body.group.toLowerCase() : null;

	const email_domain	= provider_id.split("@")[1];
	const sha1_of_email	= sha1(provider_id);

	const provider_id_hash	= email_domain + "/" + sha1_of_email;

	const response = [];

	if (group)
	{
		pool.query (

			"SELECT consumer, valid_till FROM groups"	+
			" WHERE id = $1::text"				+
			" AND group_name = $2::text"			+
			" AND valid_till > NOW()",
			[
				provider_id_hash,			// 1
				group					// 2
			],

		(error, results) =>
		{
			if (error)
			{
				return ERROR (
					res, 500, "Internal error!", error
				);
			}

			for (const row of results.rows)
			{
				response.push ({
					"consumer"	: row.consumer,
					"valid-till"	: row.valid_till
				});
			}

			return SUCCESS (res,response);
		});
	}
	else
	{
		pool.query (

			"SELECT consumer,group_name,valid_till"	+
			" FROM groups"				+
			" WHERE id = $1::text"			+
			" AND valid_till > NOW()",
			[
				provider_id_hash		// 1
			],

		(error, results) =>
		{
			if (error)
			{
				return ERROR (
					res, 500, "Internal error!", error
				);
			}

			for (const row of results.rows)
			{
				response.push ({
					"consumer"	: row.consumer,
					"group"		: row.group_name,
					"valid-till"	: row.valid_till
				});
			}

			return SUCCESS (res,response);
		});
	}
});

app.post("/auth/v[1-2]/group/delete", (req, res) => {

	const body		= res.locals.body;
	const provider_id	= res.locals.email;

	if (! body.consumer)
		return ERROR (res, 400, "No 'consumer' found in the body");

	if (body.consumer !== "*")
	{
		if (! is_valid_email(body.consumer))
		{
			return ERROR (
				res, 400, "'consumer' must be an e-mail"
			);
		}
	}

	const consumer_id = body.consumer.toLowerCase();

	if (! body.group)
		return ERROR (res, 400, "No 'group' found in the body");

	if (! is_string_safe (body.group))
		return ERROR (res, 400, "Invalid 'group'");

	const group		= body.group.toLowerCase();

	const email_domain	= provider_id.split("@")[1];
	const sha1_of_email	= sha1(provider_id);

	const provider_id_hash	= email_domain + "/" + sha1_of_email;

	let query	= "UPDATE groups SET"					+
				" valid_till = (NOW() - interval '1 seconds')"	+
				" WHERE id = $1::text"				+
				" AND group_name = $2::text"			+
				" AND valid_till > NOW()";

	const params	= [
				provider_id_hash,			// 1
				group					// 2
	];

	if (consumer_id !== "*")
	{
		query	+= " AND consumer = $3::text";
		params.push(consumer_id);				// 3
	}

	pool.query (query, params, (error, results) =>
	{
		if (error)
			return ERROR (res, 500, "Internal error!", error);

		if (consumer_id !== "*" && results.rowCount === 0)
		{
			return ERROR (
				res, 400, "Consumer not found in the group"
			);
		}

		const response = {
			"num-consumers-deleted"	: results.rowCount
		};

		return SUCCESS (res,response);
	});
});

app.post("/auth/v[1-2]/certificate-info", (req, res) => {

	const cert	= res.locals.cert;

	const response	= {
		"id"			: res.locals.email,
		"certificate-class"	: res.locals.cert_class,
		"serial"		: cert.serialNumber.toLowerCase(),
		"fingerprint"		: cert.fingerprint.toLowerCase(),
	};

	return SUCCESS (res,response);
});

/* --- Marketplace APIs --- */

app.post("/marketplace/v[1-2]/credit/info", (req, res) => {

	const id		= res.locals.email;
	const cert		= res.locals.cert;
	const cert_class	= res.locals.cert_class;

	let query		= "SELECT * FROM credit"			+
					" WHERE id = $1::text"			+
					" AND cert_fingerprint = $2::text"	+
					" AND cert_serial = $3::text";

	let serial 	= "*";
	let fingerprint	= "*";

	if (cert_class < 3) // get the real serial and fingerprint
	{
		serial		= cert.serialNumber.toLowerCase();
		fingerprint	= cert.fingerprint.toLowerCase();

		query += " LIMIT 1";
	}

	pool.query (
		query,
		[
			id,
			fingerprint,
			serial
		],

	(error, results) =>
	{
		if (error)
			return ERROR (res, 500, "Internal error!", error);

		if (results.rowCount === 0)
			return ERROR (res, 400, "No credits available");

		const response = {};

		if (cert_class < 3)
		{
			response.credits 		= results.rows[0].amount;
			response["last-updated"]	= results.rows[0].last_updated;

			return SUCCESS (res, response);
		}

		pool.query ("SELECT * FROM credit WHERE id = $1::text", [id],
		(other_error, other_results) =>
		{
			if (other_error)
				return ERROR (res, 500, "Internal error!", other_error);

			response["other-credits"] = [];

			for (const row of other_results.rows)
			{
				if (row.cert_serial === "*" && row.cert_fingerprint === "*")
				{
					response.credits 		= results.rows[0].amount;
					response["last-updated"]	= results.rows[0].last_updated;
				}
				else
				{
					response["other-credits"].push ({
						"serial"	: row.cert_serial,
						"fingerprint"	: row.cert_fingerprint,
						"credits"	: row.amount,
						"last-updated"  : row.last_updated
					});
				}
			}

			return SUCCESS (res, response);
		});
	});
});

app.post("/marketplace/v[1-2]/credit/topup", (req, res) => {

	const id		= res.locals.email;
	const body		= res.locals.body;
	const cert		= res.locals.cert;
	const cert_class	= res.locals.cert_class;

	if (! body.amount)
		return ERROR (res, 400, "No 'amount' found in the body");

	const amount = parseFloat(body.amount);

	if (isNaN(amount) || amount < 0 || amount > 1000)
		return ERROR (res, 400, "'amount' must be a positive number <= 1000");

	let serial;
	let fingerprint;

	if (cert_class < 3)
	{
		if (body.fingerprint || body.serial)
		{
			return ERROR (
				res, 400,
				"'fingerprint' or 'serial' can only be"	+
				" provided when using a class-3 "	+
				" or above certificate"
			);
		}

		serial		= cert.serialNumber.toLowerCase();
		fingerprint	= cert.fingerprint.toLowerCase();
	}
	else
	{
		/*
			For a class-3 user, by default, the topup amount
			is not associated with serial or fingerprint.

			i.e. denoted by "*"
			unless 'serial' and 'fingerprint' is provided in body.
		*/

		serial		= "*";
		fingerprint 	= "*";

		if (body.serial && body.fingerprint)
		{
			if (! is_string_safe(body.serial))
				return ERROR (res, 400, "Invalid 'serial'");

			if (! is_string_safe(body.fingerprint,":")) // fingerprint contains ':'
				return ERROR (res, 400, "Invalid 'fingerprint'");

			serial		= body.serial.toLowerCase();
			fingerprint	= body.fingerprint.toLowerCase();
		}
	}

	const now		= Math.floor (Date.now() / 1000);
	const expire		= now + 1800; // after 30 mins

	const success_url	= "https://" + SERVER_NAME + "/marketplace/topup-success";

	const first_name	= cert.subject.GN || "Unknown";
	const last_name		= cert.subject.SN || "unknown";

	const full_name		= first_name + " " + last_name;

	const post_body = {

		"type"			: "link",
		"amount"		: amount * 100.0,
		"description"		: "DataSetu credits topup for : " + id,
		"view_less"		: 1,
		"currency"		: "INR",
		"expire_by"		: expire,
		"email_notify"		: 0,
		"callback_url"		: success_url,
		"callback_method"	: "get",

		"customer"		: {
			"email"		: id,
			"name"		: full_name
		},
	};

	const options = {
		url	: rzpay_url,
		headers	: {"Content-Type": "application/json"},
		json	: true,
		body	: post_body
	};

	http_request.post(options, (error, response, body) => {

		if (error)
		{
			return ERROR (
				res, 500, "Payment failed", error
			);
		}

		if (response.statusCode !== 200)
		{
			return ERROR (
				res, 500,
				"Payment failed. Invalid status from RazorPay",
				response
			);
		}

		if (! body.short_url)
		{
			return ERROR (
				res, 500,
				"Payment failed. RazorPay did send payment url",
				body
			);
		}

		const link		= { link : body.short_url };
		const invoice_number	= body.id;

		const query = "INSERT INTO topup_transaction"		+
					" VALUES ("			+
						"$1::text,"		+
						"$2::text,"		+
						"$3::text,"		+
						"$4::int,"		+
						"to_timestamp($5::int),"+
						"$6::text,"		+
						"false,"		+
						"'{}'::jsonb"		+
					")";

		const params = [
				id,					// 1
				serial,					// 2
				fingerprint,				// 3
				amount,					// 4
				now,					// 5
				invoice_number				// 6
		];

		pool.query (query, params, (insert_error, insert_results) =>
		{
			if (insert_error || insert_results.rowCount === 0)
			{
				return ERROR (
					res, 500, "Internal error!", insert_error
				);
			}

			return SUCCESS (res, link);
		});
	});
});

app.get("/marketplace/topup-success", (req, res) => {

	const cert		= res.locals.cert;
	const cert_class	= res.locals.cert_class;

	const invoice_number	= req.query.razorpay_invoice_id;
	const invoice_status	= req.query.razorpay_invoice_status;

	if (! invoice_number || invoice_status !== "paid")
	{
		const error_response = {
			"message"	: "Payment was not completed for invoice",
			"invalid-input"	: {
				invoice					: xss_safe(invoice_number),
				time					: new Date(),
				cert_serial_used_for_payment		: cert.serialNumber.toLowerCase(),
				cert_fingerprint_used_for_payment	: cert.fingerprint.toLowerCase(),
				cert_class_used_for_payment		: cert_class
			}
		};

		const response_mid =
			"<script>"					+
				"jsonViewer.showJSON("			+
					JSON.stringify(error_response)	+
				");"					+
			"</script>";

		const page = topup_failure_1 + response_mid + topup_failure_2;

		res.setHeader("Content-Type", "text/html");
		res.status(400).end(page);

		return;
	}

	const payload = [
			req.query.razorpay_invoice_id,
			req.query.razorpay_invoice_receipt,
			req.query.razorpay_invoice_status,
			req.query.razorpay_payment_id
	].join("|");

	const expected_signature = crypto
					.createHmac("sha256",rzpay_key_secret)
					.update(payload)
					.digest("hex");

	if (req.query.razorpay_signature !== expected_signature)
	{
		const error_response = {
			"message"	: "Invalid razorpay signature",
			"invalid-input"	: {
				invoice					: xss_safe(invoice_number),
				time					: new Date(),
				razorpay_signature			: xss_safe(req.query.razorpay_signature),
				razorpay_invoice_id			: xss_safe(req.query.razorpay_invoice_id),
				razorpay_invoice_receipt		: xss_safe(req.query.razorpay_invoice_receipt),
				razorpay_invoice_status			: xss_safe(req.query.razorpay_invoice_status),
				razorpay_payment_id			: xss_safe(req.query.razorpay_payment_id),
				cert_serial_used_for_payment		: cert.serialNumber.toLowerCase(),
				cert_fingerprint_used_for_payment	: cert.fingerprint.toLowerCase(),
				cert_class_used_for_payment		: cert_class
			}
		};

		const response_mid =
			"<script>"					+
				"jsonViewer.showJSON("			+
					JSON.stringify(error_response)	+
				");"					+
			"</script>";

		const page = topup_failure_1 + response_mid + topup_failure_2;

		res.setHeader("Content-Type", "text/html");
		res.status(400).end(page);

		return;
	}

	const payment_details = {};

	for (const key in req.query)
		payment_details[key] = req.query[key];

	payment_details.origin	= req.headers.origin;
	payment_details.referer = req.headers.referrer;

	const query	= "SELECT"					+
				" update_credit($1::text,$2::jsonb)"	+
				" AS details";

	const params	= [invoice_number, payment_details];

	pool.query(query, params, (error, results) =>
	{
		if (error || results.rowCount === 0)
		{
			log ("red",error);

			const error_response = {
				"message"	: "Internal error in topup confirmation",
				"invalid-input"	: {
					invoice					: xss_safe(invoice_number),
					time					: new Date(),
					cert_serial_used_for_payment		: cert.serialNumber.toLowerCase(),
					cert_fingerprint_used_for_payment	: cert.fingerprint.toLowerCase(),
					cert_class_used_for_payment		: cert_class
				}
			};

			const response_mid =
				"<script>"					+
					"jsonViewer.showJSON("			+
						JSON.stringify(error_response)	+
					");"					+
				"</script>";

			const page = topup_failure_1 + response_mid + topup_failure_2;

			res.setHeader("Content-Type", "text/html");
			res.status(400).end(page);

			return;
		}

		const details = results.rows[0].details;

		if (! details || Object.keys(details).length === 0)
		{
			const error_response = {
				"message"	: "Invalid invoice number",
				"invalid-input"	: {
					invoice					: xss_safe(invoice_number),
					time					: new Date(),
					cert_serial_used_for_payment		: cert.serialNumber.toLowerCase(),
					cert_fingerprint_used_for_payment	: cert.fingerprint.toLowerCase(),
					cert_class_used_for_payment		: cert_class
				}
			};

			const response_mid =
				"<script>"					+
					"jsonViewer.showJSON("			+
						JSON.stringify(error_response)	+
					");"					+
				"</script>";

			const page = topup_failure_1 + response_mid + topup_failure_2;

			res.setHeader("Content-Type", "text/html");
			res.status(400).end(page);

			return;
		}

		details.cert_serial_used_for_payment		= cert.serialNumber.toLowerCase();
		details.cert_fingerprint_used_for_payment	= cert.fingerprint.toLowerCase();
		details.cert_class_used_for_payment		= cert_class;

		const response = JSON.parse(JSON.stringify(req.query));

		for (const key in details)
		{
			response[key] = details[key];
		}

		const response_mid =
				"<script>"					+
					"jsonViewer.showJSON("			+
						JSON.stringify(response)	+
					");"					+
				"</script>";

		const page = topup_success_1 + response_mid + topup_success_2;

		res.setHeader("Content-Type", "text/html");
		res.status(200).end(page);
	});
});

app.post("/marketplace/v[1-2]/confirm-payment", (req, res) => {

	const id	    	= res.locals.email;
	const body	    	= res.locals.body;
	const cert		= res.locals.cert;
	const cert_class	= res.locals.cert_class;

	if (! body.token)
		return ERROR (res, 400, "No 'token' found in the body");

	if (! is_valid_token(body.token,id))
		return ERROR (res, 400, "Invalid 'token'");

	const token		= body.token;
	const sha256_of_token	= sha256(token);

	const cert_serial	= cert.serialNumber.toLowerCase();
	const cert_fingerprint	= cert.fingerprint.toLowerCase();

	pool.query (

		"SELECT (payment_info->>'amount') AS amount"	+
		" FROM token"					+
		" WHERE id = $1::text"				+
		" AND token = $2::text"				+
		" AND cert_serial = $3::text"			+
		" AND cert_fingerprint = $4::text"		+
		" AND (payment_info->>'amount')::int > 0"	+
		" AND paid = false"				+
		" AND expiry > NOW()"				+
		" LIMIT 1",
		[
			id,					// 1
			sha256_of_token,			// 2
			cert_serial,				// 3
			cert_fingerprint			// 4
		],
		(error, results) =>
		{

			if (results.rowCount === 0)
				return ERROR (res, 400, "Invalid 'token'");

			const amount	= results.rows[0].amount;

			/*
				The below serial and fingerprint variables
				are from the "credit" and "topup_transaction"
				table, and may not be the "real" certificate's
				serial and fingerprint.
			*/

			let serial	= cert_serial;
			let fingerprint	= cert_fingerprint;

			if (cert_class > 2)
			{
				serial		= "*";
				fingerprint	= "*";
			}

			const query = "SELECT confirm_payment("		+
					"$1::text,"			+
					"$2::numeric,"			+
					"$3::text,"			+
					"$4::text,"			+
					"$5::text,"			+
					"$6::text"			+
				") AS payment_confirmed";

			const params	= [
				id,					// 1
				amount,					// 2
				cert_serial,				// 3
				cert_fingerprint,			// 4
				serial,					// 5
				fingerprint,				// 6
			];

			pool.query(query, params, (function_error, function_results) =>
			{
				if (function_error)
					return ERROR (res, 500, "Internal error!", function_error);

				if (function_results.rowCount === 0)
					return ERROR (res, 402, "Not enough balance!");

				if (! function_results.rows[0].payment_confirmed)
					return ERROR (res, 400, "Payment could not be confirmed");

				return SUCCESS (res);
			});
		}
	);
});

app.post("/marketplace/v[1-2]/audit/credits", (req, res) => {

	const id	    	= res.locals.email;
	const body	    	= res.locals.body;
	const cert		= res.locals.cert;
	const cert_class	= res.locals.cert_class;

	if (! body.hours)
		return ERROR (res, 400, "No 'hours' found in the body");

	const hours = parseInt (body.hours,10);

	// 5 yrs max
	if (isNaN(hours) || hours < 1 || hours > 43800) {
		return ERROR (res, 400, "'hours' must be a positive number");
	}

	let serial;
	let fingerprint;

	if (cert_class > 2)
	{
		serial		= "*";
		fingerprint	= "*";
	}
	else
	{
		serial		= cert.serialNumber.toLowerCase();
		fingerprint	= cert.fingerprint.toLowerCase();
	}

	pool.query (

		"SELECT amount,time,invoice_number"		+
			" FROM topup_transaction"		+
			" WHERE id = $1::text"			+
			" AND cert_serial = $2::text" 		+
			" AND cert_fingerprint = $3::text"	+
			" AND paid = true"	 		+
			" AND time >= (NOW() - $4::interval) ",
		[
			id,
			serial,
			fingerprint,
			hours + " hours"
		],
	(error, results) =>
	{
		if (error)
			return ERROR (res, 500, "Internal error!", error);

		const as_consumer		= [];
		const as_provider		= [];
		const other_transactions	= [];

		for (const row of results.rows)
		{
			as_consumer.push ({
				"transaction"		: "topup",
				"amount"		: row.amount,
				"time"			: row.time,
				"invoice-number"	: row.invoice_number,
			});
		}

		if (cert_class < 3)
		{
			const response = {
				"as-consumer" : as_consumer
			};

			return SUCCESS (res, response);
		}

		pool.query (
			"SELECT * FROM topup_transaction"	+
			" WHERE id = $1::text"			+
			" AND paid = true"			+
			" AND time >= (NOW() - $2::interval)"	+
			" AND cert_serial != '*'"		+
			" AND cert_fingerprint != '*'",
			[
				id,
				hours + " hours"
			],

		(error_1, results_1) =>
		{
			if (error_1)
				return ERROR (res, 500, "Internal error!", error_1);

			for (const row of results_1.rows)
			{
				other_transactions.push ({
					"transaction"	: "topup",
					"amount"	: row.amount,
					"time"		: row.time,
					"serial"	: row.cert_serial,
					"fingerprint"	: row.cert_fingerprint
				});
			}

			const email_domain	= id.split("@")[0].toLowerCase();
			const sha1_of_email	= sha1(id);

			const provider_id_hash	= email_domain + "/" + sha1_of_email;

			pool.query (
				"SELECT paid_at,"						+
					" (payment_info ->>'providers')::jsonb->> $1::text"	+
					" AS money"						+
					" FROM token"						+
					" WHERE amount > 0.0"					+
					" AND > (payment_info->>'amount')"			+
					" AND paid = true",
				[
					provider_id_hash
				],
			(error_2, results_2) =>
			{
				if (error_2)
					return ERROR (res, 500, "Internal error!", error_2);

				for (const row of results_2.rows)
				{
					as_provider.push ({
						"transaction"	: "received",
						"amount"	: row.money,
						"time"		: row.paid_at
					});
				}

				const response = {
					"as-consumer"		: as_consumer,
					"as-provider"		: as_consumer,
					"other-transactions"	: other_transactions
				};

				return SUCCESS(res, response);
			});
		});
	});
});

app.post("/marketplace/v[1-2]/credit/transfer", (req, res) => {

	const id	= res.locals.email;
	const body    	= res.locals.body;

	if (! body["from-fingerprint"])
		return ERROR (res, 400, "'from-fingerprint' field not found in body");

	if (! is_string_safe(body["from-fingerprint"],":"))
		return ERROR (res, 400, "Invalid 'from-fingerprint'");

	const from_fingerprint = body["from-fingerprint"];

	if (! body["to-fingerprint"])
		return ERROR (res, 400, "'to-fingerprint' field not found in body");

	if (! is_string_safe(body["to-fingerprint"],":"))
		return ERROR (res, 400, "Invalid 'to-fingerprint'");

	const to_fingerprint = body["to-fingerprint"];

	if (from_fingerprint === to_fingerprint)
		return ERROR (res, 400, "'from-fingerprint' and 'to-fingerprint' cannot be same");

	if (! body["to-serial"])
		return ERROR (res, 400, "'to-serial' field not found in body");

	if (! is_string_safe(body["to-serial"]))
		return ERROR (res, 400, "Invalid 'to-serial'");

	const to_serial = body.to["to-serial"];

	if (! body.amount)
		return ERROR (res, 400, "'amount' field not found in body");

	const amount = parseFloat(body.amount, 10);

	if (isNaN(amount) || amount < 0 || amount > 1000)
		return ERROR (res, 400, "'amount' is not a valid number");

	pool.query (

		"SELECT amount FROM credit"	+
		" WHERE id = $1::text" 		+
		" AND cert_serial = $2::text"	+
		" AND amount >=  $3::numeric",
		[
			id,
			from_fingerprint,
			amount
		],
		(error, results) =>
		{
			if (error)
			{
				return ERROR (
					res, 500,
					"Internal error!", error
				);
			}

			if (results.rowCount === 0)
			{
				return ERROR (
					res, 400,
					"Not enough balance in 'from-fingerprint'"
				);
			}

			pool.query (
				"SELECT transfer_credits("	+
					"$1::text,"		+
					"$1::numeric,"		+
					"$1::text,"		+
					"$1::text"		+
					"$1::text"		+
				") as credits_transfered",
				[
					id,
					amount,
					from_fingerprint,
					to_fingerprint,
					to_serial
				],
			(error_1, results_1) =>
			{
				if (error_1 || results_1.rowCount === 0)
				{
					return ERROR (
						res, 500,
						"Internal error!", error_1
					);
				}

				if (! results_1.credits_transfered)
				{
					return ERROR (
						res, 400,
						"Could not transfer credits"
					);
				}

				return SUCCESS (res);
			});
		}
	);
});

/* --- Invalid requests --- */

app.all("/*", (req, res) => {

	const doc = " Please visit <" + DOCUMENTATION_LINK + "> for documentation";

	if (req.method === "POST")
	{
		return ERROR (res, 404, "No such API." + doc );
	}
	else if (req.method === "GET")
	{
		if (! SERVE_HTML(req,res))
		{
			const path = req.url.split("?")[0];

			if (MIN_CERT_CLASS_REQUIRED[path])
				return ERROR (res, 405, "Method must be POST." + doc);
			else
				return ERROR (res, 404, "Page not found." + doc);
		}
	}
	else
	{
		return ERROR (res, 405, "Method must be POST." + doc);
	}
});

app.on("error", () => {
	/* nothing */
});

/* --- The main application --- */

if (! is_openbsd)
{
	// ======================== START preload code for chroot =============

	const _tmp = ["x can y z"].map (
		(r) => {
			return (parser.parse(r.trim()));
		}
	);

	evaluator.evaluate(_tmp, {});

	dns.lookup("google.com", {all:true},
		(error) => {
			if (error)
				log("yellow","DNS to google.com failed ");
		}
	);

	// ======================== END preload code for chroot ===============
}

function drop_worker_privileges()
{
	for (const k in password)
	{
		password[k] = null;
		delete password[k];	// forget all passwords
	}

	if (is_openbsd)
	{
		if (EUID === 0)
		{
			process.setgid("_aaa");
			process.setuid("_aaa");
		}

		unveil("/usr/lib",			"r" );
		unveil("/usr/libexec/ld.so",		"r" );
		unveil(__dirname + "/node_modules",	"r" );
		unveil(__dirname + "/node-aperture",	"r" );

		unveil();

		pledge.init ("error stdio tty prot_exec inet rpath dns recvfd");
	}
	else
	{
		if (EUID === 0)
		{
			process.setgid("_aaa");
			chroot("/home/datasetu-auth-server","_aaa");
			process.chdir ("/");
		}
	}

	assert (has_started_serving_apis === false);
}

if (cluster.isMaster)
{
	if (is_openbsd)
	{
		unveil("/usr/local/bin/node",	"x");
		unveil("/usr/lib",		"r");
		unveil("/usr/libexec/ld.so",	"r");

		unveil();

		pledge.init (
			"error stdio tty prot_exec inet rpath dns recvfd " +
			"sendfd exec proc"
		);
	}

	log("yellow","Master started with pid " + process.pid);

	const ALL_END_POINTS = Object.keys(MIN_CERT_CLASS_REQUIRED).sort();

	for (const e of ALL_END_POINTS)
		statistics.api.count[e] = 0;

	statistics.start_time = Math.floor (Date.now() / 1000);

	for (let i = 0; i < NUM_CPUS; i++) {
		cluster.fork();
	}

	if (LAUNCH_ADMIN_PANEL)
	{
		cluster.on ("fork", (worker) => {
			worker.on ("message", (endpoint) => {

				if (ALL_END_POINTS.indexOf(endpoint) === -1)
					endpoint = "invalid-api";

				statistics.api.count[endpoint] += 1;
			});
		});
	}

	cluster.on ("exit", (worker) => {

		log("red","Worker " + worker.process.pid + " died.");

		cluster.fork();
	});

	let stats_app; 

	if (LAUNCH_ADMIN_PANEL)
	{
		stats_app = express();

		stats_app.use(compression());
		stats_app.use(bodyParser.raw({type:"*/*"}));
	}

	if (is_openbsd) // drop "rpath"
	{
		pledge.init (
			"error stdio tty prot_exec inet dns recvfd " +
			"sendfd exec proc"
		);
	}

	if (LAUNCH_ADMIN_PANEL)
	{
		https.createServer(https_options,stats_app).listen(8443,"127.0.0.1");
		stats_app.all("/*",show_statistics);
	}
}
else
{
	https.createServer(https_options,app).listen(443,"0.0.0.0");

	drop_worker_privileges();

	log("green","Worker started with pid " + process.pid);
}

// EOF
