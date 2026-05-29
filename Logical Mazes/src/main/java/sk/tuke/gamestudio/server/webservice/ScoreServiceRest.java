package sk.tuke.gamestudio.server.webservice;

import org.springframework.web.bind.annotation.*;
import sk.tuke.gamestudio.entity.Score;
import sk.tuke.gamestudio.service.ScoreService;

import java.util.List;

@RestController
@RequestMapping("/api/score")
public class ScoreServiceRest {

    // REST vrstva sama nepracuje s DB, iba vola ScoreService.
    private final ScoreService scoreService;

    public ScoreServiceRest(ScoreService scoreService) {
        // Spring sem injektuje implementaciu sluzby z GameStudioServer.
        this.scoreService = scoreService;
    }

    @PostMapping
    public void addScore(@RequestBody Score score) {
        // Telo requestu je JSON, Spring ho prevedie na objekt Score.
        scoreService.addScore(score);
    }

    @GetMapping("/{game}")
    public List<Score> getTopScores(@PathVariable String game) {
        // Vrati top skore pre konkretny level, napr. logicalmazes_level_1.
        return scoreService.getTopScores(game);
    }

    @GetMapping("/best/{gamePrefix}")
    public List<Score> getBestOverallScores(@PathVariable String gamePrefix) {
        // Vrati celkovy rebricek pre vsetky levely hry.
        return scoreService.getBestOverallScores(gamePrefix);
    }
}
