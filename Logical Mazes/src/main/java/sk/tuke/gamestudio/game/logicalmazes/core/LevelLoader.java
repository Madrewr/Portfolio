package sk.tuke.gamestudio.game.logicalmazes.core;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LevelLoader {

    // nechcem aby sa tato trieda dala vytvarat cez new
    // pouzivam len staticku metodu loadFromResource
    private LevelLoader() {

    }

    // nacita level zo suboru v resources (napr. levels/level1.txt)
    public static Maze loadFromResource(String path) {
        // najdem subor v resources cez classloader
        InputStream is = LevelLoader.class.getClassLoader().getResourceAsStream(path);

        // ak je null, tak subor neexistuje alebo je zla cesta
        if (is == null) {
            throw new IllegalArgumentException("Nenasiel som resource subor: " + path);
        }

        // sem si nacitam vsetky riadky mapy
        List<String> lines = new ArrayList<>();

        // citam subor riadok po riadku
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;

            // citam kym sa da
            while ((line = br.readLine()) != null) {
                // preskakujem prazdne riadky (aby mi to nerobilo problemy)
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (Exception e) {
            // ked nastane chyba pri citani, hodim runtime exception
            throw new RuntimeException("Chyba pri citani levelu: " + path, e);
        }

        // ak sa nenasiel ziadny riadok, level je prazdny
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Level je prazdny: " + path);
        }

        // pocet riadkov = vyska
        int rows = lines.size();

        // pocet stlpcov = dlzka prveho riadku
        int cols = lines.get(0).length();

        // kontrola ze kazdy riadok ma rovnaku dlzku
        for (String l : lines) {
            if (l.length() != cols) {
                throw new IllegalArgumentException("Level nema rovnaku dlzku riadkov.");
            }
        }

        // vytvorim prazdny maze s danou velkostou
        Maze maze = new Maze(rows, cols);

        // kontrola ze som nasiel start P
        boolean playerFound = false;

        // prechadzam vsetky policka mapy
        for (int r = 0; r < rows; r++) {
            // aktualny riadok z txt
            String l = lines.get(r);

            for (int c = 0; c < cols; c++) {
                // znak na pozicii (r,c)
                char ch = l.charAt(c);

                // legenda mapy:
                // # = stena (neprechodne)
                // . = prazdne
                // P = start hraca (1 kus)
                // C = collectible (moze byt viac kusov)

                if (ch == '#') {
                    // nastavim ako blok
                    maze.setBlocked(r, c, true);

                } else if (ch == 'C') {
                    // nastavim collectible na danom policku
                    maze.setCollectible(r, c);

                } else if (ch == 'P') {
                    // nastavim start hraca
                    maze.setPlayerPosition(r, c);
                    playerFound = true;

                } else if (ch == '.') {
                    // prazdne policko, netreba nic robit

                } else {
                    // ak je iny znak, tak je to chyba v level subore
                    throw new IllegalArgumentException("Neznamy znak '" + ch + "' na (" + r + "," + c + ")");
                }
            }
        }

        // ak som nenasiel P, tak level je neplatny
        if (!playerFound) {
            throw new IllegalArgumentException("V leveli chyba start 'P'.");
        }

        // vratim pripraveny maze
        return maze;
    }
}