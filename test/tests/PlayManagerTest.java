package tests;

import boss.BedrockBoss;
import game.Board;
import game.ClearEvent;
import game.ClearSource;
import game.GameState;
import game.PlayManager;
import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import tetromino.Shape;
import tetromino.Tetromino;
import upgrades.Upgrade;

final class PlayManagerTest {
    static final long FRAME = 16_666_667L;

    private PlayManagerTest() {
    }

    static PlayManager newGame(int seed) {
        return new PlayManager(new Random(seed), List.<BedrockBoss>of());
    }

    // Uses a fresh patch charge to fill count cells of a column, going up from bottomRow
    static void patchColumn(PlayManager pm, int col, int bottomRow, int count) {
        pm.getUpgrader().getPatch().getUpgrade().apply();
        pm.usePatch();
        Check.equal(GameState.AIM_PATCH, pm.getGameState(), "aiming the patch");
        int startRow = pm.getBoard().getPlayableRows() - 1;
        pm.moveCursor(col - Board.COLUMNS / 2, bottomRow - startRow);
        for (int i = 0; i < count; ++i) {
            pm.markTarget();
            pm.moveCursor(0, -1);
        }
        pm.confirmTarget();
    }

    // Fills the rows from top to bottom (inclusive) except column 0
    static void fillRowsWithGap(Board board, int top, int bottom) {
        for (int row = top; row <= bottom; ++row) {
            BoardTest.fillRow(board, row, 0);
        }
    }

    // Clears 10 lines with patches. The bottom 10 playable rows have to be empty
    static void clearTenLines(PlayManager pm) {
        int bottom = pm.getBoard().getPlayableRows() - 1;
        fillRowsWithGap(pm.getBoard(), bottom - 9, bottom);
        for (int count : new int[]{3, 3, 3, 1}) {
            patchColumn(pm, 0, bottom, count);
        }
    }

    static void testLockedPieceClearsLineAndIsRecorded() {
        PlayManager pm = newGame(1);
        Tetromino piece = pm.getCurrentPiece();
        Shape shape = piece.getShape();
        int drop = piece.getDropDistance();
        boolean[] landsInBottomRow = new boolean[Board.COLUMNS];
        for (int[] block : piece.getBlocks()) {
            if (block[1] + drop == Board.ROWS - 1) {
                landsInBottomRow[block[0]] = true;
            }
        }
        for (int col = 0; col < Board.COLUMNS; ++col) {
            if (!landsInBottomRow[col]) {
                pm.getBoard().fillCell(col, Board.ROWS - 1, Color.gray);
            }
        }
        Shape next = pm.getNextShapes().get(0);

        pm.hardDrop();
        pm.update(FRAME);

        Check.equal(1, pm.getLines(), "lines");
        Check.equal(10, pm.getScore(), "score");
        Check.equal(next, pm.getCurrentPiece().getShape(), "next piece spawned");
        ClearEvent event = pm.getHistory().getLast();
        Check.equal(ClearSource.LINE_CLEAR, event.getSource(), "source");
        Check.equal(1, event.getLinesCleared(), "event lines");
        Check.equal(10, event.getScoreEarned(), "event score");
        Check.equal(shape, event.getPiece(), "event piece");
        Check.isTrue(event.isHardDrop(), "event hard drop");
        Check.equal(1, event.getPiecesSinceLastClear(), "event pieces");
        Check.equal(GameState.PLAY, pm.getGameState(), "still playing");
    }

    static void testPieceFallsAndLocksWithoutInput() {
        PlayManager pm = newGame(2);
        Shape first = pm.getCurrentPiece().getShape();
        for (int i = 0; i < 60 * 25 && pm.getCurrentPiece().getShape() == first; ++i) {
            pm.update(FRAME);
        }
        Check.equal(1, countAll(pm.getBoard()) / 4, "one piece locked");
        Check.isTrue(pm.getHistory().getEvents().isEmpty(), "no clear recorded");
    }

