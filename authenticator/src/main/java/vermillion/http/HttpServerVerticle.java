/*Copyright 2018, Robert Bosch Centre for Cyber Physical Systems, Indian Institute of Science

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package vermillion.http;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.google.common.hash.Hashing;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import vermillion.database.DbService;



/**
 * <h1>IUDX API Server</h1> An Open Source implementation of India Urban Data
 * Exchange (IUDX) platform APIs using Vert.x, an event driven and non-blocking
 * high performance reactive framework, for enabling seamless data exchange in
 * Smart Cities.
 * 
 * @author Robert Bosch Centre for Cyber-Physical Systems (iudx <at> rbccps <dot> org)
 * @version 1.0.0
 */

public class HttpServerVerticle extends AbstractVerticle implements  Handler<HttpServerRequest>
{
	public	final static Logger logger = LoggerFactory.getLogger(HttpServerVerticle.class);
	
	
	public	String 		schema;
	public	String 		message;
	
	//Service proxies
	public	DbService 			dbService;
	
	//Pool to check login info
	public	Map<String, String>	pool;
	
	@Override
	public void start(Future<Void> startFuture) throws Exception 
	{	
		logger.debug("In start");
		
		int port 			= 	80;
		
		pool				=	new HashMap<String, String>();
		
		dbService			=	DbService.createProxy(vertx, "db.queue");
		
		
		HttpServer server 	=	vertx.createHttpServer();
		
		server
		.requestHandler(HttpServerVerticle.this)
		.listen(port, ar -> {
				
			if(ar.succeeded())
			{
				logger.debug("Server started");
				startFuture.complete();
			}
			else
			{
				logger.debug("Could not start server. Cause="+ar.cause());
				startFuture.fail(ar.cause());
			}
		});

		vertx.exceptionHandler(err -> {
			err.printStackTrace();
		});
		
	}

	@Override
	public void handle(HttpServerRequest event) 
	{
		HttpServerResponse	resp	=	event.response();
		logger.debug("In handle method");
		
		logger.debug("Event path="+event.path());
		
		switch (event.path()) 
		{
			case "/auth/user":
			
				if(event.method().toString().equalsIgnoreCase("GET")) 
				{
					authUser(event);
					break;
				} 
				else 
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
	
			case "/auth/vhost":
			
				if(event.method().toString().equalsIgnoreCase("GET")) 
				{
					authVhost(event);
				}
				else 
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
				break;
				
			case "/auth/topic":
				
				if(event.method().toString().equalsIgnoreCase("GET"))
				{
					authTopic(event);
				}
				else
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
				
				break;
			
			case "/auth/resource":
				
				if(event.method().toString().equalsIgnoreCase("GET"))
				{
					authResource(event);
				}
				else
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
				
				break;
			
			default:
				
				resp = event.response();
				resp.setStatusCode(404).end();
				return;
		}
	}
	
	public void authUser(HttpServerRequest req)
	{
		HttpServerResponse	resp	=	req.response();	 
		String	username			=	req.getParam("username");
		String	password			=	req.getParam("password");
		
		if ((!isStringSafe(username))||(!isStringSafe(password)))
		{
			logger.debug("invalid entity name");
			ok(resp,"deny");
			return;
		}
		
		if(username.length()>=65)
		{
			logger.debug("long username");
			badRequest(resp);
			return;
		}
		
		checkLogin(username, password)
		.setHandler(login -> {
			
			if(!login.succeeded())
			{
				ok(resp,"deny");
				return;
			}
			
			if("admin".equals(username))
			{
				ok(resp, "allow administrator management");
				return;
			}
			
			ok(resp, "allow");
			return;
		});
	}
	
	public void authVhost(HttpServerRequest req)
	{
		HttpServerResponse resp = req.response();
		
		ok(resp, "allow");
		return;
	}
	
	public void authTopic(HttpServerRequest req)
	{
		HttpServerResponse resp = req.response();
		
		ok(resp, "allow");
		return;
	}
	
	public void authResource(HttpServerRequest req)
	{
		HttpServerResponse resp	=	req.response();
		String username			=	req.getParam("username");
		String resource			=	req.getParam("resource");
		String name				=	req.getParam("name");
		String permission		=	req.getParam("permission");
		
		logger.debug(name);
		logger.debug(username);
		logger.debug(resource);
		logger.debug(permission);
		
		if ("admin".equals(username))
		{
			ok(resp,"allow");
			return;
		}
		
		if("configure".equals(permission))
		{
			forbidden(resp, "deny");
			return;
		}
		
		if(username.length()<7||username.length()>65)
		{
			forbidden(resp,"deny");
			return;
		}
		
		if(isValidOwner(name))
		{
			forbidden(resp,"deny");
			return;
		}
		
		if("queue".equals(resource))
		{
			if("write".equals(permission))
			{
				logger.debug("permission is write");
				forbidden(resp,"deny");
				return;
			}
			
			if(!name.startsWith(username))
			{
				logger.debug("name does not start with username");
				forbidden(resp,"deny");
				return;
			}
			
			if(isValidOwner(username))
			{
				logger.debug("user is an owner");
				if(name.equals(username+".notification"))
				{
					ok(resp,"allow");
					return;
				}
			}
			else
			{
				if	(		name.equals(username)
									||
						name.equals(username+".priority")
									||
						name.equals(username+".command")
					)
				{
					ok(resp,"allow");
					return;
				}
			}
		}
		else if	(	("exchange".equals(resource))
								||
					("topic".equals(resource))
				)
		{
			if("read".equals(permission))
			{
				ok(resp,"allow");
				return;
			}
			
			if(isValidOwner(username))
			{
				forbidden(resp,"deny");
				return;
			}
			
			if(name.startsWith(username)&&name.contains("."))
			{
				if	(	name.endsWith(".public")
								||
						name.endsWith(".private")
								||
						name.endsWith(".protected")
								||
						name.endsWith(".diagnostics")
								||
						name.endsWith(".publish")
					)
				{
					ok(resp,"allow");
					return;
				}
				else
				{
					forbidden(resp,"deny");
					return;
				}
				
			}
		}
		

	}
	
