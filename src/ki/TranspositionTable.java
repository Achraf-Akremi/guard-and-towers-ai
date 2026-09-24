package ki;

/**
 * A simple Transposition Table (TT) that stores:
 *   (zobristKey, depth, score, flag)
 * for each hashed position.  Flags: 0=EXACT, 1=UPPER, 2=LOWER.
 *
 * tableSizePowerOfTwo must be e.g. 20 → table size = 2^20 entries.
 */
public class TranspositionTable {
    public static final byte FLAG_EXACT = 0;
    public static final byte FLAG_UPPER = 1;
    public static final byte FLAG_LOWER = 2;

    public static class Entry {
        public long key;
        public int  depth;
        public int  score;
        public byte flag;
    }

    private final Entry[] table;

    public TranspositionTable(int tableSizePowerOfTwo) {
        table = new Entry[1 << tableSizePowerOfTwo];
    }

    /** index = low bits of zobristKey (must be power-of-two table) */
    private int index(long zobristKey) {
        return (int) (zobristKey & (table.length - 1));
    }

    /**
     * Store an entry.  We replace if:
     *  - the slot was empty, OR
     *  - the stored key differs (collision), OR
     *  - the incoming depth >= stored depth (deeper or equal).
     */
    public void put(long zobristKey, int depth, int score, byte flag) {
        int i = index(zobristKey);
        Entry e = table[i];
        if (e == null) {
            e = new Entry();
        }
        // accept this new entry if it's same key with greater depth, or different key
        if (e.key != zobristKey || depth >= e.depth) {
            e.key   = zobristKey;
            e.depth = depth;
            e.score = score;
            e.flag  = flag;
            table[i] = e;
        }
    }

    /**
     * Retrieve the entry for this zobristKey, or null if absent / mismatched.
     */
    public Entry get(long zobristKey) {
        Entry e = table[index(zobristKey)];
        if (e != null && e.key == zobristKey) {
            return e;
        }
        return null;
    }
}
