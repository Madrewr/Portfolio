package sk.tuke.gamestudio.service;

import sk.tuke.gamestudio.entity.Rating;

public interface RatingService {
    // ulozi alebo aktualizuje rating hraca
    void setRating(Rating rating) throws RatingException;

    // vrati priemerny rating pre hru alebo level
    int getAverageRating(String game) throws RatingException;

    // vrati rating konkretneho hraca
    int getRating(String game, String player) throws RatingException;

    // vymaze ratingy, pouziva sa hlavne v testoch
    void reset() throws RatingException;
}
