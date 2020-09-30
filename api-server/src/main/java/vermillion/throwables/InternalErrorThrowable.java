package vermillion.throwables;

public final class InternalErrorThrowable extends Throwable {
    public InternalErrorThrowable(String message) {
        super(message, null, false, false);
    }
}
