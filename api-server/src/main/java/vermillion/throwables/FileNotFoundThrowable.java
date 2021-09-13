package vermillion.throwables;

import io.vertx.core.json.JsonObject;

public class FileNotFoundThrowable extends Throwable {
    public FileNotFoundThrowable(String message) {
        super(
                new JsonObject().put("status", "error").put("message", message).encode(),
                null,
                false,
                false);
    }
}
