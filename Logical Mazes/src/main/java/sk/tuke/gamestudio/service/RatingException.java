package sk.tuke.gamestudio.service;

public class RatingException extends RuntimeException {
    public RatingException(String message, Throwable cause) {
        // vynimka s povodnou pricinou
        super(message, cause);
    }

    public RatingException(String message) {
        // jednoducha vynimka iba so spravou
        super(message);
    }
}
