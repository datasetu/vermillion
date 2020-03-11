package vermillion.throwables;

public final class BadRequestThrowable extends Throwable {
  public BadRequestThrowable(String message) {
    super(message, null, false, false);
  }
}