    static void testPatchCompletingLinesCountsAsLineClear() {
        PlayManager pm = newGame(3);
        fillRowsWithGap(pm.getBoard(), 18, 19);
        patchColumn(pm, 0, 19, 2);
        Check.equal(2, pm.getLines(), "lines");
        Check.equal(20, pm.getScore(), "score");
        ClearEvent event = pm.getHistory().getLast();
        Check.equal(ClearSource.PATCH, event.getSource(), "source");
        Check.equal(2, event.getLinesCleared(), "event lines");
        Check.isTrue(event.isBoardEmptyAfter(), "perfect clear");
        Check.equal(GameState.PLAY, pm.getGameState(), "back to play");
    }

    static void testMilestoneOpensSelectionOnceAndFreezesGame() {
        PlayManager pm = newGame(4);
        Board board = pm.getBoard();
        fillRowsWithGap(board, 10, 19);
        for (int count : new int[]{3, 3, 3}) {
            patchColumn(pm, 0, 19, count);
            Check.equal(GameState.PLAY, pm.getGameState(), "no milestone yet");
        }
        patchColumn(pm, 0, 19, 1);
        Check.equal(10, pm.getLines(), "lines");
        Check.equal(GameState.SELECT_UPGRADE, pm.getGameState(), "selection open");
        List<Upgrade> options = pm.getUpgradeSelection().getOptions();
        Check.equal(3, options.size(), "three options");

        // The board doesn't move while choosing
        int[][] before = pm.getCurrentPiece().getBlocks();
        for (int i = 0; i < 300; ++i) {
            pm.update(FRAME);
        }
        pm.moveLeft();
        pm.rotate();
        pm.hardDrop();
        pm.hold(0);
        pm.useFlush();
        pm.update(FRAME);
        Check.isTrue(Arrays.deepEquals(before, pm.getCurrentPiece().getBlocks()), "frozen");
        Check.equal(GameState.SELECT_UPGRADE, pm.getGameState(), "still choosing");

        pm.selectUpgrade(2);
        Upgrade chosen = options.get(2);
        int level = chosen.getLevel();
        pm.confirmUpgrade();
        Check.equal(level + 1, chosen.getLevel(), "upgrade applied");
        Check.equal(GameState.PLAY, pm.getGameState(), "resumed");
        Check.equal(null, pm.getUpgradeSelection(), "selection closed");

        // the same milestone doesn't open again, and the piece falls again
        for (int i = 0; i < 120; ++i) {
            pm.update(FRAME);
            Check.equal(GameState.PLAY, pm.getGameState(), "milestone only once");
        }
        Check.isTrue(!Arrays.deepEquals(before, pm.getCurrentPiece().getBlocks()), "falling");

        // the next milestone works too
        clearTenLines(pm);
        Check.equal(20, pm.getLines(), "lines");
        Check.equal(GameState.SELECT_UPGRADE, pm.getGameState(), "second selection");
        pm.confirmUpgrade();
        Check.equal(GameState.PLAY, pm.getGameState(), "resumed again");
    }

    static void testHammerGivesPointsButNoLines() {
        PlayManager pm = newGame(5);
        Board board = pm.getBoard();
        for (int col = 0; col < 5; ++col) {
            board.fillCell(col, 19, Color.gray);
        }
        board.fillCell(7, 18, Color.red);
        pm.getUpgrader().getHammer().getUpgrade().apply();
        pm.useHammer();
        Check.equal(GameState.AIM_HAMMER, pm.getGameState(), "aiming");
        pm.confirmTarget();
        Check.equal(GameState.PLAY, pm.getGameState(), "back to play");
        Check.equal(0, pm.getLines(), "not a line clear");
        Check.equal(5, pm.getScore(), "points per destroyed block");
        Check.equal(Color.red, board.getColor(7, 19), "row above fell down");
        ClearEvent event = pm.getHistory().getLast();
        Check.equal(ClearSource.HAMMER, event.getSource(), "source");
        Check.equal(0, event.getLinesCleared(), "event lines");
        Check.equal(5, event.getBlocksRemoved(), "event blocks");
        Check.isTrue(!pm.getUpgrader().getHammer().hasCharge(), "charge used");
    }

