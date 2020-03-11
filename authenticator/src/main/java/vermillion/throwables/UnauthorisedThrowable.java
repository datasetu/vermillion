package vermillion.throwables;

public final class UnauthorisedThrowable extends Throwable {
  public UnauthorisedThrowable(String message) {
    super(message, null, false, false);
  }
}
