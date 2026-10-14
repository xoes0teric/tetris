package game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
    ClearHistory remembers every ClearEvent of the game, oldest first
*/
public class ClearHistory {
    private final List<ClearEvent> events = new ArrayList<>();
    private int piecesSinceLastEvent = 0;

    // Called every time a piece locks
    public void countPiece() {
        ++piecesSinceLastEvent;
    }

    public int getPiecesSinceLastEvent() {
        return piecesSinceLastEvent;
    }

    public void record(ClearEvent event) {
        events.add(event);
        piecesSinceLastEvent = 0;
    }

    public List<ClearEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    // The newest event, null if there is none
    public ClearEvent getLast() {
        return events.isEmpty() ? null : events.get(events.size() - 1);
    }
}
