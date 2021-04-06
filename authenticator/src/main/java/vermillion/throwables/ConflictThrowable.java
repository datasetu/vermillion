package vermillion.throwables;

import io.vertx.core.json.JsonObject;

public final class ConflictThrowable extends Throwable {
  public ConflictThrowable(String message) {
    super(
        new JsonObject().put("status", "error").put("message", message).encode(),
        null,
        false,
        false);
  }
}
