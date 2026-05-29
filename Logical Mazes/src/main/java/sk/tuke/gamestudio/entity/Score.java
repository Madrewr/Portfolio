package sk.tuke.gamestudio.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "score")
public class Score {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // meno hraca, ktory ulozil skore
    private String player;

    // kluc hry, napr. logicalmazes_level_1
    private String game;

    // body su v mojej hre pocet tahov
    private int points;

    // datum a cas, kedy bolo skore ulozene
    @Column(name = "playedon", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date playedOn;

    @PrePersist
    public void setDefaultPlayedOn() {
        // Ak by sa datum neposlal z kontrolera, JPA ho doplni pred ulozenim.
        if (playedOn == null) {
            playedOn = new Date();
        }
    }

    public Score(String player, String game, int points, Date playedOn) {
        // Konstruktor pouzivam pri ukladani skore po vyrieseni levelu.
        this.player = player;
        this.game = game;
        this.points = points;
        this.playedOn = playedOn;
    }

    public Score() {
        // prazdny konstruktor pre JPA
    }

    public String getPlayer() {
        return player;
    }

    public String getGame() {
        return game;
    }

    public int getPoints() {
        return points;
    }

    public Date getPlayedOn() {
        return playedOn;
    }

    public int getLevel() {
        // Level sa ziska z nazvu hry, napr. logicalmazes_level_9 -> 9.
        if (game == null) return 0;

        // Hladam posledne podciarknutie, lebo za nim je cislo levelu.
        int lastUnderscore = game.lastIndexOf('_');
        if (lastUnderscore < 0 || lastUnderscore == game.length() - 1) return 0;

        try {
            // Thymeleaf potom moze pouzit s.level bez parsovania v sablone.
            return Integer.parseInt(game.substring(lastUnderscore + 1));
        } catch (NumberFormatException e) {
            // Ak by nazov hry nebol v spravnom tvare, vratim 0 namiesto chyby.
            return 0;
        }
    }
}
