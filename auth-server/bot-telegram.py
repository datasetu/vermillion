import sys
import time
import json
import telepot

telegram_apikey = open("telegram.apikey").read().strip()

bot = telepot.Bot(telegram_apikey)

def on_chat(msg):
	content_type, chat_type, chat_id = telepot.glance(msg)
	print('Chat Message:', content_type, chat_type, chat_id)

	if content_type == 'text':
		# TODO update the chat id of user
		print(msg)
		pass	

def on_callback_query (msg):
	token_hash	= None
	chat_id		= bot.sendMessage(msg["message"]["chat"]["id"], "ok")

	print ("Got msg : ",msg)

	try:
		token_hash = msg.split("#")[1]
	except:
		pass

	if not token_hash:
		bot.sendMessage(chat_id, "invalid message")
	else:
		bot.sendMessage(chat_id, "ok for : " + token_hash)

bot.message_loop (
	{
		'chat'			: on_chat,
		'callback_query'	: on_callback_query
	},
	run_forever = True
)

print('Listening ...')
