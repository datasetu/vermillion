package vermillion.throwables;

public final class ConflictThrowable extends Throwable {
    public ConflictThrowable(String message) {
        super(message, null, false, false);
    }
}
