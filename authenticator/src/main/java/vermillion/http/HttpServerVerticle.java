package vermillion.http;

import com.google.common.hash.Hashing;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import vermillion.database.reactivex.DbService;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;


/**
 * <h1>IUDX API Server</h1> An Open Source implementation of India Urban Data
 * Exchange (IUDX) platform APIs using Vert.x, an event driven and non-blocking
 * high performance reactive framework, for enabling seamless data exchange in
 * Smart Cities.
 *
 * @author Robert Bosch Centre for Cyber-Physical Systems (iudx <at> rbccps <dot> org)
 * @version 1.0.0
 */

public class HttpServerVerticle extends AbstractVerticle {
	public final static Logger logger = LoggerFactory.getLogger(HttpServerVerticle.class);

	//Service proxies
	public DbService dbService;

	@Override
	public void start(Promise < Void > promise) {
		logger.debug("In start");

		int port = 80;

		dbService = vermillion.database.DbService.createProxy(vertx.getDelegate(), "db.queue");

		Router router = Router.router(vertx);

		router.get("/auth/user").handler(this::authUser);
		router.get("/auth/vhost").handler(this::authVhost);
		router.get("/auth/topic").handler(this::authTopic);
		router.get("/auth/resource").handler(this::authResource);

		vertx
				.createHttpServer()
				.requestHandler(router)
				.rxListen(80)
				.subscribe(s -> {
							logger.debug("Server started");
							promise.complete();
						},
						err -> {
							logger.debug("Could not start server. Cause=" + err.getMessage());
							promise.fail(err.getMessage());
						});

		vertx.exceptionHandler(err -> {
			err.printStackTrace();
		});
	}

	public void authUser(RoutingContext context) {

		HttpServerRequest request = context.request();
		HttpServerResponse resp = request.response();
		String username = request.getParam("username");
		String password = request.getParam("password");

		if ((!isStringSafe(username)) || (!isStringSafe(password))) {
			logger.debug("invalid entity name");
			ok(resp, "deny");
			return;
		}

		if (username.length() >= 65) {
			logger.debug("long username");
			badRequest(resp);
			return;
		}

		checkLogin(username, password)
				.doOnError(err -> ok(resp, "deny"))
				.andThen((CompletableSource) user -> {
					if ("admin".equals(username)) {
						ok(resp, "allow administrator management");
						return;
					}
					ok(resp, "allow");
				})
				.subscribe();
	}

	public void authVhost(RoutingContext context) {
		HttpServerResponse resp = context.response();
		ok(resp, "allow");
		return;
	}

	public void authTopic(RoutingContext context) {
		HttpServerResponse resp = context.response();
		ok(resp, "allow");
		return;
	}

	public void authResource(RoutingContext context) {
		HttpServerRequest request = context.request();
		HttpServerResponse resp = request.response();
		String username = request.getParam("username");
		String resource = request.getParam("resource");
		String name = request.getParam("name");
		String permission = request.getParam("permission");

		logger.debug(name);
		logger.debug(username);
		logger.debug(resource);
		logger.debug(permission);

		if ("admin".equals(username)) {
			ok(resp, "allow");
			return;
		}

		if ("configure".equals(permission)) {
			forbidden(resp, "deny");
			return;
		}

		if (username.length() < 7 || username.length() > 65) {
			forbidden(resp, "deny");
			return;
		}

		if (isValidOwner(name)) {
			forbidden(resp, "deny");
			return;
		}

		if ("queue".equals(resource)) {
			if ("write".equals(permission)) {
				logger.debug("permission is write");
				forbidden(resp, "deny");
				return;
			}

			if (!name.startsWith(username)) {
				logger.debug("name does not start with username");
				forbidden(resp, "deny");
				return;
			}

			if (isValidOwner(username)) {
				logger.debug("user is an owner");
				if (name.equals(username + ".notification")) {
					ok(resp, "allow");
					return;
				}
			} else {
				if (name.equals(username) ||
						name.equals(username + ".priority") ||
						name.equals(username + ".command")
				) {
					ok(resp, "allow");
					return;
				}
			}
		} else if (("exchange".equals(resource)) ||
				("topic".equals(resource))
		) {
			if ("read".equals(permission)) {
				ok(resp, "allow");
				return;
			}

			if (isValidOwner(username)) {
				forbidden(resp, "deny");
				return;
			}

			if (name.startsWith(username) && name.contains(".")) {
				if (    name.endsWith(".public") ||
						name.endsWith(".private") ||
						name.endsWith(".protected") ||
						name.endsWith(".diagnostics") ||
						name.endsWith(".publish")
				) {
					ok(resp, "allow");
					return;
				} else {
					forbidden(resp, "deny");
					return;
				}
			}
		}
	}

