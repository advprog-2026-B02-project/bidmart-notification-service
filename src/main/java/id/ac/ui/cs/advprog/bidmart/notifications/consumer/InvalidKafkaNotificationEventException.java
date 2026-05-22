package id.ac.ui.cs.advprog.bidmart.notifications.consumer;

public class InvalidKafkaNotificationEventException extends RuntimeException {
    public InvalidKafkaNotificationEventException(String message) {
        super(message);
    }

    public InvalidKafkaNotificationEventException(String message, Throwable cause) {
        super(message, cause);
    }
}