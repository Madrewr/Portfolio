package sk.tuke.gamestudio;

import sk.tuke.gamestudio.game.logicalmazes.Main;
import sk.tuke.gamestudio.game.logicalmazes.consoleui.ConsoleUI;
import sk.tuke.gamestudio.game.logicalmazes.consoleui.PlayResult;
import sk.tuke.gamestudio.game.logicalmazes.core.LevelLoader;
import sk.tuke.gamestudio.game.logicalmazes.core.Maze;
import sk.tuke.gamestudio.service.CommentService;
import sk.tuke.gamestudio.service.CommentServiceRestClient;
import sk.tuke.gamestudio.service.RatingService;
import sk.tuke.gamestudio.service.RatingServiceRestClient;
import sk.tuke.gamestudio.service.ScoreService;
import sk.tuke.gamestudio.service.ScoreServiceRestClient;

import java.io.InputStream;

public class RestClientMain {

    private static boolean levelExists(int level) {
        // Level hladam v resources podla nazvu suboru.
        String path = "levels/level" + level + ".txt";
        InputStream is = Main.class.getClassLoader().getResourceAsStream(path);

        // Ak je stream null, subor levelu neexistuje.
        if (is == null) {
            return false;
        }

        try {
            // Stream zatvorim, lebo tu iba testujem existenciu suboru.
            is.close();
        } catch (Exception e) {
            // Ked sa nepodari zatvorit stream, pre tuto kontrolu to nevadi.
        }
        return true;
    }

    public static void main(String[] args) {
        // Tu pouzijem REST klientov namiesto JDBC, preto musi bezat GameStudioServer.
        String url = "http://localhost:8080/api";

        // Vytvorim klientov pre vsetky tri sluzby.
        ScoreService scoreService = new ScoreServiceRestClient(url);
        CommentService commentService = new CommentServiceRestClient(url);
        RatingService ratingService = new RatingServiceRestClient(url);

        // UI dostane sluzby zvonku, takze nevie ci su JDBC alebo REST.
        ConsoleUI ui = new ConsoleUI(scoreService, commentService, ratingService);

        // Menu vrati 0, ked chce hrac skoncit.
        int level = ui.showMenuAndGetStartLevel();
        if (level == 0) {
            System.out.println("Koniec.");
            return;
        }

        while (true) {
            // Ak hrac zada level, ktory neexistuje, vratim ho na level 1.
            if (!levelExists(level)) {
                System.out.println("Level " + level + " neexistuje, davam level 1.");
                level = 1;
            }

            // Level sa nacita vzdy nanovo, aby restart zacal od cisteho stavu.
            Maze maze = LevelLoader.loadFromResource("levels/level" + level + ".txt");

            // ConsoleUI odohra jeden level a vrati vysledok.
            PlayResult result = ui.play(maze, level);

            if (result == PlayResult.QUIT) {
                System.out.println("Koniec.");
                break;
            }

            if (result == PlayResult.RESTART) {
                System.out.println("Restartujem level " + level + "...");
                continue;
            }

            // Po vyhre sa pytam, ci chce hrac pokracovat na dalsi level.
            boolean next = ui.askNextLevel();
            if (!next) {
                System.out.println("Ok, ideme znova level " + level + ".");
                continue;
            }

            int nextLevel = level + 1;

            // Ak dalsi level existuje, prejdem nan, inak idem od levelu 1.
            if (levelExists(nextLevel)) {
                level = nextLevel;
            } else {
                System.out.println("Uz nie je novy level, zacinam znova od levelu 1.");
                level = 1;
            }
        }
    }
}
