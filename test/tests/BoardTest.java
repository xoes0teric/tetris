package tests;

import game.Board;
import java.awt.Color;
import java.util.List;

final class BoardTest {
    private BoardTest() {
    }

    static void fillRow(Board board, int row, int... skipCols) {
        for (int col = 0; col < Board.COLUMNS; ++col) {
            boolean skip = false;
            for (int s : skipCols) {
                skip |= s == col;
            }
            if (!skip) {
                board.fillCell(col, row, Color.gray);
            }
        }
    }

    static void testFullRowIsClearedAndRowsAboveFall() {
        Board board = new Board();
        fillRow(board, 19);
        board.fillCell(3, 18, Color.red);
        Check.equal(List.of(19), board.clearFullRows(), "cleared rows");
        Check.equal(Color.red, board.getColor(3, 19), "block above fell down");
        Check.equal(0, board.countBlocks(18), "row 18 is empty");
    }

    static void testFillCellRejectsOccupiedAndOutside() {
        Board board = new Board();
        Check.isTrue(board.fillCell(0, 0, Color.red), "free cell");
        Check.isTrue(!board.fillCell(0, 0, Color.red), "occupied cell");
        Check.isTrue(!board.fillCell(-1, 5, Color.red), "left of board");
        Check.isTrue(!board.fillCell(Board.COLUMNS, 5, Color.red), "right of board");
        Check.isTrue(!board.fillCell(0, Board.ROWS, Color.red), "below board");
        Check.isTrue(!board.fillCell(0, -1, Color.red), "above board");
    }

    static void testDestroyRowRemovesRowAndShifts() {
        Board board = new Board();
        fillRow(board, 19, 0, 1);
        board.fillCell(5, 18, Color.red);
        Check.equal(8, board.destroyRow(19), "blocks destroyed");
        Check.equal(Color.red, board.getColor(5, 19), "row above fell down");
        Check.equal(1, board.countBlocks(19), "only the fallen block is left");
    }

    static void testRemoveBottomRowsShiftsStackDown() {
        Board board = new Board();
        for (int row = 15; row < Board.ROWS; ++row) {
            board.fillCell(row - 15, row, Color.red);
        }
        Check.equal(3, board.removeBottomRows(3), "blocks removed");
        Check.equal(Color.red, board.getColor(0, 18), "row 15 moved to 18");
        Check.equal(Color.red, board.getColor(1, 19), "row 16 moved to 19");
        Check.equal(0, board.countBlocks(17), "nothing else left");
    }

    static void testUnbreakableRowsPushStackUpAndBlockPieces() {
        Board board = new Board();
        board.fillCell(4, 19, Color.red);
        Check.isTrue(board.addUnbreakableRows(4), "fits");
        Check.equal(16, board.getPlayableRows(), "playable rows");
        Check.equal(Color.red, board.getColor(4, 15), "stack pushed up");
        Check.isTrue(board.isUnbreakable(16) && !board.isUnbreakable(15), "unbreakable rows");
        Check.isTrue(!board.isFreeCell(0, 16), "pieces can't enter unbreakable rows");
        Check.boardValid(board);
    }

    static void testUnbreakableRowsOverflowReportsTopOut() {
        Board board = new Board();
        board.fillCell(9, 2, Color.red);
        Check.isTrue(!board.addUnbreakableRows(4), "block pushed out of the top");
        Check.boardValid(board);
    }

    static void testUnbreakableRowsSurviveEveryBoardEdit() {
        Board board = new Board();
        board.addUnbreakableRows(4);
        fillRow(board, 15);
        Check.equal(List.of(15), board.clearFullRows(), "only the playable row clears");
        Check.equal(4, board.getUnbreakableRows(), "after line clear");

        Check.equal(0, board.destroyRow(16), "hammer can't hit unbreakable rows");
        Check.equal(4, board.getUnbreakableRows(), "after destroyRow");

        Check.isTrue(!board.fillCell(0, 17, Color.red), "can't patch unbreakable cells");

        fillRow(board, 13, 0);
        fillRow(board, 14, 0);
        fillRow(board, 15, 0);
        fillRow(board, 12, 0);
        Check.equal(27, board.removeBottomRows(3), "flush removes rows 13-15 only");
        Check.equal(9, board.countBlocks(15), "row 12 fell to row 15");
        Check.equal(4, board.getUnbreakableRows(), "after flush");
        Check.boardValid(board);
    }

    static void testRemovingUnbreakableRowsRestoresStack() {
        Board board = new Board();
        board.fillCell(2, 19, Color.red);
        board.addUnbreakableRows(4);
        board.removeUnbreakableRows();
        Check.equal(Board.ROWS, board.getPlayableRows(), "all rows playable");
        Check.equal(Color.red, board.getColor(2, 19), "stack moved back down");
        Check.isTrue(board.isFreeCell(0, 18), "old unbreakable cells are free");
        Check.boardValid(board);
    }
}
