package sk.tuke.gamestudio.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "comment")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // meno hraca, ktory pridal komentar
    private String player;

    // kluc hry alebo levelu, napr. logicalmazes_level_1
    private String game;

    // text komentara je v DB v stlpci comment
    @Column(name = "comment")
    private String comment;

    // datum a cas pridania komentara
    @Column(name = "commentedon", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date commentedOn;

    @PrePersist
    public void setDefaultCommentedOn() {
        // Ak by sa datum neposlal z kontrolera, JPA ho doplni pred ulozenim.
        if (commentedOn == null) {
            commentedOn = new Date();
        }
    }

    public Comment(String player, String game, String comment, Date commentedOn) {
        // Konstruktor pouzivam pri vytvarani komentara v kode.
        this.player = player;
        this.game = game;
        this.comment = comment;
        this.commentedOn = commentedOn;
    }

    public Comment() {
        // prazdny konstruktor pre JPA
    }

    public String getPlayer() {
        return player;
    }

    public String getGame() {
        return game;
    }

    public String getComment() {
        return comment;
    }

    public Date getCommentedOn() {
        return commentedOn;
    }
}
