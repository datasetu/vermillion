package vermillion;

public class Utils {
  public static final String psql_database_url = "postgres";
  public static final String psql_database_username = "postgres";
  public static final String psql_database_password = System.getenv("POSTGRES_PWD");
  public static final int psql_database_port = 5432;
  public static final String psql_database_name = "postgres";
}
