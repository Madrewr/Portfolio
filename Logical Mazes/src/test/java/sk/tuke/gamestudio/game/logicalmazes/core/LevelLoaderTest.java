package sk.tuke.gamestudio.game.logicalmazes.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LevelLoaderTest {

    @Test
    void testLoadExistingLevel() {
        Maze maze = LevelLoader.loadFromResource("levels/level1.txt");

        assertNotNull(maze);
        assertTrue(maze.getRows() > 0);
        assertTrue(maze.getCols() > 0);

        // player musi byt v mape
        assertTrue(maze.getPlayerRow() >= 0 && maze.getPlayerRow() < maze.getRows());
        assertTrue(maze.getPlayerCol() >= 0 && maze.getPlayerCol() < maze.getCols());
    }

    @Test
    void testAllExistingLevelsCanBeLoaded() {
        // V projekte je vytvorenych 9 levelov, vsetky musia byt nacitatelne.
        for (int i = 1; i <= 9; i++) {
            Maze maze = LevelLoader.loadFromResource("levels/level" + i + ".txt");

            assertNotNull(maze);
            assertTrue(maze.getRows() > 0);
            assertTrue(maze.getCols() > 0);
        }
    }

    @Test
    void testLoadMissingFileThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            LevelLoader.loadFromResource("levels/neexistuje.txt");
        });
    }

    @Test
    void testLoadLevelWithoutPlayerThrows() {
        // test_no_player.txt obsah: len . a # a C, bez P
        assertThrows(IllegalArgumentException.class, () -> {
            LevelLoader.loadFromResource("levels/test_no_player.txt");
        });
    }

    @Test
    void testLoadLevelWithUnknownCharThrows() {
        // test_bad_char.txt ma napr. X
        assertThrows(IllegalArgumentException.class, () -> {
            LevelLoader.loadFromResource("levels/test_bad_char.txt");
        });
    }

    @Test
    void testLevelRowLengthMustMatch() {
        // jeden riadok ma inu dlzku
        assertThrows(IllegalArgumentException.class, () -> {
            LevelLoader.loadFromResource("levels/test_bad_width.txt");
        });
    }
}
