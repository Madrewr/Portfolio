package sk.tuke.gamestudio.service;

import sk.tuke.gamestudio.entity.Rating;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

@Transactional
public class RatingServiceJPA implements RatingService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void setRating(Rating rating) throws RatingException {
        // Rating mimo rozsah 1-5 nechcem ukladat.
        if (rating.getRating() < 1 || rating.getRating() > 5) {
            throw new RatingException("Rating musi byt 1-5");
        }

        // merge = insert alebo update, ked existuje rovnaky player + game.
        entityManager.merge(rating);
    }

    @Override
    public int getAverageRating(String game) throws RatingException {
        // AVG vrati priemer hodnoteni pre jeden level hry.
        // COALESCE zabezpeci, ze bez ratingov dostanem 0 a nie null.
        Double avg = entityManager.createQuery(
                        "SELECT COALESCE(AVG(r.rating), 0) FROM Rating r WHERE r.game = :g",
                        Double.class
                )
                .setParameter("g", game)
                .getSingleResult();

        // Na stranke zobrazujem cele cislo, preto priemer zaokruhlim.
        return (int) Math.round(avg);
    }

    @Override
    public int getRating(String game, String player) throws RatingException {
        // Zistim rating prihlaseneho hraca pre aktualny level.
        Integer val = entityManager.createQuery(
                        "SELECT r.rating FROM Rating r WHERE r.game = :g AND r.player = :p",
                        Integer.class
                )
                .setParameter("g", game)
                .setParameter("p", player)
                .getResultStream()
                .findFirst()
                .orElse(0);

        // Ak hrac este nehodnotil, vrati sa 0.
        return val;
    }

    @Override
    public void reset() throws RatingException {
        // Pomocna metoda na vymazanie ratingov pri testovani.
        entityManager.createQuery("DELETE FROM Rating").executeUpdate();
    }
}
