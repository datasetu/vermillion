/**
 * 
 */
package iudx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import iudx.database.DbServiceImpl;

/**
 * @author Swaminathan Vasanth Rajaraman <swaminathanvasanth.r@gmail.com>
 *
 */
public class URLs 
{
	public static final String psql_database_url = "postgres";
	public static final String psql_database_username = "postgres";
	public static final String psql_database_password = System.getenv("POSTGRES_PWD");
	public static final int psql_database_port = 5432;
	public static final String psql_database_name = "postgres";
	
	private final static Logger logger = LoggerFactory.getLogger(DbServiceImpl.class);
		
	public static String getPsqlDatabaseUrl() {
		return psql_database_url;
	}
	public static String getPsqlDatabaseUsername() {
		return psql_database_username;
	}
	public static String getPsqlDatabasePassword() {
		return psql_database_password;
	}
	public static int getPsqlDatabasePort() {
		return psql_database_port;
	}
}
