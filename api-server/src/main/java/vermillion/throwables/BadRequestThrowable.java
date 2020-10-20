package vermillion.throwables;

import io.vertx.core.json.JsonObject;

public final class BadRequestThrowable extends Throwable {
  public BadRequestThrowable(String message) {
    super(
        new JsonObject().put("status", "error").put("message", message).encode(),
        null,
        false,
        false);
  }
}
