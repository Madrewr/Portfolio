package sk.tuke.gamestudio.service;

public class ScoreException extends RuntimeException {
    public ScoreException(String message, Throwable cause) {
        // vynimka s povodnou pricinou
        super(message, cause);
    }

    public ScoreException(String message) {
        // jednoducha vynimka iba so spravou
        super(message);
    }
}
