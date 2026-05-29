package sk.tuke.gamestudio.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "player_account")
public class PlayerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true, length = 40)
    private String username;

    @Column(name = "passwordhash", nullable = false, length = 120)
    private String passwordHash;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "registeredon", nullable = false)
    private Date registeredOn;

    public PlayerAccount() {
        // Prazdny konstruktor potrebuje JPA pri nacitani objektu z databazy.
    }

    public PlayerAccount(String username, String passwordHash, Date registeredOn) {
        // Konstruktor pouzivam pri registracii noveho hraca.
        this.username = username;
        this.passwordHash = passwordHash;
        this.registeredOn = registeredOn;
    }

    @PrePersist
    public void setDefaultRegisteredOn() {
        // Ak by sa datum neposlal zo service, JPA ho doplni pred ulozenim.
        if (registeredOn == null) {
            registeredOn = new Date();
        }
    }

    public int getId() {
        // Id sa vytvori automaticky v databaze.
        return id;
    }

    public String getUsername() {
        // Username sa potom pouziva aj ako meno hraca pri score, komentaroch a ratingu.
        return username;
    }

    public String getPasswordHash() {
        // Do databazy neukladam povodne heslo, ale iba hash.
        return passwordHash;
    }

    public Date getRegisteredOn() {
        // Datum registracie je hlavne informacny udaj.
        return registeredOn;
    }
}
