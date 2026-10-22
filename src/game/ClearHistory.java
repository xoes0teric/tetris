package game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ClearHistory remembers every ClearEvent of the game, oldest first.
 * It also counts the pieces locked since the last event.
 */
public class ClearHistory {
    private final List<ClearEvent> events = new ArrayList<>();
    private int piecesSinceLastEvent = 0;

    /**
     * Counts one locked piece. Called every time a piece locks.
     */
    public void countPiece() {
        ++piecesSinceLastEvent;
    }

    /**
     * Returns the number of pieces locked since the last event.
     *
     * @return the pieces locked since the last event
     */
    public int getPiecesSinceLastEvent() {
        return piecesSinceLastEvent;
    }

    /**
     * Adds an event to the history and resets the piece count.
     *
     * @param event the event to remember
     */
    public void record(ClearEvent event) {
        events.add(event);
        piecesSinceLastEvent = 0;
    }

    /**
     * Returns every event of the game, oldest first.
     *
     * @return an unmodifiable list of all events
     */
    public List<ClearEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * Returns the newest event.
     *
     * @return the newest event, or null if there is none
     */
    public ClearEvent getLast() {
        return events.isEmpty() ? null : events.get(events.size() - 1);
    }
}
