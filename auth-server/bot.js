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
const chroot			= require("chroot");
const Slimbot			= require('slimbot');

const telegram_apikey		= fs.readFileSync ("telegram.apikey","ascii").trim();
const slimbot			= new Slimbot(telegram_apikey);

const EUID			= process.geteuid();
const is_openbsd		= os.type() === "OpenBSD";
const pledge			= is_openbsd ? require("node-pledge")	: null;
const unveil			= is_openbsd ? require("openbsd-unveil"): null;

if (is_openbsd)
{
	if (EUID === 0)
	{
		process.setgid("_bot");
		process.setuid("_bot");
	}

	unveil("/usr/lib",			"r" );
	unveil("/usr/libexec/ld.so",		"r" );
	unveil(__dirname + "/node_modules",	"r" );

	unveil();

	pledge.init ("error stdio tty prot_exec inet dns");
}
else
{
	if (EUID === 0)
	{
		process.setgid("_bot");
		chroot("/home/datasetu-auth-server","_bot");
		process.chdir ("/");
	}
}

slimbot.on('message', message => {
	slimbot.sendMessage(message.chat.id, 'Message received');
});

// Call API

slimbot.startPolling();
