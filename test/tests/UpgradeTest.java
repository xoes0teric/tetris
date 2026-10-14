package tests;

import game.Board;
import game.PiecePicker;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import tetromino.Shape;
import tetromino.Tetromino;
import upgrades.Flush;
import upgrades.Hammer;
import upgrades.Hold;
import upgrades.Patch;
import upgrades.SeeTheFuture;
import upgrades.Upgrade;
import upgrades.UpgradeSelection;
import upgrades.Upgrader;

final class UpgradeTest {
    private UpgradeTest() {
    }

    static void testUpgradesStopAtMaxLevel() {
        Upgrader upgrader = new Upgrader(new Random(1));
        for (Upgrade upgrade : upgrader.getAll()) {
            for (int i = 0; i < upgrade.getMaxLevel() + 2; ++i) {
                upgrade.apply();
            }
            Check.equal(upgrade.getMaxLevel(), upgrade.getLevel(), upgrade.getName() + " level");
            Check.isTrue(!upgrade.isAvailable(), upgrade.getName() + " not offered when maxed");
        }
        Check.equal(3, upgrader.getHold().getUpgrade().getMaxLevel(), "hold max");
        Check.equal(3, upgrader.getSeeTheFuture().getUpgrade().getMaxLevel(),
                "see the future max");
    }

    static void testSelectionOffersThreeDifferentAvailableUpgrades() {
        for (int seed = 0; seed < 20; ++seed) {
            Upgrader upgrader = new Upgrader(new Random(seed));
            List<Upgrade> options = upgrader.openSelection().getOptions();
            Check.equal(3, options.size(), "option count");
            Check.equal(3, new HashSet<>(options).size(), "no duplicates");
        }
    }

    static void testSelectionSkipsMaxedUpgrades() {
        Upgrader upgrader = new Upgrader(new Random(2));
        upgrader.getHammer().getUpgrade().apply();
        upgrader.getPatch().getUpgrade().apply();
        upgrader.getFlush().getUpgrade().apply();
        for (int i = 0; i < 3; ++i) {
            upgrader.getHold().getUpgrade().apply();
        }
        List<Upgrade> options = upgrader.openSelection().getOptions();
        Check.equal(List.of(upgrader.getSeeTheFuture().getUpgrade()), options, "only see the future left");

        for (int i = 0; i < 3; ++i) {
            upgrader.getSeeTheFuture().getUpgrade().apply();
        }
        Check.isTrue(upgrader.openSelection().getOptions().isEmpty(), "nothing left to offer");
    }

    static void testSingleUseUpgradeIsOfferedAgainAfterUse() {
        Upgrader upgrader = new Upgrader(new Random(3));
        Hammer hammer = upgrader.getHammer();
        hammer.getUpgrade().apply();
        Check.isTrue(!hammer.getUpgrade().isAvailable(), "not offered while charged");
        Board board = new Board();
        board.fillCell(0, 19, Color.red);
        hammer.startAiming(board);
        hammer.confirm();
        Check.isTrue(!hammer.hasCharge(), "charge spent");
        Check.isTrue(hammer.getUpgrade().isAvailable(), "offered again");
    }

    static void testMilestoneIsDueOncePerTenLines() {
        Upgrader upgrader = new Upgrader(new Random(4));
        Check.isTrue(!upgrader.isSelectionDue(9), "not due at 9");
        Check.isTrue(upgrader.isSelectionDue(10), "due at 10");
        upgrader.openSelection();
        Check.isTrue(!upgrader.isSelectionDue(10), "used up at 10");
        Check.isTrue(!upgrader.isSelectionDue(19), "not due at 19");
        Check.isTrue(upgrader.isSelectionDue(21), "due at 21");
        upgrader.openSelection();
        Check.isTrue(!upgrader.isSelectionDue(21), "used up at 21");
    }

    static void testSelectionMovesWrapsAndApplies() {
        Upgrader upgrader = new Upgrader(new Random(5));
        UpgradeSelection selection = upgrader.openSelection();
        selection.move(-1);
        Check.equal(2, selection.getSelected(), "wraps to the last option");
        selection.select(1);
        Upgrade chosen = selection.getOptions().get(1);
        int before = chosen.getLevel();
        Check.equal(chosen, selection.confirm(), "confirmed option");
        Check.equal(before + 1, chosen.getLevel(), "applied");
    }

    static void testHoldOncePerPieceAndSlotsPerLevel() {
        Hold hold = new Hold();
        Check.isTrue(!hold.canHold(0), "locked before upgrade");
        hold.getUpgrade().apply();
        Check.isTrue(hold.canHold(0) && !hold.canHold(1), "level 1 has one slot");
        Check.equal(null, hold.swap(0, Shape.T), "empty slot");
        Check.isTrue(!hold.canHold(0), "only once per piece");
        hold.onPieceLocked();
        Check.equal(Shape.T, hold.swap(0, Shape.I), "swapped back");
        hold.getUpgrade().apply();
        hold.getUpgrade().apply();
        hold.onPieceLocked();
        Check.equal(3, hold.getSlotCount(), "three slots");
        Check.isTrue(hold.canHold(2), "third slot usable");
    }