	public Completable checkLogin(String id, String apikey) {
		logger.debug("In check_login");

		logger.debug("ID=" + id);
		logger.debug("Apikey=" + apikey);

		if ("".equals(id) || "".equals(apikey)) {
			return Completable.error(new UnauthorisedThrowable("ID or Apikey is missing"));
		}

		if (isValidOwner(id) == isValidEntity(id)) {
			return Completable.error(new UnauthorisedThrowable("User is neither an owner nor an entity"));
		}

		String query = "SELECT * FROM users WHERE id	=	'" +
				id + "'" +
				"AND blocked = 'f'";

		return dbService.rxRunQuery(query)
				.map(row -> row.get(0))
				.doOnError(e -> Completable.error(new UnauthorisedThrowable("No such user")))
				.map(row -> Arrays.asList(row.substring(row.indexOf("[") + 1, row.indexOf("]")).split(",\\s")))
				.map(row -> {
					String salt = row.get(3);
					String string_to_hash = apikey + salt + id;
					String expected_hash = row.get(1);
					String actual_hash = Hashing
							.sha256()
							.hashString(string_to_hash, StandardCharsets.UTF_8)
							.toString();
					return expected_hash.equals(actual_hash);
				})
				.flatMapCompletable(login -> login ? Completable.complete() : Completable.error(new UnauthorisedThrowable("Invalid credentials")));
	}

	public boolean isStringSafe(String resource) {
		logger.debug("In is_string_safe");

		logger.debug("resource=" + resource);

		boolean safe = (resource.length() - (resource.replaceAll("[^#-/a-zA-Z0-9_]+", "")).length()) == 0;

		logger.debug("Original resource name =" + resource);
		logger.debug("Replaced resource name =" + resource.replaceAll("[^#-/a-zA-Z0-9]+", ""));
		return safe;
	}

	public boolean isValidOwner(String owner_name) {
		logger.debug("In is_valid_owner");

		//TODO simplify this
		if ((!Character.isDigit(owner_name.charAt(0))) &&
				((owner_name.length() - (owner_name.replaceAll("[^a-z0-9]+", "")).length()) == 0)
		) {
			logger.debug("Original owner name = " + owner_name);
			logger.debug("Replaced name = " + owner_name.replaceAll("[^a-z0-9]+", ""));
			return true;
		} else {
			logger.debug("Original owner name = " + owner_name);
			logger.debug("Replaced name = " + owner_name.replaceAll("[^a-z0-9]+", ""));
			return false;
		}
	}

	public boolean isValidEntity(String resource) {
		//TODO: Add a length check
		logger.debug("In is_valid_entity");

		String[] entries = resource.split("/");

		logger.debug("Entries = " + Arrays.asList(entries));

		if (entries.length != 2) {
			return false;
		} else return (isValidOwner(entries[0])) &&
				(isStringSafe(entries[1]));
	}

	public void forbidden(HttpServerResponse resp, String message) {
		if (!resp.closed()) {
			resp.setStatusCode(403).end(message);
		}
		return;
	}

	public void ok(HttpServerResponse resp, String message) {
		if (!resp.closed()) {
			resp.setStatusCode(200).end(message);
		}

		return;
	}

	public void badRequest(HttpServerResponse resp) {
		if (!resp.closed()) {
			resp.setStatusCode(400).end();
		}

		return;
	}
}

final class UnauthorisedThrowable extends Throwable {
	UnauthorisedThrowable(String message) {
		super(message, null, false, false);
	}
}