    static void testHammerCancelKeepsChargeAndEmptyRowIsRejected() {
        PlayManager pm = newGame(6);
        pm.getUpgrader().getHammer().getUpgrade().apply();
        pm.useHammer();
        pm.confirmTarget(); // the bottom row is empty
        Check.equal(GameState.AIM_HAMMER, pm.getGameState(), "can't smash an empty row");
        pm.cancelTarget();
        Check.equal(GameState.PLAY, pm.getGameState(), "cancelled");
        Check.isTrue(pm.getUpgrader().getHammer().hasCharge(), "charge kept");
    }

    static void testFlushGivesNoPointsAndNoLines() {
        PlayManager pm = newGame(7);
        Board board = pm.getBoard();
        fillRowsWithGap(board, 15, 19);
        pm.getUpgrader().getFlush().getUpgrade().apply();
        pm.useFlush();
        Check.equal(0, pm.getScore(), "no points");
        Check.equal(0, pm.getLines(), "no lines");
        Check.equal(18, countAll(board), "two rows left");
        Check.equal(9, board.countBlocks(19), "stack moved down");
        Check.equal(0, board.countBlocks(17), "top of stack moved down");
        ClearEvent event = pm.getHistory().getLast();
        Check.equal(ClearSource.FLUSH, event.getSource(), "source");
        Check.equal(0, event.getScoreEarned(), "event score");
        Check.isTrue(!event.getSource().isComboAction(), "flush doesn't keep combos");
    }

    static void testHoldSwapsOncePerPiece() {
        PlayManager pm = newGame(8);
        Shape first = pm.getCurrentPiece().getShape();
        pm.hold(0);
        Check.equal(first, pm.getCurrentPiece().getShape(), "hold is locked");

        pm.getUpgrader().getHold().getUpgrade().apply();
        Shape next = pm.getNextShapes().get(0);
        pm.hold(0);
        Check.equal(next, pm.getCurrentPiece().getShape(), "took the next piece");
        Check.equal(first, pm.getUpgrader().getHold().getSlot(0), "first piece held");
        pm.hold(0);
        Check.equal(next, pm.getCurrentPiece().getShape(), "only once per piece");

        pm.hardDrop();
        pm.update(FRAME);
        pm.hold(0);
        Check.equal(first, pm.getCurrentPiece().getShape(), "held piece comes back");
    }

    static void testPreviewStaysInSyncWithSpawnedPieces() {
        PlayManager pm = newGame(9);
        Check.equal(1, pm.getNextShapes().size(), "one next piece at the start");
        for (int i = 0; i < 3; ++i) {
            pm.getUpgrader().getSeeTheFuture().getUpgrade().apply();
        }
        // applied directly instead of through the menu, so the next pick refreshes the queue
        pm.hardDrop();
        pm.update(FRAME);
        for (int i = 0; i < 6; ++i) {
            List<Shape> preview = pm.getNextShapes();
            for (int move = 0; move < 4; ++move) {
                if (i % 2 == 0) {
                    pm.moveLeft();
                } else {
                    pm.moveRight();
                }
            }
            pm.hardDrop();
            pm.update(FRAME);
            Check.equal(preview.get(0), pm.getCurrentPiece().getShape(), "spawned the preview");
            Check.equal(4, pm.getNextShapes().size(), "four previews");
            Check.equal(preview.subList(1, 4), pm.getNextShapes().subList(0, 3), "queue moved");
        }
    }

    static int countAll(Board board) {
        int total = 0;
        for (int row = 0; row < Board.ROWS; ++row) {
            total += board.countBlocks(row);
        }
        return total;
    }
}
