package sk.tuke.gamestudio.server.webservice;

import org.springframework.web.bind.annotation.*;
import sk.tuke.gamestudio.entity.Rating;
import sk.tuke.gamestudio.service.RatingService;

@RestController
@RequestMapping("/api/rating")
public class RatingServiceRest {

    // REST vrstva sama nepracuje s DB, iba vola RatingService.
    private final RatingService ratingService;

    public RatingServiceRest(RatingService ratingService) {
        // Spring sem injektuje implementaciu sluzby z GameStudioServer.
        this.ratingService = ratingService;
    }

    @PostMapping
    public void setRating(@RequestBody Rating rating) {
        // Telo requestu je JSON, Spring ho prevedie na objekt Rating.
        ratingService.setRating(rating);
    }

    @GetMapping("/{game}")
    public int getAverageRating(@PathVariable String game) {
        // Vrati priemerny rating pre konkretny level.
        return ratingService.getAverageRating(game);
    }

    @GetMapping("/{game}/{player}")
    public int getRating(@PathVariable String game, @PathVariable String player) {
        // Vrati rating konkretneho hraca pre konkretny level.
        return ratingService.getRating(game, player);
    }
}
