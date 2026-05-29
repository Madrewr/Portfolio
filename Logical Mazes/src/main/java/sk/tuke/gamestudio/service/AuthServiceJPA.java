package sk.tuke.gamestudio.service;

import sk.tuke.gamestudio.entity.PlayerAccount;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;

@Transactional
public class AuthServiceJPA implements AuthService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public PlayerAccount register(String username, String password) {
        // Najprv si upravim vstupy, aby v DB neboli mena s medzerami.
        String cleanUsername = cleanUsername(username);

        // Validacia je tu v service, lebo plati pre web aj pre pripadne testy.
        validateRegistration(cleanUsername, password);

        // Ak uz hrac existuje, nechcem vytvorit duplicitny ucet.
        if (getAccount(cleanUsername) != null) {
            throw new AuthException("Toto meno je uz registrovane.");
        }

        // Heslo sa ulozi ako SHA-256 hash, nie ako povodny text.
        PlayerAccount account = new PlayerAccount(cleanUsername, hashPassword(password), new Date());
        entityManager.persist(account);
        return account;
    }

    @Override
    public PlayerAccount login(String username, String password) {
        // Login pouziva rovnake upravenie mena ako registracia.
        String cleanUsername = cleanUsername(username);

        if (cleanUsername.isEmpty() || password == null || password.isEmpty()) {
            throw new AuthException("Zadaj meno aj heslo.");
        }

        PlayerAccount account = getAccount(cleanUsername);

        // Ak ucet neexistuje alebo nesedi heslo, vratim jednu vseobecnu spravu.
        if (account == null || !account.getPasswordHash().equals(hashPassword(password))) {
            throw new AuthException("Nespravne meno alebo heslo.");
        }

        return account;
    }

    @Override
    public PlayerAccount getAccount(String username) {
        String cleanUsername = cleanUsername(username);

        if (cleanUsername.isEmpty()) {
            return null;
        }

        try {
            // Username je unikatny, preto ocakavam najviac jeden vysledok.
            return entityManager
                    .createQuery("select a from PlayerAccount a where lower(a.username) = lower(:username)", PlayerAccount.class)
                    .setParameter("username", cleanUsername)
                    .getSingleResult();
        } catch (NoResultException e) {
            // Pri nenajdenom ucte nechcem chybu, iba null.
            return null;
        }
    }

    private void validateRegistration(String username, String password) {
        // Meno hraca ma byt kratke a citatelne, lebo sa zobrazuje v rebricku.
        if (username.isEmpty()) {
            throw new AuthException("Zadaj meno.");
        }

        if (username.length() < 3 || username.length() > 30) {
            throw new AuthException("Meno musi mat 3 az 30 znakov.");
        }

        if (!username.matches("[A-Za-z0-9_]+")) {
            throw new AuthException("Meno moze obsahovat iba pismena, cisla a podciarknik.");
        }

        if (password == null || password.length() < 4) {
            throw new AuthException("Heslo musi mat aspon 4 znaky.");
        }
    }

    private String cleanUsername(String username) {
        // Null z formulara nechcem riesit v kazdej metode zvlast.
        return username == null ? "" : username.trim();
    }

    private String hashPassword(String password) {
        try {
            // SHA-256 je jednoduche riesenie bez dalsich kniznic.
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 je standard v Jave, toto by realne nemalo nastat.
            throw new AuthException("Heslo sa nepodarilo spracovat.");
        }
    }
}
