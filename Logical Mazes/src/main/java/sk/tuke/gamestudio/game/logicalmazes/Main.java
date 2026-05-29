package sk.tuke.gamestudio.game.logicalmazes;

import sk.tuke.gamestudio.game.logicalmazes.consoleui.ConsoleUI;
import sk.tuke.gamestudio.game.logicalmazes.consoleui.PlayResult;
import sk.tuke.gamestudio.game.logicalmazes.core.LevelLoader;
import sk.tuke.gamestudio.game.logicalmazes.core.Maze;

import java.io.InputStream;

public class Main {

    private static boolean levelExists(int level) {
        // poskladam cestu na level subor v resources
        String path = "levels/level" + level + ".txt";

        // skusim otvorit resource
        InputStream is = Main.class.getClassLoader().getResourceAsStream(path);

        // ak je null, tak level neexistuje
        if (is == null) {
            return false;
        }

        // zavriem stream
        try {
            is.close();
        } catch (Exception e) {
            // nic, nevadi
        }

        return true;
    }

    public static void main(String[] args) {
        // vytvorim konzolove UI
        ConsoleUI ui = new ConsoleUI();

        // menu
        // vyberiem start level, alebo koniec
        int level = ui.showMenuAndGetStartLevel();

        // ked menu vrati 0 , tak koncim program
        if (level == 0) {
            System.out.println("Koniec.");
            return;
        }

        // hlavna slucka hry (prepina levely, restart, quit)
        while (true) {
            // ak level neexistuje, prepni na 1 a oznam to
            if (!levelExists(level)) {
                System.out.println("Level " + level + " neexistuje, davam level 1.");
                level = 1;
            }

            // nacitam level zo suboru
            Maze maze = LevelLoader.loadFromResource("levels/level" + level + ".txt");

            // spustim hranie jedneho levelu
            PlayResult result = ui.play(maze, level);

            // hrac stlacil q
            if (result == PlayResult.QUIT) {
                System.out.println("Koniec.");
                break;
            }

            // hrac stlacil r (restart)
            if (result == PlayResult.RESTART) {
                System.out.println("Restartujem level " + level + "...");
                continue;
            }

            // ak sa sem dostanem, tak hrac vyhral level
            boolean next = ui.askNextLevel();

            // nechce dalsi level, tak ostava na tom istom leveli
            if (!next) {
                System.out.println("Ok, ideme znova level " + level + ".");
                continue;
            }

            // chce dalsi level
            int nextLevel = level + 1;

            // ked existuje dalsi level, posuniem sa
            if (levelExists(nextLevel)) {
                level = nextLevel;
            } else {
                // ked neexistuje, tak zacnem od 1
                System.out.println("Uz nie je novy level, zacinam znova od levelu 1.");
                level = 1;
            }
        }
    }
}