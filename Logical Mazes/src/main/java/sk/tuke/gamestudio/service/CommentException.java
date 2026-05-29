package sk.tuke.gamestudio.service;

public class CommentException extends RuntimeException {
    public CommentException(String message, Throwable cause) {
        // vynimka s povodnou pricinou
        super(message, cause);
    }

    public CommentException(String message) {
        // jednoducha vynimka iba so spravou
        super(message);
    }
}
