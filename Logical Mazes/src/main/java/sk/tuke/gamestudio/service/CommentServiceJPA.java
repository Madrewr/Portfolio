package sk.tuke.gamestudio.service;

import sk.tuke.gamestudio.entity.Comment;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.List;

@Transactional
public class CommentServiceJPA implements CommentService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void addComment(Comment comment) throws CommentException {
        // persist vlozi novy komentar do databazy
        entityManager.persist(comment);
    }

    @Override
    public List<Comment> getComments(String game) throws CommentException {
        // Komentare nacitavam iba pre aktualny level hry.
        // Najnovsie komentare chcem hore, preto ORDER BY commentedOn DESC.
        return entityManager.createQuery(
                        "SELECT c FROM Comment c WHERE c.game = :game ORDER BY c.commentedOn DESC",
                        Comment.class
                )
                .setParameter("game", game)
                // Na stranke staci poslednych 10 komentarov.
                .setMaxResults(10)
                .getResultList();
    }

    @Override
    public void reset() throws CommentException {
        // Pomocna metoda na vymazanie komentarov pri testovani.
        entityManager.createQuery("DELETE FROM Comment").executeUpdate();
    }
}
