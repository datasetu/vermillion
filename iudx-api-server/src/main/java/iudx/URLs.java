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
	private final static Logger logger = LoggerFactory.getLogger(DbServiceImpl.class);

	public static final String broker_username = "admin";
	public static final String broker_password = System.getenv("ADMIN_PWD");
	public static final int broker_port = 5672;
	public static final String broker_vhost = "/";
	public static final String psql_database_url = "postgres";
	public static final String psql_database_username = "postgres";
	public static final String psql_database_password = System.getenv("POSTGRES_PWD");
	public static final int psql_database_port = 5432;
	public static final String psql_database_name = "postgres";
	
	public static int index = 0;
	
	public static int broker_nodes;
	public static boolean single_node;
	public static boolean federated;
	public static boolean clustered;
	
	static
	{
		if("true".equals(System.getenv("SINGLE_NODE"))) 
			single_node=true;
		
		else if(System.getenv("BROKERS_IN_SWARM")!=null) 
		{
			broker_nodes = Integer.parseInt(System.getenv("BROKERS_IN_SWARM"));
			federated=true;
		}
		
		else if(System.getenv("BROKERS_IN_CLUSTER")!=null) 
		{
			broker_nodes = Integer.parseInt(System.getenv("BROKERS_IN_CLUSTER"));
			clustered=true;
		}
		
		logger.debug("Single node?"+single_node);
		logger.debug("Broker nodes="+broker_nodes);
		
	}
		
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
	public static String getBrokerUrl(String username) 
	{
		String broker_url = "";
		
		if(single_node)
		{
			//Single node deployment
			logger.debug("In single node");
			broker_url = "rabbit";
		}
		else if(federated)
		{
			/** Using a simple hash function m % n to distribute load between rabbitmq nodes.
			 *  There are 26 characters and "n" broker nodes in the swarm. If we assign a=1, 
			 *  b=2, c=3 and so on, this operation becomes straightforward. Thus to achieve this,
			 *  we take the ASCII representation of the first letter of the username and subtract 
			 *  it with 96 (since lowercase ASCII numbers start from 97 and we want the index to 
			 *  start from 1.) Once we have the index, perform the mod operation to get the bucket 
			 *  index. Since this operation is consistent across all broker operations, there is 
			 *  a strong guarantee that we exactly resolve to the appropriate broker node under 
			 *  all circumstances.
			 *  
			 *  For e.g. if the swarm cluster has 6 brokers in it and a users named "alpha" and 
			 *  "quebec" wish to operate on the broker then, for the user "alpha":
			 *   
			 *  (int)username[0] = 97 (ASCII representation of a)
			 *  97 - 96 = 1
			 *  1  %  6 = 1
			 *    
			 *  Thus, the user "alpha" is connected with "rabbit1"
			 *    
			 *  Now for the user "quebec",
			 *  
			 *  (int)username[0] = 113
			 *  113 - 96 = 17
			 *  17 % 6 = 5
			 *  
			 *  Likewise, the user "quebec" is connected with rabbit5
			 *  
			 *  If the number of broker nodes is a factor of the integer representation of the 
			 *  character, then the value of "broker_nodes" is taken as the bucket number. 
			 *  For e.g. for the user "romeo", the bucket number resolves to 0, thus we add 6
			 *  to it and connect them to rabbit6
			 */
			
			int bucket_number	=	((int)username.charAt(0)-96)%broker_nodes;
			logger.debug(bucket_number);
			int broker_bucket	=	(bucket_number==0)?broker_nodes:bucket_number;
			logger.debug(broker_bucket);
			broker_url 			=	"rabbit" + String.valueOf(broker_bucket);
			logger.debug(broker_url);
		}
		else if(clustered)
		{				
			index = index % broker_nodes;
			
			String url = "rabbit" + String.valueOf((index++)+1); 
			logger.debug("Broker url ="+url);
			
			return url; 
			
		}
		
		return broker_url;
	}
	public static String getBrokerUsername() {
		return broker_username;
	}
	public static String getBrokerPassword() {
		return broker_password;
	}
	public static int getBrokerPort() {
		return broker_port;
	}
	public static String getBrokerVhost() {
		return broker_vhost;
	}
}