	public Future<Boolean> checkLogin(String id, String apikey)
	{
		logger.debug("In check_login");
		
		logger.debug("ID="+id);
		logger.debug("Apikey="+apikey);
		
		Future<Boolean> check = Future.future();

		if("".equals(id) || "".equals(apikey))
		{
			check.fail("Invalid credentials");
		}
		
		if(! (isValidOwner(id) ^ isValidEntity(id)))
		{
			check.fail("Invalid credentials");
		}
		
		String query		=	"SELECT * FROM users WHERE id	=	'"	+
								id		+ 							"'"	+
								"AND blocked = 'f'"						;
		
		dbService.runQuery(query, reply -> {
			
		if(reply.succeeded())
		{	
			List<String> resultList	=	reply.result();
			int rowCount			=	resultList.size();
			
			if(rowCount==0)
			{
				check.fail("Not found");
			}
			
			else if(rowCount==1)
			{
				String raw				=	resultList.get(0);
				String row[] 			=	raw
											.substring(	raw.indexOf("[")+1, 
														raw.indexOf("]"))
											.split(",\\s");

				String salt 			=	row[3];
				String string_to_hash	=	apikey	+	salt	+	id;
				String expected_hash 	= 	row[1];
				String actual_hash 		= 	Hashing
											.sha256()
											.hashString(string_to_hash, StandardCharsets.UTF_8)
											.toString();
				
				boolean autonomous		=	"true".equals(row[5])?true:false;
				
				logger.debug("Salt ="+salt);
				logger.debug("String to hash="+string_to_hash);
				logger.debug("Expected hash ="+expected_hash);
				logger.debug("Actual hash ="+actual_hash);
										
				if(actual_hash.equals(expected_hash))
				{
					check.complete(autonomous);
				}
				else
				{
					check.fail("Invalid credentials");
				}
			}
			else
			{
				check.fail("Something is terribly wrong");
			}
		}
	});
		
	return check;
	}

	public boolean isStringSafe(String resource)
	{
		logger.debug("In is_string_safe");
		
		logger.debug("resource="+resource);
		
		boolean safe = (resource.length() - (resource.replaceAll("[^#-/a-zA-Z0-9_]+", "")).length())==0?true:false;
		
		logger.debug("Original resource name ="+resource);
		logger.debug("Replaced resource name ="+resource.replaceAll("[^#-/a-zA-Z0-9]+", ""));
		return safe;
	}
	
	public boolean isValidOwner(String owner_name)
	{
		logger.debug("In is_valid_owner");
		
		//TODO simplify this
		if	(	(!Character.isDigit(owner_name.charAt(0)))
									&&
			(	(owner_name.length() - (owner_name.replaceAll("[^a-z0-9]+", "")).length())==0)
			)
		{
			logger.debug("Original owner name = "+owner_name);
			logger.debug("Replaced name = "+owner_name.replaceAll("[^a-z0-9]+", ""));
			return true;
		}
		else
		{
			logger.debug("Original owner name = "+owner_name);
			logger.debug("Replaced name = "+owner_name.replaceAll("[^a-z0-9]+", ""));
			return false;
		}
	}
	
	public boolean isValidEntity(String resource)
	{
		//TODO: Add a length check
		logger.debug("In is_valid_entity");
		
		String entries[]	=	resource.split("/");
		
		logger.debug("Entries = "+Arrays.asList(entries));
		
		if(entries.length!=2)
		{
			return false;
		}
		else if	(	(isValidOwner(entries[0]))
								&&
					(isStringSafe(entries[1]))
				)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public void forbidden(HttpServerResponse resp)
	{
		if(!resp.closed())
		{
			resp.setStatusCode(403).end();
		}
		return;
	}
	
	public void forbidden(HttpServerResponse resp, String message)
	{
		if(!resp.closed())
		{
			resp.setStatusCode(403).end(message);
		}
		
		return;
	}
	
	public void error(HttpServerResponse resp, String message)
	{
		if(!resp.closed())
		{
			resp
			.putHeader("content-type", "application/json")
			.setStatusCode(500)
			.end(new JsonObject()
				.put("error", message)
				.encodePrettily());
		}
		
		return;
	}
	
	public void ok(HttpServerResponse resp)
	{
		if(!resp.closed())
		{
			resp.setStatusCode(200).end();
		}
		
		return;
	}
	
	public void ok(HttpServerResponse resp, String message)
	{
		if(!resp.closed())
		{
			resp.setStatusCode(200).end(message);
		}
		
		return;
	}
	
	public void accepted(HttpServerResponse resp)
	{
		if(!resp.closed())
		{
			resp.setStatusCode(202).end();
		}
		
		return;
	}
	
	public void accpeted(HttpServerResponse resp, String message)
	{
		if(!resp.closed())
		{
			resp.setStatusCode(202).end(message);
		}
		
		return;
	}
	
	public void badRequest(HttpServerResponse resp)
	{
		if(!resp.closed())
		{
			resp.setStatusCode(400).end();
		}
		
		return;
	}
	
	public void badRequest(HttpServerResponse resp, String message)
	{
		if(!resp.closed())
		{
			resp.setStatusCode(400).end(message);
		}
		
		return;
	}	
	
	public void conflict(HttpServerResponse resp, String message)
	{
		if(!resp.closed())
		{
			resp.setStatusCode(409).end(message);
		}
		
		return;
	}
}
				