package sk.tuke.gamestudio.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "rating")
@IdClass(Rating.RatingId.class)
public class Rating {
    @Id
    // meno hraca je cast primarneho kluca
    private String player;

    @Id
    // game je druha cast primarneho kluca
    private String game;

    // cislo hodnotenia od 1 do 5
    private int rating;

    // datum a cas posledneho hodnotenia
    @Column(name = "ratedon", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date ratedOn;

    @PrePersist
    public void setDefaultRatedOn() {
        // Ak by sa datum neposlal z kontrolera, JPA ho doplni pred ulozenim.
        if (ratedOn == null) {
            ratedOn = new Date();
        }
    }

    public Rating(String player, String game, int rating, Date ratedOn) {
        // Konstruktor pouzivam pri vytvarani ratingu v kode.
        this.player = player;
        this.game = game;
        this.rating = rating;
        this.ratedOn = ratedOn;
    }

    public Rating() {
        // prazdny konstruktor pre JPA
    }

    public String getPlayer() {
        return player;
    }

    public String getGame() {
        return game;
    }

    public int getRating() {
        return rating;
    }

    public Date getRatedOn() {
        return ratedOn;
    }

    // pomocna trieda pre composite PK
    public static class RatingId implements Serializable {
        // Musi mat rovnake polia ako @Id polia v Rating.
        private String player;
        private String game;

        public RatingId() {
        }

        public RatingId(String player, String game) {
            this.player = player;
            this.game = game;
        }

        @Override
        public boolean equals(Object o) {
            // Dva kluce su rovnake, ked maju rovnakeho hraca aj rovnaku hru.
            if (this == o) return true;
            if (!(o instanceof RatingId)) return false;
            RatingId ratingId = (RatingId) o;
            return Objects.equals(player, ratingId.player) && Objects.equals(game, ratingId.game);
        }

        @Override
        public int hashCode() {
            // hashCode musi sediet s equals, preto pouzivam tie iste polia.
            return Objects.hash(player, game);
        }
    }
}
