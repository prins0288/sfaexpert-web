package in.opt.sfa.exception;

/** A database server could not be reached; the message is safe to show the user ("Server 2 is not responding …"). */
public class ServerUnavailableException extends RuntimeException {
    public ServerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
