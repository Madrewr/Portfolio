package sk.tuke.gamestudio.service;

public class AuthException extends RuntimeException {

    public AuthException(String message) {
        // Vlastna vynimka, aby controller vedel zobrazit normalnu spravu na stranke.
        super(message);
    }
}