    static void testSeeTheFutureAddsOnePiecePerLevel() {
        SeeTheFuture future = new SeeTheFuture();
        Check.equal(1, future.getPreviewCount(), "base preview");
        future.getUpgrade().apply();
        future.getUpgrade().apply();
        future.getUpgrade().apply();
        Check.equal(4, future.getPreviewCount(), "max preview");
    }

    static void testPreviewMatchesPickedPieces() {
        Board board = new Board();
        PiecePicker picker = new PiecePicker(board, new Random(6));
        for (int round = 0; round < 10; ++round) {
            List<Shape> preview = new ArrayList<>(picker.peek(4));
            Check.equal(preview.get(0), picker.pickPiece().getShape(), "next piece");
            Check.equal(preview.subList(1, 4), picker.peek(3), "rest of the preview kept");
        }
    }

    static void testHammerAimsOnlyAtPlayableRows() {
        Board board = new Board();
        board.addUnbreakableRows(4);
        Hammer hammer = new Hammer();
        hammer.getUpgrade().apply();
        hammer.startAiming(board);
        hammer.moveCursor(10);
        Check.equal(15, hammer.getAimRow(), "cursor stops above the unbreakable rows");
        Check.isTrue(!hammer.canConfirm(), "empty row can't be smashed");
        board.fillCell(1, 15, Color.red);
        Check.isTrue(hammer.canConfirm(), "row with blocks can be smashed");
        Check.equal(1, hammer.confirm(), "removed blocks");
        Check.equal(4, board.getUnbreakableRows(), "unbreakable rows remain");
        Check.boardValid(board);
    }

    static void testPatchPlacesUpToThreeConnectedBlocks() {
        Board board = new Board();
        board.fillCell(5, 19, Color.red);
        Tetromino piece = new Tetromino(Shape.O, board);
        piece.place(0, 17); // covers columns 0-1, rows 17-18
        Patch patch = new Patch();
        patch.getUpgrade().apply();
        patch.startAiming(board, piece); // cursor at (5, 19)
        Check.isTrue(!patch.isCursorValid(), "occupied cell");
        patch.mark();
        Check.isTrue(patch.getMarked().isEmpty(), "occupied cell not marked");

        patch.moveCursor(1, 0); // (6, 19)
        patch.mark();
        patch.moveCursor(2, 0); // (8, 19) doesn't touch (6, 19)
        patch.mark();
        Check.equal(1, patch.getMarked().size(), "not connected");
        patch.moveCursor(-1, 0); // (7, 19)
        patch.mark();
        patch.moveCursor(1, 0); // (8, 19)
        patch.mark();
        Check.equal(3, patch.getMarked().size(), "three connected");
        patch.moveCursor(1, 0); // (9, 19)
        patch.mark();
        Check.equal(3, patch.getMarked().size(), "max three");

        patch.moveCursor(-2, 0); // (7, 19) is the middle, removing it splits the rest
        patch.mark();
        Check.equal(3, patch.getMarked().size(), "can't split the patch");

        Check.isTrue(patch.canConfirm(), "valid patch");
        patch.confirm();
        Check.equal(4, board.countBlocks(19), "three blocks added");
        Check.equal(Patch.COLOR, board.getColor(8, 19), "patch block");
        Check.isTrue(!patch.hasCharge(), "charge spent");
    }

    static void testPatchCantCoverFallingPieceOrUnbreakableRows() {
        Board board = new Board();
        board.addUnbreakableRows(2);
        Tetromino piece = new Tetromino(Shape.O, board);
        piece.place(5, 16); // covers columns 5-6, rows 16-17
        Patch patch = new Patch();
        patch.getUpgrade().apply();
        patch.startAiming(board, piece); // cursor at (5, 17)
        Check.isTrue(!patch.isCursorValid(), "falling piece cell");
        patch.moveCursor(0, 5);
        Check.isTrue(!patch.isCursorValid(), "cursor stays on the falling piece at row 17");
        patch.moveCursor(-1, 0);
        patch.mark();
        Check.isTrue(patch.canConfirm(), "free cell next to the piece");
        Check.boardValid(board);
    }

    static void testFlushRemovesBottomThreeRowsWithoutTouchingUnbreakable() {
        Board board = new Board();
        board.addUnbreakableRows(3);
        Flush flush = new Flush();
        flush.getUpgrade().apply();
        Check.isTrue(!flush.canUse(board), "nothing to flush");
        for (int row = 12; row < 17; ++row) {
            board.fillCell(0, row, Color.red);
        }
        Check.isTrue(flush.canUse(board), "something to flush");
        Check.equal(3, flush.use(board), "removed 3 blocks");
        Check.equal(2, countAll(board), "two blocks left");
        Check.equal(Color.red, board.getColor(0, 16), "stack moved down 3");
        Check.equal(3, board.getUnbreakableRows(), "unbreakable rows remain");
        Check.isTrue(!flush.hasCharge(), "charge spent");
        Check.boardValid(board);
    }

    private static int countAll(Board board) {
        int total = 0;
        for (int row = 0; row < Board.ROWS; ++row) {
            total += board.countBlocks(row);
        }
        return total;
    }
}
