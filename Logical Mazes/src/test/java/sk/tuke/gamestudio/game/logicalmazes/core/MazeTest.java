package sk.tuke.gamestudio.game.logicalmazes.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MazeTest {

    @Test
    void testMazeInitAndBorders() {
        Maze maze = new Maze(3, 4);

        // kontrola rozmerov
        assertEquals(3, maze.getRows());
        assertEquals(4, maze.getCols());

        // stav hry
        assertEquals(GameState.PLAYING, maze.getState());

        // okraje maju steny
        for (int c = 0; c < maze.getCols(); c++) {
            assertTrue(maze.getCell(0, c).hasWall(Direction.UP));
            assertTrue(maze.getCell(maze.getRows() - 1, c).hasWall(Direction.DOWN));
        }
        for (int r = 0; r < maze.getRows(); r++) {
            assertTrue(maze.getCell(r, 0).hasWall(Direction.LEFT));
            assertTrue(maze.getCell(r, maze.getCols() - 1).hasWall(Direction.RIGHT));
        }
    }

    @Test
    void testMoveCountIncreases() {
        Maze maze = new Maze(5, 5);
        maze.setPlayerPosition(2, 2);

        int before = maze.getMoveCount();
        maze.move(Direction.RIGHT);
        int after = maze.getMoveCount();

        assertEquals(before + 1, after);
    }

    @Test
    void testBlockedStopsSliding() {
        Maze maze = new Maze(5, 5);
        maze.setPlayerPosition(2, 1);

        // dam blok pred hraca, aby sa zastavil
        maze.setBlocked(2, 3, true);

        // posun doprava: z (2,1) pojde na (2,2) a stopne pred (2,3)
        maze.move(Direction.RIGHT);

        assertEquals(2, maze.getPlayerRow());
        assertEquals(2, maze.getPlayerCol());
    }

    @Test
    void testWallOnCurrentCellStopsSliding() {
        Maze maze = new Maze(5, 5);
        maze.setPlayerPosition(2, 2);

        // nastavim pravu stenu na aktualnom policku
        maze.getCell(2, 2).setWall(Direction.RIGHT, true);

        // hrac sa pokusi ist doprava, ale stena ho nepusti
        maze.move(Direction.RIGHT);

        assertEquals(2, maze.getPlayerRow());
        assertEquals(2, maze.getPlayerCol());
    }

    @Test
    void testBorderWallStopsAtMapEdge() {
        Maze maze = new Maze(5, 5);
        maze.setPlayerPosition(2, 2);

        // hrac sa bude klzat doprava az po okraj
        maze.move(Direction.RIGHT);

        // na poslednom stlpci sa zastavi, lebo okraj ma pravu stenu
        assertEquals(2, maze.getPlayerRow());
        assertEquals(4, maze.getPlayerCol());
    }

    @Test
    void testCollectibleIsCollectedWhenPassingOver() {
        Maze maze = new Maze(5, 5);
        maze.setPlayerPosition(2, 1);

        // nastavim collectible na trase
        maze.setCollectible(2, 2);

        assertEquals(1, maze.getRemainingCollectibles());

        // posun doprava, prejde cez (2,2) a ma ho zobrat
        maze.move(Direction.RIGHT);

        assertEquals(0, maze.getRemainingCollectibles());
    }

    @Test
    void testCollectibleOnStartPositionIsCollectedImmediately() {
        Maze maze = new Maze(5, 5);

        // najprv dam C na startovacie policko
        maze.setCollectible(2, 2);
        assertEquals(1, maze.getRemainingCollectibles());

        // ked tam nastavim hraca, C sa hned zoberie
        maze.setPlayerPosition(2, 2);

        assertEquals(0, maze.getRemainingCollectibles());
        assertEquals(GameState.SOLVED, maze.getState());
    }

    @Test
    void testSolvedWhenAllCollectiblesCollected() {
        Maze maze = new Maze(5, 5);
        maze.setPlayerPosition(2, 1);

        // jedno C
        maze.setCollectible(2, 2);
        assertEquals(GameState.PLAYING, maze.getState());

        // prejdem cez C
        maze.move(Direction.RIGHT);

        assertEquals(0, maze.getRemainingCollectibles());
        assertEquals(GameState.SOLVED, maze.getState());
    }

    @Test
    void testNotSolvedIfNoCollectiblesInLevel() {
        Maze maze = new Maze(5, 5);
        maze.setPlayerPosition(2, 2);

        // bez C sa to nema prepnut do SOLVED
        assertEquals(0, maze.getRemainingCollectibles());
        assertEquals(GameState.PLAYING, maze.getState());
    }

    @Test
    void testNoMoveWhenSolved() {
        Maze maze = new Maze(5, 5);
        maze.setPlayerPosition(2, 1);

        // jedno C aby sa vyriesilo
        maze.setCollectible(2, 2);
        maze.move(Direction.RIGHT);

        assertEquals(GameState.SOLVED, maze.getState());

        int before = maze.getMoveCount();

        // ked je SOLVED, move sa ma ignorovat
        maze.move(Direction.LEFT);

        assertEquals(before, maze.getMoveCount());
    }
}
