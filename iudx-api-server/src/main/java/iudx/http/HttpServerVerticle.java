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

package iudx.http;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.apache.commons.lang3.RandomStringUtils;
import com.google.common.hash.Hashing;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;

import iudx.broker.BrokerService;
import iudx.database.DbService;

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
	public	BrokerService		brokerService;
	
	//Pool to check login info
	public	Map<String, String>	pool;
	
	//Characters to be used by APIKey generator while generating apikey 
	private static final String PASSWORDCHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-";
	
	/**
	 * This method is used to setup and start the Vert.x server. It uses the
	 * available processors (n) to create (n*2) workers and also gets the available
	 * URLs from the URL class.
	 * 
	 * @param args Unused.
	 * @return Nothing.
	 * @exception Exception On setup or start error.
	 * @see Exception
	 */
	
	/**
	 * This method is used to setup certificates for enabling HTTPs in Vert.x
	 * server. It uses the provided .jks (Java Key Store) certificate in the path
	 * and the password to enable SSL/TLS over HTTP in the desired port.
	 * 
	 * @param Nothing.
	 * @return Nothing.
	 * @exception Exception On start error.
	 * @see Exception
	 */
	
	@Override
	public void start(Future<Void> startFuture) throws Exception 
	{	
		logger.debug("In start");
		
		int port 			= 	8443;
		
		pool				=	new HashMap<String, String>();
		
		dbService			=	DbService.createProxy(vertx, "db.queue");
		brokerService		=	BrokerService.createProxy(vertx, "broker.queue");
		
		
		HttpServer server 	=	vertx.createHttpServer(new HttpServerOptions()
								.setSsl(true)
								.setKeyStoreOptions(new JksOptions()
								.setPath("my-keystore.jks")
								.setPassword("password")));
		
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
		
		brokerService.createQueue("DATABASE", ar -> {
			
			if(!ar.succeeded())
			{
				logger.error("Could not create queue. Cause="+ar.cause());
			}
		});
		
	}
	
	/**
	 * This method is used to handle the client requests and map it to the
	 * corresponding APIs using a switch case.
	 * 
	 * @param HttpServerRequest event - This is the handle for the incoming request
	 *                          from client.
	 * @return Nothing.
	 */

	@Override
	public void handle(HttpServerRequest event) 
	{
		HttpServerResponse	resp	=	event.response();
		logger.debug("In handle method");
		
		logger.debug("Event path="+event.path());
		
		switch (event.path()) 
		{
			case "/admin/register-owner":
			
				if(event.method().toString().equalsIgnoreCase("POST")) 
				{
					registerOwner(event);
					break;
				} 
				else 
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
	
			case "/admin/deregister-owner":
			
				if(event.method().toString().equalsIgnoreCase("POST")) 
				{
					deRegisterOwner(event);
				}
				else 
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
				break;
				
			case "/entity/publish":
				
				if(event.method().toString().equalsIgnoreCase("POST"))
				{
					publish(event);
				}
				else
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
				
				break;
				
			case "/entity/publish-async":
				
				if(event.method().toString().equalsIgnoreCase("POST"))
				{
					publishAsync(event);
				}
				else
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
				
				break;
		
			case "/entity/subscribe":
				
				if(event.method().toString().equalsIgnoreCase("GET"))
				subscribe(event);
				break;
			
			case "/catalogue":
				
				if(event.method().toString().equalsIgnoreCase("GET"))
				cat(event);
				break;
		
			case "/owner/register-entity":
			case "/admin/register-entity":
				
				if(event.method().toString().equalsIgnoreCase("POST")) 
				{
					register(event);
				} 
				else 
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
				break;
			
			case "/owner/deregister-entity":
			case "/admin/deregister-entity":
				
				if(event.method().toString().equalsIgnoreCase("POST")) 
				{
					deRegister(event);
				}
				else 
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
				break;
				
			case "/owner/block":
			case "/admin/block":
			
				if(event.method().toString().equalsIgnoreCase("POST")) 
				{
					block(event, "t");
				} 
				else 
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
				break;
				
			case "/owner/unblock":
			case "/admin/unblock":
			
				if(event.method().toString().equalsIgnoreCase("POST")) 
				{
					block(event, "f");
				} 
				else 
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
				break;
				
			case "/entity/bind":
			case "/owner/bind" :
				
				if(event.method().toString().equalsIgnoreCase("POST")) 
				{
					queueBind(event);
				} 
				else 
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
				break;
			
			case "/entity/unbind":
			case "/owner/unbind" :
				
				if(event.method().toString().equalsIgnoreCase("POST")) 
				{
					queueUnbind(event);
				} 
				else 
				{
					resp = event.response();
					resp.setStatusCode(404).end();
				}
				break;

			case "/entity/follow" :
			case "/owner/follow" :
				
				if(event.method().toString().equalsIgnoreCase("POST")) 
				{
					follow(event);
				} 
				else 
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
				break;
			
			case "/entity/unfollow" :
			case "/owner/unfollow" :
				
				if(event.method().toString().equalsIgnoreCase("POST")) 
				{
					unfollow(event);
				} 
				else 
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
				break;
				
			case "/entity/share":
			case "/owner/share" :
				
				if(event.method().toString().equalsIgnoreCase("POST")) 
				{
					share(event);
				} 
				else 
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
				break;
				
			case "/owner/entities" :
				
				if(event.method().toString().equalsIgnoreCase("GET")) 
				{
					entities(event);
				} 
				else 
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
				break;
				
			case "/owner/reset-apikey" :
				
				if(event.method().toString().equalsIgnoreCase("POST")) 
				{
					resetApikey(event);
				} 
				else 
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
				break;
			
			case "/owner/set-autonomous" :
				
				if(event.method().toString().equalsIgnoreCase("POST")) 
				{
					setAutonomous(event);
				} 
				else 
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
				break;
			
			case "/admin/owners" :
				
				if(event.method().toString().equalsIgnoreCase("GET")) 
				{
					getOwners(event);
				} 
				else 
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
				break;
				
			case "/entity/follow-requests":
			case "/owner/follow-requests" :
				
				if(event.method().toString().equalsIgnoreCase("GET")) 
				{
					followRequests(event);
				} 
				else 
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
				break;
				
			case "/entity/follow-status":
			case "/owner/follow-status" :
				
				if(event.method().toString().equalsIgnoreCase("GET")) 
				{
					followStatus(event);
				} 
				else 
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
				break;
				
			case "/entity/reject-follow":
			case "/owner/reject-follow" :
				
				if(event.method().toString().equalsIgnoreCase("POST")) 
				{
					rejectFollow(event);
				} 
				else 
				{
					resp = event.response();
					resp.setStatusCode(404).end();
					return;
				}
				break;
				
			case "/entity/permissions":
			case "/owner/permissions" :
				
				if(event.method().toString().equalsIgnoreCase("GET")) 
				{
					permissions(event);
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
				
				break;
		}
	}
	
	/**
	 * This method is the implementation of owner Registration API, which handles
	 * new owner registration requests by IUDX admin.
	 * 
	 * @param HttpServerRequest req - This is the handle for the incoming request
	 *                          from client.
	 * @return HttpServerResponse resp - This sends the appropriate response for the
	 *         incoming request.
	 */
	
	public void registerOwner(HttpServerRequest req) 
	{
		logger.debug("In register owner");
		
		HttpServerResponse	resp	=	req.response();
		String	id					=	req.getHeader("id");
		String	apikey				=	req.getHeader("apikey");
		String owner_name			=	req.getHeader("owner");
		
		logger.debug("id="+id);
		logger.debug("apikey="+apikey);
		logger.debug("owner="+owner_name);
		
		if(		(id == null)
					||
			(apikey == null)
					||
			(owner_name == null)
		)
		{
			badRequest(resp,"Inputs missing in headers");
			return;
		}
		
		if (!id.equalsIgnoreCase("admin")) 
		{
			forbidden(resp);
			return;
		}
		
		if(!isValidOwner(owner_name))
		{
			badRequest(resp,"Owner name is invalid");
			return;
		}
		
		checkLogin(id,apikey)
		.setHandler(login -> {
			
		if(!login.succeeded())
		{
			forbidden(resp,"Invalid id or apikey");
			return;
		}
			
		logger.debug("Login ok");
			
		entityDoesNotExist(owner_name)
		.setHandler(entityDoesNotExist -> {
					
		if(!entityDoesNotExist.succeeded())
		{
			conflict(resp,"Owner already exists");
			return;
		}
		
		logger.debug("Owner does not exist");
			
			//TODO schema is null
		generateCredentials(owner_name, "{}", "true")
		.setHandler(generate_credentials -> {
		
		if(!generate_credentials.succeeded())
		{
			error(resp,"Could not generate credentials");
			return;
		}
		
		logger.debug("Generated credetials for owner");
			
		brokerService.createOwnerResources(owner_name, broker_create -> {
		
		if(!broker_create.succeeded())
		{
			error(resp,"Could not create exchanges and queues");
			return;
		}
		
		logger.debug("Created owner resources");
			
		brokerService.createOwnerBindings(owner_name, broker_bind -> {
		
		if(!broker_bind.succeeded())
		{
			error(resp,"Could not create bindings");
			return;
		}
		
		logger.debug("Created owner bindings. All ok");
		
		if(!resp.closed())
		{
			resp
			.putHeader("content-type", "application/json")
			.setStatusCode(201)
			.end(new JsonObject()
				.put("id", owner_name)
				.put("apikey", generate_credentials.result())
				.encodePrettily());
			return;
		}
						});		
					});						
				});
			});
		});
	}
	
	/**
	 * This method is the implementation of owner De-Registration API, which handles
	 * owner de-registration requests by IUDX admin.
	 * 
	 * @param HttpServerRequest req - This is the handle for the incoming request
	 *                          from client.
	 * @return HttpServerResponse resp - This sends the appropriate response for the
	 *         incoming request.
	 */
	
	//TODO: deregister owner has to be async
	//TODO: Handle all errors correctly
	//TODO: Add script to remove zombie entries in postgres as well as broker (in case async deregister fails)
	
	public void deRegisterOwner(HttpServerRequest req) 
	{
		logger.debug("In deregister_owner");
		
		HttpServerResponse resp		= req.response();
		String id 					= req.getHeader("id");
		String apikey 				= req.getHeader("apikey");
		String owner_name 			= req.getHeader("owner");
		
		logger.debug("id="+id);
		logger.debug("apikey="+apikey);
		logger.debug("owner="+owner_name);
		
		if(		(id == null)
					||
			  (apikey == null)
					||
			(owner_name == null)
		)
		{
			badRequest(resp,"Inputs missing in headers");
			return;
		}
		
		if (!"admin".equalsIgnoreCase(id)) 
		{
			forbidden(resp);
			return;
		}
		
		if(!isValidOwner(owner_name))
		{
			badRequest(resp,"Owner name is invalid");
			return;
		}
		
		checkLogin(id, apikey)
		.setHandler(login -> {
			
		if(!login.succeeded())
		{
			forbidden(resp,"Invalid id or apikey");
			return;
		}
			
		logger.debug("Login ok");
		
		entityExists(owner_name).setHandler(ownerExists -> {
		
		if(!ownerExists.succeeded())
		{
			forbidden(resp,"No such owner");
			return;
		}
		
		logger.debug("Owner exists");
			
		brokerService.deleteOwnerResources(owner_name, delete_owner_resources -> {
		
		if(!delete_owner_resources.succeeded())
		{
			error(resp,"Could not delete owner resources");
			return;
		}
		
		logger.debug("Deleted owner resources from broker");
			
		String acl_query	=	"DELETE FROM acl WHERE"	+
								" from_id LIKE '"		+	
								owner_name				+	
								"/%'"					+
								" OR exchange LIKE '"	+
								owner_name				+
								"/%'"					;
								
		dbService.runQuery(acl_query, aclDelete -> {
									
		if(!aclDelete.succeeded())
		{
			error(resp,"Could not delete from acl table");
			return;
		}
		
		logger.debug("Deleted owner entries from acl table");
			
		String entity_query	=	"SELECT * FROM users WHERE"	+
					    		" id LIKE '"				+	
					    		owner_name					+								    					  
					    		"/%'"						;
										
		dbService.runQuery(entity_query, ids -> {
		
		if(!ids.succeeded())
		{
			error(resp,"Could not get entities belonging to owner");
			return;
		}
		
		List<String> resultList	=	ids.result();
			
		String id_list	=	"";
			
		for(String row:resultList)
		{
			String processed_row[]	=	row
										.substring	(row.indexOf("[")+1, row.indexOf("]"))
										.trim()
										.split(",\\s");
				
			logger.debug("Processed row="+Arrays.asList(processed_row));
				
			id_list	=	id_list	+	processed_row[0]	+	",";
		}
			
		id_list	=	id_list.substring(0,id_list.length()-1);
			
		logger.debug("id_list="+id_list);
												
		brokerService.deleteEntityResources(id_list, deleteEntities -> {
		
		if(!deleteEntities.succeeded())
		{
			error(resp, "Could not delete owner entities");
			return;
		}
		
		logger.debug("Deleted entity resources from broker");
			
		String user_query	=	"DELETE FROM users WHERE"	+
								" id LIKE '"				+	
								owner_name					+	
								"/%'"						+
								" OR id LIKE '"				+
								owner_name					+
								"'"							;
														
		dbService.runQuery(user_query, deleteUsers -> {
		
		if(!deleteUsers.succeeded())
		{
			error(resp,"Could not delete from users' table");
			return;
		}
		
		logger.debug("Deleted entities from users table. All ok");
		
		ok(resp);
		return;
								});
							});
						});
					});
				});
			});	
		});
	}
	
	/**
	 * This method is the implementation of entity Registration API, which handles
	 * the new device or application registration requests by owners.
	 * 
	 * @param HttpServerRequest req - This is the handle for the incoming request
	 *                          from client.
	 * @return HttpServerResponse resp - This sends the appropriate response for the
	 *         incoming request.
	 */

	//TODO: Try Future Compose?
	
	public void register(HttpServerRequest req) 
	{
		logger.debug("In register entity");
		
		logger.debug("host="+req.remoteAddress());
		
		HttpServerResponse resp	=	req.response();
		String id 				=	req.getHeader("id");
		String apikey			=	req.getHeader("apikey");
		String entity			=	req.getHeader("entity");
		String is_autonomous	=	req.getHeader("is-autonomous");
		String full_entity_name	=	id	+ "/" 	+ entity;
		
		String autonomous_flag;
		
		if(is_autonomous	==	null)
		{
			autonomous_flag	=	"f";
		}
		else if("true".equals(is_autonomous))
		{
			autonomous_flag	=	"t";
		}
		else if("false".equals(is_autonomous))
		{
			autonomous_flag	=	"f";
		}
		else
		{
			badRequest(resp,"Invalid is-autonomous header");
			return;
		}
		
		logger.debug("id="+id+"\napikey="+apikey+"\nentity="+entity+"\nis-autonomous="+autonomous_flag);
		
		//TODO: Check if body is null
		req.bodyHandler(body -> {
			
			schema = body.toString();
			logger.debug("schema="+schema);
			
			try
			{
				new JsonObject(schema);
			}
			catch (Exception e)
			{
				forbidden(resp,"Body must be a valid JSON");
				return;
			}
		});	
		
		if(	  (id == null)
				  ||
			(apikey == null)
				  ||
			(entity == null)
		)
		{
			badRequest(resp,"Inputs missing in headers");
			return;
		}
		
		//TODO: Add appropriate field checks. E.g. valid owner, valid entity etc.
		// Check if ID is owner
		if (!isValidOwner(id)) 
		{
			logger.debug("owner is invalid");
			forbidden(resp,"Invalid owner");
			return;
		} 
		
		if(!isStringSafe(entity))
		{
			logger.debug("invalid entity name");
			badRequest(resp,"Invalid entity name");
			return;
		}
		
		checkLogin(id,apikey).setHandler(login -> {
		
		if(!login.succeeded())
		{
			logger.debug(login.cause());
			forbidden(resp,"Invalid id or apikey");
			return;
		}
		
		logger.debug("login ok");
		entityDoesNotExist(full_entity_name).setHandler(entityDoesNotExist -> {
					
		if(!entityDoesNotExist.succeeded())
		{
			logger.debug(entityDoesNotExist.cause());
			conflict(resp,"ID already used");
			return;
		}
		
		logger.debug("entity does not exist");
		generateCredentials(full_entity_name, schema, autonomous_flag)
		.setHandler(genCredentials -> {
							
		if(!genCredentials.succeeded())
		{
			logger.debug(genCredentials.cause());
			error(resp,"Could not generate entity credentials");
			return;
		}
		
		logger.debug("credentials generated");
		brokerService.createEntityResources(full_entity_name, createEntityResources -> {
									
		if(!createEntityResources.succeeded())
		{
			logger.debug(createEntityResources.cause());
			error(resp,"Could not create exchanges and queues");
			return;
		}
		
		logger.debug("resources created");
		brokerService.createEntityBindings(full_entity_name, createEntityBindings -> {
										
		if(!createEntityBindings.succeeded())
		{
			logger.debug(createEntityBindings.cause());
			error(resp,"Could not create bindings");
			return;
		}
		
		logger.debug("all ok");
			
		JsonObject response = new JsonObject();
		response.put("id", full_entity_name);
		response.put("apikey", genCredentials.result());
			
		if(!resp.closed())
		{
			resp
			.putHeader("content-type", "application-json")
			.setStatusCode(201)
			.end(response.encodePrettily());
			return;
		}
						});
					});
				});		
			});
		});	
	}
	/**
	 * This method is the implementation of entity De-Registration API, which handles
	 * the device or application de-registration requests by owners.
	 * 
	 * @param HttpServerRequest req - This is the handle for the incoming request
	 *                          from client.
	 * @return HttpServerResponse resp - This sends the appropriate response for the
	 *         incoming request.
	 */
	
	public void deRegister(HttpServerRequest req) 
	{
		logger.debug("In deregister entity");
		
		HttpServerResponse resp =	req.response();
		String id 				=	req.getHeader("id");
		String apikey			=	req.getHeader("apikey");
		String entity			=	req.getHeader("entity");
		
		logger.debug("id="+id+"\napikey="+apikey+"\nentity="+entity);

		// Check if ID is owner
		if (!isValidOwner(id)) 
		{
			forbidden(resp,"Invalid owner");
			return;
		} 
		
		if(!isOwner(id, entity))
		{
			forbidden(resp,"You are not the owner of the entity");
			return;
		}
		
		if(!isValidEntity(entity))
		{
			forbidden(resp,"Invalid entity");
			return;
		}
		
		checkLogin(id, apikey)
		.setHandler(login -> {
			
		if(!login.succeeded())
		{
			logger.debug("invalid credentials. Cause="+login.cause());
			forbidden(resp,"Invalid id or apikey");
			return;
		}
		
		logger.debug("login ok");
			
		entityExists(entity)
		.setHandler(entityExists -> {
					
		if(!entityExists.succeeded())
		{
			logger.debug("No such entity. Cause="+entityExists.cause());
			badRequest(resp,"No such entity");
			return;
		}
		
		logger.debug("entity exists");
		brokerService.deleteEntityResources(entity, deleteEntityResources -> {
							
		if(!deleteEntityResources.succeeded())
		{
			logger.debug("Could not delete entity resources. Cause="+deleteEntityResources.cause());
			error(resp,"Could not delete exchanges and queues");
			return;
		}
		
		logger.debug("Entity resources deleted");
			
		String acl_query	=	"DELETE FROM acl WHERE "	+
								"from_id = '"				+
								entity						+
								"' OR exchange LIKE '"		+
								entity						+
								".%'"						;
		
		dbService.runQuery(acl_query, aclQuery -> {
									
		if(!aclQuery.succeeded())
		{
			logger.debug("Could not delete from acl. Cause="+aclQuery.cause());
			error(resp,"Could not delete from acl");
			return;
		}
		
		logger.debug("Deleted from acl");
			
		String follow_query	=	"DELETE FROM follow WHERE "	+
								" requested_by = '"			+
								entity						+
								"' OR exchange LIKE '"		+
								entity						+
								".%'"						;
										
		dbService.runQuery(follow_query, followQuery -> {
											
		if(!followQuery.succeeded())
		{
			logger.debug("Could not delete from follow. Cause="+followQuery.cause());
			error(resp,"Could not delete from follow");
			return;
		}
		
		logger.debug("Deleted from follow");
			
		String user_query	=	"DELETE FROM users WHERE "	+
								" id = '"					+
								entity						+
								"'"							;
												
		dbService.runQuery(user_query, userQuery -> {
													
		if(!userQuery.succeeded())
		{
			logger.debug("Could not delete from users. Cause="+userQuery.cause());
			error(resp,"Could not delete from users");
			return;
		}
		
		logger.debug("all ok");
	
		ok(resp);
		return;
							});
						});	
					});
				});		
			});	
		});
	}
	
	/**
	 * This method is the implementation of entity Block and Un-Block API, which
	 * handles the device or application block requests by owners.
	 * 
	 * @param HttpServerRequest req - This is the handle for the incoming request
	 *                          from client.
	 * @param                   boolean block - This is the flag for a block request
	 * @param                   boolean un_block - This is the flag for an un-block
	 *                          request
	 * @return HttpServerResponse resp - This sends the appropriate response for the
	 *         incoming request.
	 */
	
	public void block(HttpServerRequest req, String blocked) 
	{
		logger.debug("In block/unblock API");
		
		HttpServerResponse resp	=	req.response();
		String 	id				=	req.getHeader("id");
		String 	apikey			=	req.getHeader("apikey");
		String	owner			=	req.getHeader("owner");
		String	entity			=	req.getHeader("entity");
		
		if	(	(id		==	null)
						||
				(apikey	==	null)
			)
		{
			badRequest(resp,"Inputs missing in headers");
			return;
		}
		
		logger.debug("id="+id);
		logger.debug("apikey="+apikey);
		
		if	(	(owner	==	null)
						&&
				(entity	==	null)
			)
		{
			badRequest(resp, "Inputs missing in headers");
			return;
		}
		
		if(owner != null)
		{
			if(!isStringSafe(owner))
			{
				badRequest(resp, "Invalid owner");
				return;
			}
			
			if(!("admin".equals(id)))
			{
				forbidden(resp, "Only admin can block owners");
				return;
			}
			
			if(!isValidOwner(owner))
			{
				badRequest(resp, "owner is not valid");
				return;
			}
		}
			
		else if(entity != null)
		{	
			if	(	!(
					isOwner(id, entity)
							|| 
					"admin".equals(id)
					)
				)
			{
				forbidden(resp,"You are not the owner of the entity");
				return;
			}
			
			if(!isStringSafe(entity))
			{
				badRequest(resp, "Invalid entity");
				return;
			}
			
			if(!isValidEntity(entity))
			{
				forbidden(resp,"entity is not valid");
				return;
			}
		}
		
		final String username	=	(owner==null)?entity:owner;
		
		final String userString	=	(owner==null)?entity:owner+"/%";
		
		checkLogin(id,apikey)
		.setHandler(login -> {
			
		if(!login.succeeded())
		{
			forbidden(resp,"Invalid id or apikey");
			return;
		}
		
		logger.debug("Login ok");
			
		entityExists(username)
		.setHandler(entityExists -> {
					
		if(!entityExists.succeeded())
		{
			forbidden(resp,"No such entity");
			return;
		}
		
		logger.debug("Entity exists");
			
		String query	=	"UPDATE users SET blocked = '"	+
							blocked							+
							"' WHERE (id = '"				+
							username						+
							"' OR id LIKE '"				+
							userString						+
							"')"							;
		dbService.runQuery(query, updateUserTable -> {
							
		if(!updateUserTable.succeeded())
		{
			error(resp,"Could not update users table");
			return;
		}
		
		logger.debug("Updated users table. All ok");
			
		ok(resp);
		return;
		
				});
			});	
		});
	}
	
	public void queueBind(HttpServerRequest req)
	{
		logger.debug("In queue_bind");
		
		HttpServerResponse resp	=	req.response();
		
		//Mandatory headers
		String id				=	req.getHeader("id");
		String apikey			=	req.getHeader("apikey");
		String to				=	req.getHeader("to");
		String topic			=	req.getHeader("topic");
		String message_type		=	req.getHeader("message-type");
		
		//Optional headers
		String is_priority		=	req.getHeader("is-priority");
		String from				=	req.getHeader("from");
		
		logger.debug("id="+id);
		logger.debug("apikey="+apikey);
		logger.debug("to="+to);
		logger.debug("topic="+topic);
		logger.debug("message-type="+message_type);
		
		logger.debug("is-priorty="+is_priority);
		logger.debug("from="+from);
		
		if	(		(id		==	null)
							||
					(apikey	==	null)
							||
					(to		==	null)
							||
					(topic	==	null)
							||
				(message_type	==	null)
					
			)
			{
				badRequest(resp,"Inputs missing in headers");
				return;
			}
		
		if(!(isValidOwner(id) ^ isValidEntity(id)))
		{
			badRequest(resp,"Invalid id");
			return;
		}
		if(isValidOwner(id))
		{
			if(from	==	null)
			{
				forbidden(resp,"'from' value missing in headers");
				return;
			}
			
			if(!isOwner(id, from))
			{
				forbidden(resp,"You are not the owner of the 'from' entity");
				return;
			}
			
			if(!isValidEntity(from))
			{
				forbidden(resp,"'from' is not a valid entity");
				return;
			}
		}
		else
		{
			from	=	id;
		}
		
		if	(	(!"public".equals(message_type))
								&&
				(!"private".equals(message_type))
								&&
				(!"protected".equals(message_type))
								&&
				(!"diagnostics".equals(message_type))
			)
		{
			badRequest(resp,"'message-type' is invalid");
			return;
		}
		
		if	(	("private".equals(message_type))	
							&&	
					(!isOwner(id, to))	
			)
		{
			forbidden(resp,"You are not the owner of the 'to' entity");
			return;
		}
		
		if	(	(!isStringSafe(from))
						||
				(!isStringSafe(to))
						||
				(!isStringSafe(topic))
			)
		{
			forbidden(resp,"Invalid headers");
			return;
		}
		
		String queue	=	from;
		
		if(is_priority	!=	null)
		{
			if	(	(!"true".equals(is_priority)
								&&
					(!"false".equals(is_priority)))
				)
			{
				forbidden(resp,"Invalid is-priority header");
				return;
			}
			else if("true".equals(is_priority))
			{
				queue	=	queue	+	".priority";
			}
		}
		
		final String from_id		=	from;
		final String exchange_name	=	to + "." + message_type;
		final String queue_name		=	queue;
		
		checkLogin(id, apikey)
		.setHandler(login -> {
		
		if(!login.succeeded())
		{
			forbidden(resp,"Invalid id or apikey");
			return;
		}
			
		logger.debug("Login ok");
			
		if(!login.result())
		{
			forbidden(resp,"Unauthorised");
			return;
		}
			
		logger.debug("Autonomous ok");
				
		if(!"public".equals(message_type))
		{
			logger.debug("Message type is not public");
					
			if(!isOwner(id, to))
			{
				logger.debug("Id is not the owner of to");
						
				String acl_query	=	"SELECT * FROM acl WHERE		"	+
										"from_id			=			'"	+
										from_id								+
										"' AND exchange		=			'"	+
										exchange_name						+
										"' AND permission	=	'read'	"	+
										" AND valid_till > now()		"	+
										" AND topic			=			'"	+
										topic								+
										"'"									;
			
				dbService.runQuery(acl_query, aclQuery -> {
				
				if(!aclQuery.succeeded())
				{
					error(resp,"Could not query acl table");
					return;
				}
				
				if(aclQuery.result().size()!=1)
				{
					forbidden(resp,"Unauthorised");
					return;
				}
				
				brokerService.bind(queue_name, exchange_name, topic, bind -> {
			
				if(!bind.succeeded())
				{
					error(resp,"Bind failed");
					return;
				}
					
				logger.debug("Bound. All ok");
									
				ok(resp);
				return;
					});
				});
			}
			else
			{
				logger.debug("Id is the owner of to");
						
				brokerService.bind(queue_name, exchange_name, topic, bind -> {
						
				if(!bind.succeeded())
				{
					error(resp,"Bind failed");
					return;
				}
				
				logger.debug("Bound. All ok");
							
				ok(resp);
				return;
			});
			
			}				
		}
		
		else
		{
			logger.debug("Message type is public");
			
			logger.debug("exchange="+exchange_name);
			logger.debug("queue="+queue_name);
					
			brokerService.bind(queue_name, exchange_name, topic, bind -> {
			
			if(!bind.succeeded())
			{
				error(resp,"Bind failed");
				return;
			}
			
			logger.debug("Bound. All ok");
							
			ok(resp);
			return;
			});
		}
	});	
}
	public void queueUnbind(HttpServerRequest req)
	{
		logger.debug("In queue_unbind");
		
		HttpServerResponse resp	=	req.response();
		
		//Mandatory headers
		String id				=	req.getHeader("id");
		String apikey			=	req.getHeader("apikey");
		String to				=	req.getHeader("to");
		String topic			=	req.getHeader("topic");
		String message_type		=	req.getHeader("message-type");
		
		//Optional headers
		String is_priority		=	req.getHeader("is-priority");
		String from				=	req.getHeader("from");
		
		logger.debug("id="+id);
		logger.debug("apikey="+apikey);
		logger.debug("to="+to);
		logger.debug("topic="+topic);
		logger.debug("message-type="+message_type);
		
		logger.debug("is-priorty="+is_priority);
		logger.debug("from="+from);
		
		if	(		(id		==	null)
							||
					(apikey	==	null)
							||
					(to		==	null)
							||
					(topic	==	null)
							||
				(message_type	==	null)
					
			)
			{
				badRequest(resp,"Inputs missing in headers");
				return;
			}
		
		if(!(isValidOwner(id) ^ isValidEntity(id)))
		{
			badRequest(resp,"Invalid id");
			return;
		}
		if(isValidOwner(id))
		{
			if(from	==	null)
			{
				forbidden(resp,"'from' value missing in headers");
				return;
			}
			
			if(!isOwner(id, from))
			{
				forbidden(resp,"You are not the owner of the 'from' entity");
				return;
			}
			
			if(!isValidEntity(from))
			{
				forbidden(resp,"'from' is not a valid entity");
				return;
			}
		}
		else
		{
			from	=	id;
		}
		
		if(!isValidEntity(to))
		{
			forbidden(resp,"'to' is not a valid entity");
			return;
		}
		
		if	(	(!"public".equals(message_type))
							&&
				(!"private".equals(message_type))
							&&
				(!"protected".equals(message_type))
							&&
				(!"diagnostics".equals(message_type))
			)
		{
			badRequest(resp,"'message-type' is invalid");
			return;
		}
		
		if	(	("private".equals(message_type))	
							&&	
					(!isOwner(id, to))	
			)
		{
			forbidden(resp,"You are not the owner of the 'to' entity");
			return;
		}
		
		if	(	(!isStringSafe(from))
						||
				(!isStringSafe(to))
						||
				(!isStringSafe(topic))
			)
		{
			forbidden(resp,"Invalid headers");
			return;
		}
		
		String queue	=	from;
		
		if(is_priority	!=	null)
		{
			if	(	(!"true".equals(is_priority)
								&&
					(!"false".equals(is_priority)))
				)
			{
				forbidden(resp,"Invalid is-priority header");
				return;
			}
			else if("true".equals(is_priority))
			{
				queue	=	queue	+	".priority";
			}
		}
		
		final String from_id		=	from;
		final String exchange_name	=	to + "." + message_type;
		final String queue_name		=	queue;
		
		checkLogin(id, apikey)
		.setHandler(login -> {
		
		if(!login.succeeded())
		{
			forbidden(resp,"Invalid id or apikey");
			return;
		}
			
		logger.debug("Login ok");
			
		if(!login.result())
		{
			forbidden(resp,"Unauthorised");
			return;
		}
			
		logger.debug("Autonomous ok");
				
		if(!"public".equals(message_type))
		{
			logger.debug("Message type is not public");
					
			if(!isOwner(id, to))
			{
				logger.debug("Id is not the owner of to");
						
				String acl_query	=	"SELECT * FROM acl WHERE		"	+
										"from_id			=			'"	+
										from_id								+
										"' AND exchange		=			'"	+
										exchange_name						+
										"' AND permission	=	'read'	"	+
										" AND valid_till > now()		"	+
										" AND topic			=			'"	+
										topic								+
										"'"									;
			
				dbService.runQuery(acl_query, aclQuery -> {
				
				if(!aclQuery.succeeded())
				{
					error(resp,"Could not query acl table");
					return;
				}
				
				if(aclQuery.result().size()!=1)
				{
					forbidden(resp,"Unauthorised");
					return;
				}
				
				brokerService.unbind(queue_name, exchange_name, topic, bind -> {
			
				if(!bind.succeeded())
				{
					error(resp,"Unbind failed");
					return;
				}
					
				logger.debug("Unbound. All ok");
									
				ok(resp);
				return;
					});
				});
			}
			else
			{
				logger.debug("Id is the owner of to");
						
				brokerService.unbind(queue_name, exchange_name, topic, bind -> {
						
				if(!bind.succeeded())
				{
					error(resp,"Unbind failed");
					return;
				}
				
				logger.debug("Unbound. All ok");
							
				ok(resp);
				return;
			});
			
			}				
		}
		
		else
		{
			logger.debug("Message type is public");
					
			brokerService.unbind(queue_name, exchange_name, topic, bind -> {
			
			if(!bind.succeeded())
			{
				error(resp,"Bind failed");
				return;
			}
			
			logger.debug("Bound. All ok");
							
			ok(resp);
			return;
			});
		}
	});	
}
	
	public void follow(HttpServerRequest req) 
	{
		logger.debug("In follow API");
		
		HttpServerResponse	resp	=	req.response();
		
		//Mandatory Headers
		String 	id					=	req.getHeader("id");
		String 	apikey				=	req.getHeader("apikey");
		String 	to					=	req.getHeader("to");
		String 	topic				=	req.getHeader("topic");
		String 	validity			=	req.getHeader("validity");
		String 	permission			=	req.getHeader("permission");
		
		//Optional Headers
		String	from				=	req.getHeader("from");
		String 	message_type_header	=	req.getHeader("message-type");
		
		logger.debug("id="+id);
		logger.debug("apikey="+apikey);
		logger.debug("to="+to);
		logger.debug("message-type="+message_type_header);
		logger.debug("topic="+topic);
		logger.debug("validity="+validity);
		logger.debug("permission="+permission);
		
		if	(	(id				==	null)
								||
				(apikey			==	null)
								||
				(to				==	null)
								||
				(topic			==	null)
								||
				(validity		==	null)
								||
				(permission		==	null)
			)
		{
			badRequest(resp,"Inputs missing in headers");
			return;
		}
		
		if(!(isValidOwner(id) ^ isValidEntity(id)))
		{
			badRequest(resp,"Invalid id");
			return;
		}
		
		if(isValidOwner(id))
		{
			if(from	==	null)
			{
				forbidden(resp,"'from' value missing in headers");
				return;
			}
			
			if(!isOwner(id, from))
			{
				forbidden(resp,"You are not the owner of the 'from' entity");
				return;
			}
			
			if(!isValidEntity(from))
			{
				forbidden(resp,"'from' is not a valid entity");
				return;
			}
		}
		else
		{
			from	=	id;
		}
		
		if(message_type_header	!=	null)
		{
			if	(	(!"protected".equals(message_type_header))
									&&
					(!"diagnostics".equals(message_type_header))
				)
			{
				badRequest(resp,"'message-type' is invalid");
				return;
			}
			
		}
		else
		{
			message_type_header	=	"protected";
		}
		
		if	(	!	(	("read".equals(permission))
									||
						("write".equals(permission))
									||
						("read-write".equals(permission))
					)
			)
		{
			badRequest(resp,"Invalid permission string");
			return;
		}
		
		try
		{
			int validity_integer	=	Integer.parseInt(validity);
			
			if	(	(validity_integer<0)
							||
					(validity_integer>10000)
				)
			{
				badRequest(resp,"Invalid validity header");
				return;
			}
		}
		catch(Exception e)
		{
			badRequest(resp,"Invalid validity header");
			return;
		}
		
		if	(	(!isStringSafe(from))
						||
				(!isStringSafe(to))
						||
				(!isStringSafe(topic))
						||
				(!isStringSafe(validity))
						||
				(!isStringSafe(permission))
			)
		{
			badRequest(resp,"Invalid headers");
			return;
		}
		
		final String from_id		=	from;
		final String message_type	=	message_type_header;
		final String status			=	(isOwner(id, to))?"approved":"pending";
				
		checkLogin(id, apikey)
		.setHandler(login -> {
		
		if(!login.succeeded())
		{
			forbidden(resp, "Invalid id or apikey");
			return;
		}
		
		logger.debug("Login ok");
		
		if(!login.result())
		{
			forbidden(resp, "Unauthorised");
			return;
		}
		
		logger.debug("Autonomous ok");
			
		// Check if the requested follow entity exists
		entityExists(to).setHandler(entityExists -> {
			
		if(!entityExists.succeeded())
		{
			//TODO: What about blocked entities?
			forbidden(resp, "'to' entity does not exist");
			return;
		}
				
		if("read".equals(permission)) 
		{
			logger.debug("Permission is read");
				
			insertIntoFollow	(	id,to+"."+message_type,topic,"read",
										validity,from_id,status
								)
			.setHandler(followInsert -> {
				
			if(!followInsert.succeeded())
			{
				error(resp, "Could not insert into follow");
				return;
			}
					
			if("approved".equals(status))
			{
				String validity_string	=	"now()	+	interval	'"	+	
											validity					+	
											" hours'"					;
							
				insertIntoAcl	(	from_id, to+"."+message_type, permission,	
									validity_string, followInsert.result(), 
									topic
								)
				.setHandler(aclInsert -> {
					
				if(!aclInsert.succeeded())
				{
					error(resp,"Could not insert into acl");
					return;
				}
				
				publishToNotification	(from_id, permission, 
										to, entityExists.result())
				.setHandler(publish -> {
										
				if(!publish.succeeded())
				{
					error(resp, "Could not publish to notification exchange");
					return;
				}
				
				if(!resp.closed())
				{
					resp
					.putHeader("content-type", "application/json")
					.setStatusCode(202)
					.end(new JsonObject()
					.put("follow-id-read", followInsert.result())
					.put("status", "approved")
					.encodePrettily());
				}
				
				return;
				});
				});
			}
			else
			{
				publishToNotification(from_id, permission, to, entityExists.result())
				.setHandler(publish -> {
								
				if(!publish.succeeded())
				{
					error(resp,"Could not publish to notification exchange");
					return;
				}
				
				if(!resp.closed())
				{
					resp
					.putHeader("content-type", "application/json")
					.setStatusCode(202)
					.end(new JsonObject()
					.put("follow-id-read", followInsert.result())
					.encodePrettily());
				}
				
				return;
					});
				}
			});
		}
			
		if("write".equals(permission))
		{
			logger.debug("Permission is write");
				
			insertIntoFollow	(	id, to+".command", topic, "write", 
										validity, from_id, status
								)
			.setHandler(followInsert -> {
				
			if(!followInsert.succeeded())
			{
				error(resp,"Could not insert into follow");
				return;
			}
			
			if("approved".equals(status))
			{
				String validity_string	=	"now()	+	interval	'"	+	
											validity					+	
											" hours'"					;
							
				insertIntoAcl	(	from_id, to+".command", permission,	
									validity_string, followInsert.result(), 
									topic
								)
							
				.setHandler(aclInsert -> {
								
				if(!aclInsert.succeeded())
				{
					error(resp,"Could not insert into acl");
					return;
				}
					
				bindToPublishExchange(from_id, to, topic)
				.setHandler(bind -> {
				
				if(!bind.succeeded())
				{
					error(resp,"Could not bind publish exchnage and command queue");
					return;
				}
						
				publishToNotification(from_id, permission, to, entityExists.result())
				.setHandler(publish -> {
				
				if(!publish.succeeded())
				{
					error(resp,"Could not publish to notification exchange");
					return;
				}
				
				if(!resp.closed())
				{
					resp
					.putHeader("content-type", "application/json")
					.setStatusCode(202)
					.end(new JsonObject()
					.put("follow-id-write", followInsert.result())
					.put("status", "approved")
					.encodePrettily());
				}
											
				return;
						});
					});
				});
			}
			else
			{
				publishToNotification(from_id, permission, to, entityExists.result())
				.setHandler(publish -> {
					
				if(!publish.succeeded())
				{
					error(resp,"Could not publish to notification exchange");
					return;
				}
				
				if(!resp.closed())
				{
					resp
					.putHeader("content-type", "application/json")
					.setStatusCode(202)
					.end(new JsonObject()
					.put("follow-id-write", followInsert.result())
					.encodePrettily());
				}
					
				return;
				});
			}
				});
		}
			
		if("read-write".equals(permission))
		{
			logger.debug("Permission is read-write");
				
			insertIntoFollow	(	id, to+"."+message_type, topic, "read", 
									validity, from_id, status
								)
			.setHandler(readFollow -> {
			
			if(!readFollow.succeeded())
			{
				error(resp,"Could not insert into follow");
				return;
			}

			insertIntoFollow	(	id, to+".command", topic, "write", 
									validity, from_id, status
								)
			.setHandler(writeFollow -> {
				
			if(!writeFollow.succeeded())
			{
				error(resp,"Could not insert into follow" );
				return;
			}
			
			if("approved".equals(status))
			{
				String validity_string	=	"now()	+	interval	'"	+	
											validity					+	
											" hours'"					;
									
				insertIntoAcl	(	from_id, to+"."+message_type, "read",	
									validity_string, readFollow.result(), 
									topic
								)
				.setHandler(addAclRead -> {
				
				if(!addAclRead.succeeded())
				{
					error(resp,"Could not insert into acl");
					return;
				}
						
				insertIntoAcl	(	from_id, to+".command", "write",	
									validity_string, writeFollow.result(), 
									topic
								)
				.setHandler(addAclWrite -> {
				
				if(!addAclWrite.succeeded())
				{
					error(resp,"Could not add into acl");
					return;
				}
						
				bindToPublishExchange(from_id, to, topic)
				.setHandler(bind -> {
		
				if(!bind.succeeded())
				{
					error(resp,"Could not bind publish exchnage and command queue");
					return;
				}
				
				publishToNotification(from_id, permission, to, entityExists.result())
				.setHandler(publish -> {
				
				if(!publish.succeeded())
				{
					error(resp,"Could not publish to notification exchange");
					return;
				}
				
				if(!resp.closed())
				{
					resp
					.putHeader("content-type", "application/json")
					.setStatusCode(202)
					.end(new JsonObject()
					.put("follow-id-read", readFollow.result())
					.put("follow-id-write", writeFollow.result())
					.put("status", "approved")
					.encodePrettily());
				}
																	
				return;
							});
						});
					});	
				});
			}
			
			else
			{
				publishToNotification(from_id, permission, to, entityExists.result())
				.setHandler(publish -> {
				
				if(!publish.succeeded())
				{
					error(resp,"Could not publish to notification exchange");
					return;
				}
				
				if(!resp.closed())
				{
					resp
					.putHeader("content-type", "application/json")
					.setStatusCode(202)
					.end(new JsonObject()
							.put("follow-id-read", readFollow.result())
							.put("follow-id-write", writeFollow.result())
							.encodePrettily());
				}
											
				return;
						});
			}
					});
				});
			}
		});
	});
}
	
	public Future<Void> bindToPublishExchange(String from, String to, String topic)
	{
		Future<Void> future	=	Future.future();
		
		String exchange		=	from	+	".publish";
		String queue		=	to		+	".command";
		String routingKey	=	to		+	".command."	+	topic;	
		
		brokerService.bind(queue, exchange, routingKey, bind -> {
			
			if(bind.succeeded())
			{
				future.complete();
			}
			else
			{
				future.fail(bind.cause());
			}
		});
		return future;
	}
	
	public Future<Void> publishToNotification(String from_id, String permission, String to, boolean autonomous)
	{
		Future<Void> future	=	Future.future();
		
		String exchange			=	autonomous?to+".notification":to.split("/")[0]+".notification";
		String topic			=	"Request for follow";
		String message_string	=	from_id + " has requested " + permission + " access on " + to;
		JsonObject	message		=	new JsonObject()
									.put("message", message_string);
		
		brokerService.adminPublish(exchange, topic, message.toString(), publish -> {
			
			if(publish.succeeded())
			{
				future.complete();
			}
			else
			{
				future.fail(publish.cause());
			}
		});
		
		return future;
	}
	
	public Future<String> insertIntoFollow	(	String id, String exchange, String topic, 
													String permission, String validity, 
													String from, String status
												)
	{
		logger.debug("In insert into follow");
		
		Future<String> future = Future.future();
		
		String follow_query	=	"INSERT INTO follow VALUES (DEFAULT,	'"	+
								id											+	"','"	+
								exchange									+	"',"	+
								"now(),											'"		+
								permission									+	"','"	+
								topic										+	"','"	+
								validity									+	"','"	+
								status										+	"','"	+
								from										+	"')"	;
		
		dbService.runQuery(follow_query, followQuery -> {
			
		if(followQuery.succeeded())
		{
			String follow_id_query	=	"SELECT * FROM follow WHERE from_id	=	'"	+
										from										+
										"' AND exchange 					= 	'"	+
										exchange									+
										"'"											;
				
			dbService.runQuery(follow_id_query, getFollowID -> {
					
		if(getFollowID.succeeded())
		{
			List<String>	list	=	getFollowID.result();
						
			String row				=	list.get(0);
			
			String follow_id		=	row.substring(row.indexOf("[")+1,row.indexOf("]")).trim().split(",\\s")[0];
			
			logger.debug("Row="+row);
			logger.debug("Follow ID="+follow_id);
						
			future.complete(follow_id);
		}
		else
		{
			future.fail(getFollowID.cause());
		}
	});
		}
		else
		{
			future.fail(followQuery.cause());
		}
	});
		
		return future;
	}
	
	public Future<Void> insertIntoAcl	(	String from_id, String exchange, String permission, 
											String valid_till, String follow_id, String topic
										)
	{
		logger.debug("In insert into acl");
		
		Future<Void> future	=	Future.future();
		
		String acl_query	=	"INSERT INTO acl VALUES	(	'"	+
								from_id							+	"','"	+	
								exchange						+	"','"	+
								permission						+	"',"	+
								valid_till						+	",'"	+
								follow_id						+	"','"	+
								topic							+	"',DEFAULT)";
		
		dbService.runQuery(acl_query, query -> {
			
			if(query.succeeded())
			{
				future.complete();
			}
			else
			{
				future.fail(query.cause());
			}
		});
		return future;
	}
	
	public void unfollow(HttpServerRequest req)
	{
		logger.debug("In unfollow API");
		
		HttpServerResponse	resp	=	req.response();
		
		//Mandatory Headers
		String 	id					=	req.getHeader("id");
		String 	apikey				=	req.getHeader("apikey");
		String 	to					=	req.getHeader("to");
		String 	topic				=	req.getHeader("topic");
		String 	permission			=	req.getHeader("permission");
		String 	message_type_header	=	req.getHeader("message-type");
		
		//Optional Headers
		String	from				=	req.getHeader("from");
		
		
		logger.debug("id="+id);
		logger.debug("apikey="+apikey);
		logger.debug("to="+to);
		logger.debug("message-type="+message_type_header);
		logger.debug("topic="+topic);
		logger.debug("permission="+permission);
		
		if	(	(id						==	null)
										||
				(apikey					==	null)
										||
				(to						==	null)
										||
				(topic					==	null)
										||
				(permission				==	null)
			)
		{
			badRequest(resp,"Inputs missing in headers");
			return;
		}
		
		if(!(isValidOwner(id) ^ isValidEntity(id)))
		{
			badRequest(resp,"Invalid id");
			return;
		}
		
		if(isValidOwner(id))
		{
			if(from	==	null)
			{
				forbidden(resp, "'from' value missing in headers");
				return;
			}
			
			if(!isOwner(id, from))
			{
				forbidden(resp,"You are not the owner of the 'from' entity");
				return;
			}
			
			if(!isValidEntity(from))
			{
				forbidden(resp,"'from' is not a valid entity");
				return;
			}
		}
		else
		{
			from	=	id;
		}
		
		if(message_type_header	!=	null)
		{
			if	(	(!"protected".equals(message_type_header))
									&&
					(!"diagnostics".equals(message_type_header))
				)
			{
				forbidden(resp,"'message-type' is invalid");
				return;
			}
			
		}
		else
		{
			message_type_header	=	"protected";
		}
		
		if	(	!	(	("read".equals(permission))
									||
						("write".equals(permission))
									||
						("read-write".equals(permission))
					)
			)
		{
			badRequest(resp,"Invalid permission string");
			return;
		}
		
		
		if	(	(!isStringSafe(from))
						||
				(!isStringSafe(to))
						||
				(!isStringSafe(topic))
			)
		{
			badRequest(resp,"Invalid headers");
			return;
		}
		
		final String from_id		=	from;
		final String message_type	=	message_type_header;
		final String exchange		=	to	+	"."	+	message_type;
		
		checkLogin(id, apikey)
		.setHandler(login -> {
			
		if(!login.succeeded())
		{
			forbidden(resp,"Invalid id or apikey");
			return;
		}
			
		logger.debug("Login ok");
				
		if(!login.result())
		{
			forbidden(resp,"Unauthorised");
			return;
		}
				
		logger.debug("Autonomous ok");
		
		if	("write".equals(permission))
		{
			String acl_query	=	"SELECT * FROM acl WHERE from_id	=	'"			+
									from_id												+			
									"' AND exchange						=	'"			+
									to													+
									".command"											+
									"' AND topic						=	'"			+
									topic												+
									"' AND permission					=	'write'"	;

			dbService.runQuery(acl_query, query -> {
			
			if(!query.succeeded())
			{
				error(resp,"Could not get acl details");
				return;
			}
			
			if(query.result().size()!=1)
			{
				forbidden(resp, "Unauthorised");
				return;
			}
			
			String raw				=	query.result().get(0);
				
			String processed_row[]	=	raw.substring(raw.indexOf("[")+1, raw.indexOf("]")).trim().split(",\\s");
				
			String acl_id			=	processed_row[6];
			String follow_id		=	processed_row[4];
					
			brokerService.unbind(to+".command", from_id+".publish", to+".command."+topic, unbind -> {
						
			if(!unbind.succeeded())
			{
				error(resp,"Could not unbind");
				return;
			}
						
			String delete_from_acl	=	"DELETE FROM acl WHERE acl_id="+acl_id;
						
			dbService.runQuery(delete_from_acl, aclDelete -> {
							
			if(!aclDelete.succeeded())
			{
				error(resp,"Could not delete from acl");
				return;
			}
							
			String delete_from_follow	=	"DELETE FROM follow WHERE follow_id='"+follow_id+"'";
							
			dbService.runQuery(delete_from_follow, followDelete -> {
								
			if(!followDelete.succeeded())
			{
				error(resp,"Could not delete from follow");
				return;
			}
			
			ok(resp);
			return;
						});
					});
				});
			});
		}
		
		else if("read".equals(permission))
		{
			String acl_query	=	"SELECT * FROM acl WHERE from_id	=	'"		+
									from_id											+	
									"' AND exchange						=	'"		+
									to												+
									"."												+
									message_type									+
									"' AND topic						=	'"		+
									topic											+
									"' AND permission					=	'read'"	;
		
			dbService.runQuery(acl_query, query -> {
					
			if(!query.succeeded())
			{
				error(resp,"Could not get acl details");
				return;	
			}		
						
			if(query.result().size()!=1)
			{
				forbidden(resp,"Unauthorised");
				return;
			}
						
			String raw				=	query.result().get(0);
				
			String processed_row[]	=	raw.substring(raw.indexOf("[")+1, raw.indexOf("]")).trim().split(",\\s");
					
			String acl_id			=	processed_row[6];
			String follow_id		=	processed_row[4];
					
			String delete_from_acl	=	"DELETE FROM acl WHERE acl_id="+acl_id;
						
			dbService.runQuery(delete_from_acl, aclDelete -> {
				
			if(!aclDelete.succeeded())
			{
				error(resp,"Could not delete from acl");
				return;
			}
				
			String delete_from_follow	=	"DELETE FROM follow WHERE follow_id='"+follow_id+"'";
								
			dbService.runQuery(delete_from_follow, followDelete -> {
							
			if(!followDelete.succeeded())
			{
				error(resp,"Could not delete from follow");
				return;
			}
			
			logger.debug("Queue="+from_id+" Exchange="+exchange);
			
			brokerService.unbind(from_id, exchange, topic, unbind -> {
			
			if(!unbind.succeeded())
			{
				error(resp,"Could not unbind regular queue");
				return;
			}
			
			brokerService.unbind(from_id+".priority", exchange, topic, priorityUnbind -> {
				
			if(!priorityUnbind.succeeded())
			{
				error(resp,"Could not unbind priority queue");
				return;
			}
													
			ok(resp);
			return;	
							});
						});
					});
				});
			});	
		}
		else if("read-write".equals(permission))
		{
			String acl_query	=	"SELECT * FROM acl WHERE from_id	=	'"	+
									from_id	+	"' AND (exchange		=	'"	+
									to		+	".command' OR exchange	= 	'"	+
									to		+	".protected')"					+
									" AND topic		=	'"					+	
									topic										+
									"' AND (permission	=	'write'"			+
									" OR permission 	=	'read')"			;

			dbService.runQuery(acl_query, query -> {
			
			if(!query.succeeded())
			{
				error(resp,"Could not get acl details");
				return;
			}
			
			if(query.result().size()!=2)
			{
				forbidden(resp, "Unauthorised");
				return;
			}
			
			String first			=	query.result().get(0);
			String second			=	query.result().get(1);
			
			String first_follow_id	=	first
										.substring	(
													first.indexOf("[")+1, 
													first.indexOf("]")
													)
										.trim()
										.split(",\\s")
										[4];
			
			String second_follow_id	=	second
										.substring	(
													second.indexOf("[")+1, 
													second.indexOf("]")
													)
										.trim()
										.split(",\\s")
										[4];
			
			String first_acl_id		=	first
										.substring	(
													first.indexOf("[")+1, 
													first.indexOf("]")
													)
										.trim()
										.split(",\\s")
										[6];

			String second_acl_id	=	second
										.substring	(
													second.indexOf("[")+1, 
													second.indexOf("]")
													)
										.trim()
										.split(",\\s")
										[6];
				
			brokerService.unbind(to+".command", from_id+".publish", to+".command."+topic, commandUnbind -> {
					
			if(!commandUnbind.succeeded())
			{
				error(resp,"Could not unbind");
				return;
			}
			
			brokerService.unbind(from_id, exchange, topic, regularUnbind -> {
				
			if(!regularUnbind.succeeded())
			{
				error(resp,"Could not unbind");
				return;
			}
			
			brokerService.unbind(from_id + ".priority", exchange, topic, priorityUnbind -> {
				
			if(!priorityUnbind.succeeded())
			{
				error(resp,"Could not unbind");
				return;
			}
			
			});
			});
					
			String delete_from_acl	=	"DELETE FROM acl WHERE (acl_id	=	"	+
										first_acl_id							+
										" OR acl_id 					= 	"	+
										second_acl_id							+
										")"										;
					
			dbService.runQuery(delete_from_acl, aclDelete -> {
						
			if(!aclDelete.succeeded())
			{
				error(resp,"Could not delete from acl");
				return;
			}
						
			String delete_from_follow	=	"DELETE FROM follow WHERE (follow_id	=	"	+
											first_follow_id									+
											" OR follow_id 							= 	"	+
											second_follow_id								+
											")"												;
						
			dbService.runQuery(delete_from_follow, followDelete -> {
							
			if(!followDelete.succeeded())
			{
				error(resp,"Could not delete from follow");
				return;
			}
			
			ok(resp);
			return;
						});
					});
				});
			});
		}
		});		
	}
										
	public void share(HttpServerRequest req)
	{
		logger.debug("In share API");
		
		HttpServerResponse	resp	=	req.response();
		String 	id					=	req.getHeader("id");
		String 	apikey				=	req.getHeader("apikey");
		String 	follow_id			=	req.getHeader("follow-id");
		
		logger.debug("id="+id);
		logger.debug("apikey="+apikey);
		logger.debug("follow-id="+follow_id);
		
		
		if	(	(id	==	null)
						||
				(apikey	==	null)
						||
				(follow_id	==	null)
			)
		{
			badRequest(resp,"Inputs missing in headers");
			return;
		}
		
		String exchange_string	=	isValidOwner(id)?(id+"/%.%"):(isValidEntity(id))?id+".%":"";
		
		if(!isStringSafe(follow_id))
		{
			forbidden(resp,"Invalid follow-id");
			return;
		}
		
		checkLogin(id, apikey)
		.setHandler(login -> {
			
		if(!login.succeeded())
		{
			forbidden(resp,"Invalid id or apikey");
			return;
		}
		
		logger.debug("Login ok");
			
		if(!login.result())
		{
			forbidden(resp,"Unauthorised");
			return;
		}
			
		logger.debug("Autonomous ok");
			
		String follow_query	=	"SELECT * FROM follow WHERE follow_id	=	"	+
								Integer.parseInt(follow_id)						+
								" AND exchange LIKE 						'"	+
								exchange_string									+
								"' AND status	=	'pending'				"	;
				
		dbService.runQuery(follow_query, getDetails -> {
					
		if(!getDetails.succeeded())
		{
			error(resp,"Could not get follow details");
			return;
		}
		
		List<String> resultList		=	getDetails.result();
		int rowCount				=	resultList.size();
			
		if(rowCount!=1)
		{
			forbidden(resp,"Follow ID is invalid or has already been approved");
			return;
		}
			
		String row				=	resultList.get(0);
				
		String	processed_row[]	=	row.substring(row.indexOf("[")+1, row.indexOf("]")).trim().split(",\\s");
			
		String from_id			=	processed_row[8];
		String exchange			=	processed_row[2];
		String permission		=	processed_row[4];
		String topic			=	processed_row[5];
		String validity			=	processed_row[6];
			
		logger.debug("from_id="+from_id);
		logger.debug("exchange="+exchange);
		logger.debug("permission="+permission);
		logger.debug("topic="+topic);
		logger.debug("validity="+validity);
							
		String from_query		=	"SELECT * FROM users WHERE id = '" + from_id + "'";
							
		dbService.runQuery(from_query, fromQuery -> {
								
		if(!fromQuery.succeeded())
		{
			error(resp,"Could not query users table");
			return;
		}
		
		logger.debug("From query ok");
			
		if(fromQuery.result().size()!=1)
		{
			forbidden(resp,"Invalid from id");
			return;
		}
		
		logger.debug("Found from entity");
				
		String update_follow_query	=	"UPDATE follow SET status = 'approved' WHERE follow_id = " + follow_id;
										
		dbService.runQuery(update_follow_query, updateFollowQuery -> {
											
		if(!updateFollowQuery.succeeded())
		{
			error(resp,"Could not update follow table");
			return;
		}
		
		logger.debug("Update follow ok");
			
		String acl_insert = "INSERT INTO acl VALUES("	+	"'"
							+from_id					+	"','"
							+exchange 					+	"','"
							+permission					+	"',"
							+"now() + interval '"		+
							Integer.parseInt(validity) 	+
							" hours'"					+	",'"
							+follow_id					+	"','"
							+topic						+	"',"
							+"DEFAULT)"					;
												
												
		dbService.runQuery(acl_insert, aclInsert -> {
													
		if(!aclInsert.succeeded())
		{
			error(resp,"Failed to insert into acl table");
			return;
		}
		
		logger.debug("Insert into acl ok");
			
		if("write".equals(permission))
		{
			logger.debug("Permission is write");
			
			String publish_exchange	=	from_id + ".publish";
			String publish_queue	=	exchange;
			String publish_topic	=	exchange + "." + topic;
															
			brokerService.bind(publish_queue, publish_exchange, publish_topic, bind -> {
																
		if(!bind.succeeded())
		{
			error(resp,"Failed to bind");
			return;
		}
		
		logger.debug("Bound. All ok");
			
		ok(resp);
		return;
			});
		}
		else
		{
			logger.debug("All ok");
			
			ok(resp);
			return;
		}	
						});
					});
				});
			});
		});	
	}
	
	public void cat(HttpServerRequest req)
	{
		HttpServerResponse resp	=	req.response();
		
		logger.debug("In cat API");
		
		String cat_query	=	"SELECT id, schema FROM users WHERE id LIKE '%/%'";
		
		dbService.runQuery(cat_query, query -> {
			
			if(!query.succeeded())
			{
				error(resp,"Could not get catalogue items");
				return;
			}
			
			List<String> resultList	=	query.result();
						
			JsonArray response		=	new JsonArray();
						
			for(String row:resultList)
			{
				String	processed_row[]	=	row.substring(row.indexOf("[")+1, row.indexOf("]")).trim().split(",\\s");
							
				logger.debug("Processed row ="+Arrays.asList(processed_row));
							
				JsonObject entity		=	new JsonObject();
				
				entity.put("id", processed_row[0]);
				entity.put("schema", new JsonObject(processed_row[1]));
				
				response.add(entity);
			}
			
			logger.debug("All ok");
			
			if(!resp.closed())
			{
				resp
				.putHeader("content-type", "application/json")
				.setStatusCode(200)
				.end(response.encodePrettily());
			}
				});
	}

	public void entities(HttpServerRequest req)
	{
		logger.debug("In entities API");
		
		HttpServerResponse resp	=	req.response();
		
		String id				=	req.getHeader("id");
		String apikey			=	req.getHeader("apikey");
		
		logger.debug("id="+id);
		logger.debug("apikey="+apikey);

		if	(	(id	==	null)
						||
				(apikey	==	null)
			)
		{
			badRequest(resp,"Inputs missing in headers");
			return;
		}
		
		if(!isValidOwner(id))
		{
			forbidden(resp,"id is not valid");
			return;
		}
		
		checkLogin(id, apikey)
		.setHandler(login -> {
			
		if(!login.succeeded())
		{
			forbidden(resp,"Invalid id or apikey");
			return;
		}
		
		logger.debug("Login ok");
				
		if(!login.result())
		{
			forbidden(resp,"Unauthorised");
			return;
		}
				
		logger.debug("Autonomous ok");
				
		String users_query	=	"SELECT * FROM users WHERE id LIKE '" + id + "/%'";
			
		dbService.runQuery(users_query, query -> {
					
		if(!query.succeeded())
		{
			error(resp,"Could not get entities' details");
			return;
		}
		
		List<String> resultList	=	query.result();
					
		JsonArray response		=	new JsonArray();
					
		for(String row:resultList)
		{
			String	processed_row[]	=	row.substring(row.indexOf("[")+1, row.indexOf("]")).trim().split(",\\s");
						
			logger.debug("Processed row ="+Arrays.asList(processed_row));
						
			JsonObject entity		=	new JsonObject();
						
			entity.put("id", processed_row[0]);
			entity.put("is-autonomous", processed_row[5]);
						
			response.add(entity);
		}
		
		logger.debug("All ok");
		
		if(!resp.closed())
		{
			resp
			.putHeader("content-type", "application/json")
			.setStatusCode(200)
			.end(response.encodePrettily());
		}
			});
		});
	}
	
	public void resetApikey(HttpServerRequest req)
	{
		logger.debug("In reset apikey");
		
		HttpServerResponse resp	=	req.response();
		
		String	id				=	req.getHeader("id");
		String 	apikey			=	req.getHeader("apikey");
		String	owner			=	req.getHeader("owner");
		String	entity			=	req.getHeader("entity");
		
		if	(	(id		==	null)
						||
				(apikey	==	null)
			)
		{
			badRequest(resp,"Inputs missing in headers");
			return;
		}
		
		logger.debug("id="+id);
		logger.debug("apikey="+apikey);
		
		if	(	(owner	==	null)
						&&
				(entity	==	null)
			)
		{
			badRequest(resp, "Inputs missing in headers");
			return;
		}
		
		if(owner != null)
		{
			if(!isStringSafe(owner))
			{
				badRequest(resp, "Invalid owner");
				return;
			}
			
			if(!("admin".equals(id)))
			{
				forbidden(resp, "Only admin can block owners");
				return;
			}
			
			if(!isValidOwner(owner))
			{
				badRequest(resp, "owner is not valid");
				return;
			}
		}
			
		else if(entity != null)
		{	
			if	(	!(
					isOwner(id, entity)
							|| 
					"admin".equals(id)
					)
				)
			{
				forbidden(resp,"You are not the owner of the entity");
				return;
			}
			
			if(!isStringSafe(entity))
			{
				badRequest(resp, "Invalid entity");
				return;
			}
			
			if(!isValidEntity(entity))
			{
				forbidden(resp,"entity is not valid");
				return;
			}
		}
		
		final String username	=	(owner==null)?entity:owner;
		
		checkLogin(id, apikey)
		.setHandler(login -> {
			
		if(!login.succeeded())
		{
			forbidden(resp,"Invalid id or apikey");
			return;
		}
		
		logger.debug("Login ok");
		
		entityExists(username)
		.setHandler(entityExists -> {
			
		if(!entityExists.succeeded())
		{
			badRequest(resp, "No such entity");
			return;
		}
			
		updateCredentials(username)
		.setHandler(update -> {
						
		if(!update.succeeded())
		{
			error(resp,"Could not reset apikey");
			return;
		}
			
		logger.debug("All ok");
							
		JsonObject response	=	new JsonObject();
							
		response.put("id", username);
		response.put("apikey", update.result());
			
		if(!resp.closed())
		{
			resp
			.putHeader("content-type", "application/json")
			.setStatusCode(200)
			.end(response.encodePrettily());
		}
				});	
			});	
		});
	}
	
	public void setAutonomous(HttpServerRequest req)
	{
		logger.debug("In set autonomous");
		
		HttpServerResponse	resp	=	req.response();
		String	id					=	req.getHeader("id");
		String	apikey				=	req.getHeader("apikey");
		String 	entity				=	req.getHeader("entity");
		String 	autonomous			=	req.getHeader("is-autonomous");
		
		logger.debug("id="+id);
		logger.debug("apikey="+apikey);
		logger.debug("entity="+entity);
		logger.debug("is-autonomous="+autonomous);
		
		if	(	(id			==	null)
							||
				(apikey		==	null)
							||
				(entity		==	null)
							||
				(autonomous	==	null)
			)
		{
			badRequest(resp,"Inputs missing in headers");
			return;
		}
		
		if (!isValidOwner(id))
		{
			forbidden(resp,"id is not valid");
			return;
		}
		
		if(!isValidEntity(entity))
		{
			forbidden(resp,"entity is not valid");
			return;
		}
		
		if	(	!(	("true".equals(autonomous))
							||
					("false".equals(autonomous))
				)
			)
		{
			badRequest(resp,"Invalid is-autonomous header");
			return;
		}
		
		if(!isOwner(id, entity))
		{
			forbidden(resp,"You are not the owner of the entity");
			return;
		}
		
		checkLogin(id,apikey)
		.setHandler(login -> {
			
		if(!login.succeeded())
		{
			forbidden(resp,"Invalid id or apikey");
			return;
		}
		
		logger.debug("Login ok");
				
		String	query	=	"UPDATE users SET is_autonomous	=	'"	+
							autonomous								+
							"' WHERE id						=	'"	+
							entity									+
							"'"										;
				
		dbService.runQuery(query, queryResult -> {
					
		if(!queryResult.succeeded())
		{
			error(resp,"Could not update is-autonomous value");
			return;
		}
		
		logger.debug("All ok");
			
		ok(resp);
		return;
			});
		});
	}
	
	public void getOwners(HttpServerRequest req)
	{
		logger.debug("In owners API");
		
		HttpServerResponse resp	=	req.response();
		String	id				=	req.getHeader("id");
		String	apikey			=	req.getHeader("apikey");
		
		logger.debug("id="+id);
		logger.debug("apikey="+apikey);
		
		if(!"admin".equals(id))
		{
			forbidden(resp);
			return;
		}
		
		checkLogin(id,apikey)
		.setHandler(login -> {
			
		if(!login.succeeded())
		{
			forbidden(resp,"Invalid id or apikey");
			return;
		}
		
		logger.debug("Login ok");
				
		String user_query	=	"SELECT * FROM users WHERE id NOT LIKE '%/%'";
				
		dbService.runQuery(user_query, query -> {
					
		if(!query.succeeded())
		{
			error(resp,"Could not get owner details");
			return;
		}
		
		List<String> list	=	query.result();
					
		JsonArray response	=	new JsonArray();
					
		for(String row:list)
		{
			String	processed_row[]	=	row.substring(row.indexOf("[")+1, row.indexOf("]")).trim().split(",\\s");
						
			logger.debug("Processed row ="+Arrays.asList(processed_row));
						
			response.add(processed_row[0]);
		}
		
		logger.debug("All ok");
		
		if(!resp.closed())
		{	
			resp
			.putHeader("content-type", "application/json")
			.setStatusCode(200)
			.end(response.encodePrettily());
		}
			});
		});
	}
	
	public void followRequests(HttpServerRequest req)
	{
		logger.debug("In follow-requests API");
		
		HttpServerResponse resp	=	req.response();
		
		String	id				=	req.getHeader("id");
		String 	apikey			=	req.getHeader("apikey");
		
		logger.debug("id="+id);
		logger.debug("apikey="+apikey);
		
		if	(	(id		==	null)
						||
				(apikey	==	null)
			)
		{
			badRequest(resp,"Inputs missing in headers");
			return;
		}
		
		if	(	!	(	(isValidOwner(id))
								^
						(isValidEntity(id))
					)	
			)
		{
			forbidden(resp,"Invalid id");
			return;
		}
		
		String exchange_string	=	isValidOwner(id)?(id+"/%.%"):(isValidEntity(id))?id+".%":"";
		
		checkLogin(id, apikey)
		.setHandler(login -> {
			
		if(!login.succeeded())
		{
			forbidden(resp,"Invalid id or apikey");
			return;
		}
		
		logger.debug("Login ok");
				
		if(!login.result())
		{
			forbidden(resp,"Unauthorised");
			return;
		}
				
		logger.debug("Autonomous ok");
				
		String follow_query	=	"SELECT * FROM follow WHERE exchange LIKE '"	+
								exchange_string 								+
								"' AND status = 'pending' ORDER BY TIME"		;
				
		dbService.runQuery(follow_query, query -> {
					
		if(!query.succeeded())
		{
			error(resp,"Could not get follow requests' details");
			return;
		}
		
		List<String>	list		=	query.result();
		JsonArray		response	=	new JsonArray();
					
		for(String row:list)
		{
			String	processed_row[]	=	row.substring(row.indexOf("[")+1, row.indexOf("]")).trim().split(",\\s");
						
			logger.debug("Processed row ="+Arrays.asList(processed_row));
						
			JsonObject temp	=	new JsonObject();
						
			temp.put("follow-id", processed_row[0]);
			temp.put("from", processed_row[1]);
			temp.put("to", processed_row[2]);
			temp.put("time", processed_row[3]);
			temp.put("permission", processed_row[4]);
			temp.put("topic", processed_row[5]);
			temp.put("validity", processed_row[6]);
						
			response.add(temp);
		}
					
		logger.debug("All ok");
		
		if(!resp.closed())
		{	
			resp
			.putHeader("content-type", "application/json")
			.setStatusCode(200)
			.end(response.encodePrettily());
		}
		
			});
		});
	}
	
	public void followStatus(HttpServerRequest req)
	{
		logger.debug("In follow-status API");
		
		HttpServerResponse resp	=	req.response();
		
		String	id				=	req.getHeader("id");
		String 	apikey			=	req.getHeader("apikey");
		
		logger.debug("id="+id);
		logger.debug("apikey="+apikey);
		
		if	(	(id		==	null)
						||
				(apikey	==	null)
			)
		{
			badRequest(resp,"Inputs missing in headers");
			return;
		}
		
		if	(	!	(	(isValidOwner(id))
								^
						(isValidEntity(id))
					)	
			)
		{
			forbidden(resp,"Invalid id");
			return;
		}
		
		String from_string	=	isValidOwner(id)?(id+"/%"):(isValidEntity(id))?id:"";
		
		checkLogin(id, apikey)
		.setHandler(login -> {
			
		if(!login.succeeded())
		{
			forbidden(resp,"Invalid id or apikey");
			return;
		}
		
		logger.debug("Login ok");
				
		if(!login.result())
		{
			forbidden(resp,"Unauthorised");
			return;
		}
				
		logger.debug("Autonomous ok");
				
		String follow_query	=	"SELECT * FROM follow WHERE from_id LIKE '"	+
								from_string 								+
								"' ORDER BY TIME"	;
				
		dbService.runQuery(follow_query, query -> {
					
		if(!query.succeeded())
		{
			error(resp,"Could not get follow requests' details");
			return;
		}
		
		List<String>	list		=	query.result();
		JsonArray		response	=	new JsonArray();
					
		for(String row:list)
		{
			String	processed_row[]	=	row.substring(row.indexOf("[")+1, row.indexOf("]")).trim().split(",\\s");
						
			logger.debug("Processed row ="+Arrays.asList(processed_row));
						
			JsonObject temp	=	new JsonObject();
						
			temp.put("follow-id", processed_row[0]);
			temp.put("from", processed_row[1]);
			temp.put("to", processed_row[2]);
			temp.put("time", processed_row[3]);
			temp.put("permission", processed_row[4]);
			temp.put("topic", processed_row[5]);
			temp.put("validity", processed_row[6]);
			temp.put("status", processed_row[7]);
						
			response.add(temp);
			}
						
			logger.debug("All ok");
			
			if(!resp.closed())
			{
				resp
				.putHeader("content-type", "application/json")
				.setStatusCode(200)
				.end(response.encodePrettily());
			}
			
			});
		});
	}
	
	public void rejectFollow(HttpServerRequest req)
	{
		logger.debug("In reject follow");
		
		HttpServerResponse resp	=	req.response();
		String	id				=	req.getHeader("id");
		String	apikey			=	req.getHeader("apikey");
		String	follow_id		=	req.getHeader("follow-id");
		
		logger.debug("id="+id);
		logger.debug("apikey="+apikey);
		logger.debug("follow-id="+follow_id);
		
		
		if	(	(id			==	null)
							||
				(apikey		==	null)
							||
				(follow_id	==	null)
			)
		{
			forbidden(resp,"Inputs missing in headers");
			return;
		}
		
		if(!isStringSafe(follow_id))
		{
			badRequest(resp,"Invalid follow-id");
			return;
		}
		
		String exchange_string	=	isValidOwner(id)?(id+"/%.%"):(isValidEntity(id))?id+".%":"";
		
		checkLogin(id,apikey)
		.setHandler(login -> {
			
		if(!login.succeeded())
		{
			forbidden(resp,"Invalid id or apikey");
			return;
		}
		
		logger.debug("Login ok");
				
		if(!login.result())
		{
			forbidden(resp,"Unauthorised");
			return;
		}
				
		logger.debug("Autonomous ok");
				
		String follow_query	=	"SELECT * FROM follow WHERE follow_id	=	'"	+
								follow_id										+
								"' AND exchange LIKE 						'"	+
								exchange_string									+
								"' AND status = 'pending'"						;
				
		dbService.runQuery(follow_query, query -> {
					
		if(!query.succeeded())
		{
			error(resp,"Could not get follow details");
			return;
		}
		
		if(query.result().size()!=1)
		{
			badRequest(resp,"Follow-id is not valid");
			return;
		}
						
		logger.debug("Follow-id is valid");
						
		String update_query	=	"UPDATE follow SET status	=	'rejected'"	+
								"WHERE follow_id			=	'"			+
								follow_id									+
								"'"											;
						
		dbService.runQuery(update_query, updateQuery -> {
							
		if(!updateQuery.succeeded())
		{
			error(resp,"Could not run update query on follow");
			return;
		}
		
		logger.debug("All ok");
								
		ok(resp);
		return;
				});
			});
		});
	}
	
	public void permissions(HttpServerRequest req)
	{
		logger.debug("In permissions");
		
		HttpServerResponse	resp	=	req.response();
		
		String	id					=	req.getHeader("id");
		String	apikey				=	req.getHeader("apikey");
		String	entity				=	req.getHeader("entity");
		
		logger.debug("id="+id);
		logger.debug("apikey="+apikey);
		
		if	(	(id		==	null)
						||
				(apikey	==	null)
			)
		{
			badRequest(resp,"Inputs missing in headers");
			return;
		}
		
		if	(	!	(	(isValidOwner(id))
								^
						(isValidEntity(id))
					)	
			)
		{
			forbidden(resp,"Invalid id");
			return;
		}
		
		if(isValidOwner(id))
		{
			if(entity	==	null)
			{
				badRequest(resp,"Entity value not specified in headers");
				return;
			}
			
			if(!isOwner(id, entity))
			{
				forbidden(resp,"You are not the owner of the entity");
				return;
			}
		}
		
		String from_id	=	(isValidOwner(id))?entity:(isValidEntity(id)?id:"");
		
		checkLogin(id,apikey)
		.setHandler(login -> {
			
		if(!login.succeeded())
		{
			forbidden(resp,"Invalid id or apikey");
			return;
		}
		
		String 	acl_query	=	"SELECT * FROM acl WHERE from_id	=	'"	+
								from_id										+
								"' AND valid_till > now()"					;
				
		dbService.runQuery(acl_query, query -> {
					
		if(!query.succeeded())
		{
			error(resp, "Could not get acl details");
			return;
		}
		
		List<String>	list		=	query.result();
		JsonArray 		response	=	new JsonArray();
					
		for(String row:list)
		{
			String	processed_row[]	=	row.substring(row.indexOf("[")+1, row.indexOf("]")).trim().split(",\\s");
						
			logger.debug("Processed row ="+Arrays.asList(processed_row));
									
			JsonObject temp	=	new JsonObject();
						
			temp.put("entity", processed_row[1]);
			temp.put("permission", processed_row[2]);
						
			response.add(temp);
		}
				
		logger.debug("All ok");
		
		if(!resp.closed())
		{	resp
			.putHeader("content-type", "application/json")
			.setStatusCode(200)
			.end(response.encodePrettily());
		}
			});
		});	
	}
	
	/**
	 * This method is the implementation of Publish API, which handles the
	 * publication request by clients.
	 * 
	 * @param HttpServerRequest event - This is the handle for the incoming request
	 *                          from client.
	 * @return HttpServerResponse resp - This sends the appropriate response for the
	 *         incoming request.
	 */
	
	public void publish(HttpServerRequest req) 
	{
		HttpServerResponse resp	=	req.response();
		String	id				=	req.getHeader("id");
		String	apikey			=	req.getHeader("apikey");
		String	to				=	req.getHeader("to");
		String 	subject			=	req.getHeader("subject");
		String 	message_type	=	req.getHeader("message-type");
		
		logger.debug("id="+id);
		logger.debug("apikey="+apikey);
		logger.debug("to="+to);
		logger.debug("subject="+subject);
		logger.debug("message-type="+message_type);
		
		if	(	(id				==	null)
								||
				(apikey			==	null)
								||
				(to				==	null)
								||
				(subject		==	null)
								||
				(message_type	==	null)
			)
		{
			badRequest(resp,"Inputs missing in headers");
			return;
		}
		
		//TODO: Add proper validation
		req.bodyHandler(body -> 
		{
			message = body.toString();
			
			logger.debug("body="+message);
			
			String temp_exchange	=	"";
			String temp_topic		=	"";
			
			if(id.equals(to))
			{
				if	(	(!"public".equals(message_type))
										&&
						(!"private".equals(message_type))
										&&
						(!"protected".equals(message_type))
										&&
						(!"diagnostics".equals(message_type))
					)
				{
					badRequest(resp,"'message-type' is invalid");
					return;
				}
				
				temp_exchange	=	id	+	"."	+	message_type;
				temp_topic		=	subject;
			}
			else
			{
				if	(!"command".equals(message_type))
				{
					badRequest(resp,"'message-type' can only be command");
					return;
				}
				
				temp_topic		=	to				+	"."	+	
									message_type	+	"."	+	
									subject					;
				
				temp_exchange	=	id	+	".publish";
			}
			
			if(!isValidEntity(to))
			{
				badRequest(resp, "'to' is not a valid entity");
				return;
			}
			
			final String exchange	=	temp_exchange;
			final String topic		=	temp_topic;
			
			logger.debug("Exchange="+exchange);
			logger.debug("Topic="+topic);
			
			brokerService.publish(id, apikey, exchange, topic, message, ar -> {
					
			if(!ar.succeeded())
			{
				logger.debug("AR Cause="+ar.cause());
				if(ar.cause().toString().contains("ACCESS_REFUSED"))
				{
					forbidden(resp);
					return;
				}
				else
				{
					error(resp, "Could not publish to broker");
					return;
				}
			}
			else
			{
				accepted(resp);
				return;
			}
		});
		});
	}
	
	public void publishAsync(HttpServerRequest req) 
	{
		HttpServerResponse resp	=	req.response();
		String	id				=	req.getHeader("id");
		String	apikey			=	req.getHeader("apikey");
		String	to				=	req.getHeader("to");
		String 	subject			=	req.getHeader("subject");
		String 	message_type	=	req.getHeader("message-type");
		
		logger.debug("id="+id);
		logger.debug("apikey="+apikey);
		logger.debug("to="+to);
		logger.debug("subject="+subject);
		logger.debug("message-type="+message_type);
		
		if	(	(id				==	null)
								||
				(apikey			==	null)
								||
				(to				==	null)
								||
				(subject		==	null)
								||
				(message_type	==	null)
			)
		{
			badRequest(resp,"Inputs missing in headers");
			return;
		}
		
		//TODO: Add proper validation
		req.bodyHandler(body -> 
		{
			message = body.toString();
			
			logger.debug("body="+message);
			
			String temp_exchange	=	"";
			String temp_topic		=	"";
			
			if(id.equals(to))
			{
				if	(	(!"public".equals(message_type))
										&&
						(!"private".equals(message_type))
										&&
						(!"protected".equals(message_type))
										&&
						(!"diagnostics".equals(message_type))
					)
				{
					badRequest(resp,"'message-type' is invalid");
					return;
				}
				
				temp_exchange	=	id	+	"."	+	message_type;
				temp_topic		=	subject;
			}
			else
			{
				if	(!"command".equals(message_type))
				{
					badRequest(resp,"'message-type' can only be command");
					return;
				}
				
				temp_topic		=	to				+	"."	+	
									message_type	+	"."	+	
									subject					;
				
				temp_exchange	=	id	+	".publish";
			}
			
			if(!isValidEntity(to))
			{
				badRequest(resp, "'to' is not a valid entity");
				return;
			}
			
			final String exchange	=	temp_exchange;
			final String topic		=	temp_topic;
			
			logger.debug("Exchange="+exchange);
			logger.debug("Topic="+topic);
			
			brokerService.publish(id, apikey, exchange, topic, message, ar -> {});
			
			accepted(resp);
			return;
		});
	}

	/**
	 * This method is the implementation of Subscribe API, which handles the
	 * subscription request by clients.apikey
	 * 
	 * @param HttpServerRequest event - This is the handle for the incoming request
	 *                          from client.
	 * @return void - Though the return type is void, the HTTP response is written internally as per the request. 
	 */
	
	public void subscribe(HttpServerRequest req) 
	{
		
		HttpServerResponse	resp	=	req.response();
		
		//Mandatory headers
		String	id 					=	req.getHeader("id");
		String 	apikey 				= 	req.getHeader("apikey");
		
		//Optional headers
		String	message_type_header	=	req.getHeader("message-type");
		String	num_messages		=	req.getHeader("num-messages");
		
		if	(	id		==	null	
						||
				apikey	==	null
			)
		{
			badRequest(resp, "Inputs missing in headers");
			return;
		}
		
		if(message_type_header	!=	null)
		{
			if	(	(!"priority".equals(message_type_header))
									&&
					(!"command".equals(message_type_header))
									&&
					(!"notification".equals(message_type_header))
									&&
					(!"private".equals(message_type_header))
				)
			{
				badRequest(resp,"'message-type' is invalid");
				return;
			}
			
			message_type_header	=	"."	+	message_type_header;
			
		}
		else
		{
			message_type_header	=	"";
		}
		
		if(num_messages	!=	null)
		{
			int messages	=	0;
			
			try
			{
				messages	=	Integer.parseInt(num_messages);
			}
			catch(Exception e)
			{
				badRequest(resp, "Invalid num-messages header");
				return;
			}
			
			if(messages<0)
			{
				badRequest(resp, "Invalid num-messages header");
				return;
			}
			
			if(messages>1000)
			{
				num_messages="1000";
			}
		}
		else
		{
			num_messages	=	"10";
		}
		
		final String message_type	=	message_type_header;
		final int message_count		=	Integer.parseInt(num_messages);
		
		if(pool.get(id+apikey)==null)
		{
			checkLogin(id, apikey)
			.setHandler(login -> {
				
				if(!login.succeeded())
				{
					forbidden(resp);
					return;
				}
				
				pool.put(id+apikey, "1");
				
				brokerService.subscribe(id, apikey, message_type, message_count, result -> {
					
					if(!result.succeeded())
					{
						error(resp, "Could not get messages");
						return;
					}
					
					if(!resp.closed())
					{	
						resp
						.putHeader("content-type", "application/json")
						.setStatusCode(200)
						.end(result.result().encodePrettily());
					}
				});
			});
		}
		else
		{
			brokerService.subscribe(id, apikey, message_type, message_count, result -> {
			
			if(!result.succeeded())
			{
				error(resp, "Could not get messages");
				return;
			}
			
			if(!resp.closed())
			{	
				resp
				.putHeader("content-type", "application/json")
				.setStatusCode(200)
				.end(result.result().encodePrettily());
				
			}
		});
		}
		
		return;
	}
	
	/**
	 * This method is used to verify if the requested registration entity is already
	 * registered.
	 * 
	 * @param String registration_entity_id - This is the handle for the incoming
	 *               request header (entity) from client.
	 * @return Future<String> verifyentity - This is a callable Future which notifies
	 *         on completion.
	 */
	
	public Future<Boolean> entityExists(String entity_id) 
	{
		logger.debug("In entity does not exist");
		
		Future<Boolean> future	=	Future.future();		
		String query		=	"SELECT * FROM users WHERE id	=	'"	+ 
								entity_id 								+	
								"'" 									;
		
		dbService.runQuery(query, reply -> {
			
		if(reply.succeeded())
		{	
			List<String> list	=	reply.result();
			int rowCount		=	list.size();
			
			if(rowCount==1)
			{
				String raw	=	list.get(0);
				
				String 	processed_row[]	=	raw.substring(raw.indexOf("[")+1, raw.indexOf("]"))
											.trim()
											.split(",\\s");
				
				boolean autonomous		=	"true".equals(processed_row[5])?true:false;
				
				future.complete(autonomous);
			}
			else
			{
				future.fail("Entity does not exist");
			}		
		}
	});
		return future;
	}
	
	public Future<Void> entityDoesNotExist(String registration_entity_id) 
	{
		logger.debug("in entity does not exist");
		
		Future<Void> future		=	Future.future();		
		String query			=	"SELECT * FROM users WHERE id = '"
									+ registration_entity_id +	"'";
		
		dbService.runQuery(query, reply -> {
			
		if(reply.succeeded())
		{
			List<String> resultList	=	reply.result();
			int rowCount			=	resultList.size();
		
			if(rowCount==1)
			{
				future.fail("Entity exists");	
			}
			else
			{
				future.complete();
			}
				
		}
		else
		{
			logger.debug(reply.cause());
			future.fail("Could not get entity details");
		}
	});
		return future;
	}
	
	public boolean isOwner(String owner, String entity)
	{
		logger.debug("In is_owner");
		
		logger.debug("Owner="+owner);
		logger.debug("Entity="+entity);
		
		if	(	(isValidOwner(owner))
						&&
				(entity.startsWith(owner))
						&&
				(entity.contains("/"))
			)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public Future<String> generateCredentials(String id, String schema, String autonomous) 
	{
		logger.debug("In generate credentials");
		
		Future<String> future	= Future.future();
		
		String apikey			=	genRandString(32);
		String salt 			=	genRandString(32);
		String blocked 			=	"f";
		
		String string_to_hash	=	apikey + salt + id;
		String hash				=	Hashing.sha256()
									.hashString(string_to_hash, StandardCharsets.UTF_8)
									.toString();
		
		logger.debug("Id="+id);
		logger.debug("Generated apikey="+apikey);
		logger.debug("Salt="+salt);
		logger.debug("String to hash="+string_to_hash);
		logger.debug("Hash="+hash);
		
		String query			=	"INSERT INTO users VALUES('"
									+id			+	"','"
									+hash		+	"','"
									+schema 	+	"','"
									+salt		+ 	"','"
									+blocked	+	"','"
									+autonomous +	"')";
		
		dbService.runQuery(query, reply -> {
			
		if(reply.succeeded())
		{
			logger.debug("Generate credentials query succeeded");
			future.complete(apikey);
		}
		else
		{
			logger.debug("Failed to run query. Cause="+reply.cause());
			future.fail(reply.cause().toString());
		}
		
	});
		
		return future;
	}
	
	public Future<String> updateCredentials(String id)
	{
		logger.debug("In update credentials");
		
		Future<String> future	=	Future.future();

		String apikey			=	genRandString(32);
		String salt 			=	genRandString(32);
		
		String string_to_hash	=	apikey + salt + id;
		String hash				=	Hashing.sha256()
									.hashString(string_to_hash, StandardCharsets.UTF_8)
									.toString();
		
		logger.debug("Id="+id);
		logger.debug("Generated apikey="+apikey);
		logger.debug("Salt="+salt);
		logger.debug("String to hash="+string_to_hash);
		logger.debug("Hash="+hash);
		
		String update_query		=	"UPDATE users SET password_hash	=	'" 
									+	hash	+	"',	salt	=		'" 
									+ 	salt	+	"' WHERE id	=		'"
									+	id		+	"'"					;														
		
		dbService.runQuery(update_query, reply -> {
			
		if(reply.succeeded())
		{
			logger.debug("Update credentials query succeeded");
			future.complete(apikey);
		}
		else
		{
			logger.debug("Failed to run query. Cause="+reply.cause());
			future.fail(reply.cause().toString());
		}
	});
		
		return future;
}
	
	public String genRandString(int len)
	{
		logger.debug("In genRandString");
		
		String randStr	=	RandomStringUtils
							.random(len, 0, PASSWORDCHARS.length(), 
							true, true, PASSWORDCHARS.toCharArray());
		
		logger.debug("Generated random string = "+randStr);
		
		return randStr;
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
		
		boolean safe = (resource.length() - (resource.replaceAll("[^#-/a-zA-Z0-9]+", "")).length())==0?true:false;
		
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
