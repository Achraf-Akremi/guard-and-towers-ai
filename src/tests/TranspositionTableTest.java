package tests;

import static org.junit.jupiter.api.Assertions.*;

import ki.TranspositionTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * JUnit tests for TranspositionTable with nicely formatted console output.
 * Ensure TranspositionTable.Entry is declared package‐private or public.
 */
class TranspositionTableTest {

    @Test
    @DisplayName("1) put/get Exact entry")
    void testPutGetExact() {
        TranspositionTable smallTT = new TranspositionTable(2);

        long key   = 0x11111111L;
        int  depth = 5;
        int  score = 123;
        byte flag  = TranspositionTable.FLAG_EXACT;

        // 1.1) Before insertion, it should be null
        assertNull(smallTT.get(key),
                () -> formatFailure("put/get Exact entry",
                        "initial get(key)",
                        "null",
                        describeEntry(smallTT.get(key))));

        // 1.2) Insert an EXACT entry
        smallTT.put(key, depth, score, flag);

        TranspositionTable.Entry e = smallTT.get(key);
        assertNotNull(e,
                () -> formatFailure("put/get Exact entry",
                        "entry presence",
                        "non-null",
                        "null"));

        assertEquals(depth, e.depth,
                () -> formatFailure("put/get Exact entry",
                        "depth",
                        String.valueOf(depth),
                        String.valueOf(e.depth)));
        assertEquals(score, e.score,
                () -> formatFailure("put/get Exact entry",
                        "score",
                        String.valueOf(score),
                        String.valueOf(e.score)));
        assertEquals(flag, e.flag,
                () -> formatFailure("put/get Exact entry",
                        "flag",
                        String.valueOf(flag),
                        String.valueOf(e.flag)));
        assertEquals(key, e.key,
                () -> formatFailure("put/get Exact entry",
                        "key",
                        "0x" + Long.toHexString(key),
                        "0x" + Long.toHexString(e.key)));

        // Pretty‐print result block
        System.out.println();
        System.out.println("────────────────────────────────────────────────────────");
        System.out.println("✔ [put/get Exact entry] PASSED");
        System.out.println("  Expected: depth=" + depth
                + ", score=" + score
                + ", flag=" + flag
                + ", key=0x" + Long.toHexString(key));
        System.out.println("  Got     : depth=" + e.depth
                + ", score=" + e.score
                + ", flag=" + e.flag
                + ", key=0x" + Long.toHexString(e.key));
        System.out.println("────────────────────────────────────────────────────────");
        System.out.println();
    }

    @Test
    @DisplayName("2) Lower/Upper bound behavior")
    void testLowerUpperBehavior() {
        TranspositionTable tt = new TranspositionTable(3);

        long key = 0x22222222L;

        // 2.1) Insert LOWER at depth=4
        tt.put(key, 4, 50, TranspositionTable.FLAG_LOWER);
        TranspositionTable.Entry e1 = tt.get(key);
        assertNotNull(e1,
                () -> formatFailure("Lower/Upper bound behavior",
                        "first put (depth=4, score=50, LOWER)",
                        "non-null",
                        "null"));
        assertEquals(4, e1.depth,
                () -> formatFailure("Lower/Upper bound behavior",
                        "depth after first put",
                        "4",
                        String.valueOf(e1.depth)));
        assertEquals(50, e1.score,
                () -> formatFailure("Lower/Upper bound behavior",
                        "score after first put",
                        "50",
                        String.valueOf(e1.score)));
        assertEquals(TranspositionTable.FLAG_LOWER, e1.flag,
                () -> formatFailure("Lower/Upper bound behavior",
                        "flag after first put",
                        String.valueOf(TranspositionTable.FLAG_LOWER),
                        String.valueOf(e1.flag)));

        // 2.2) Attempt overwrite with shallower UPPER at depth=2 (ignored)
        tt.put(key, 2, 30, TranspositionTable.FLAG_UPPER);
        TranspositionTable.Entry e2 = tt.get(key);
        assertNotNull(e2,
                () -> formatFailure("Lower/Upper bound behavior",
                        "entry presence after shallower put",
                        "non-null",
                        "null"));
        assertEquals(4, e2.depth,
                () -> formatFailure("Lower/Upper bound behavior",
                        "depth unchanged after shallower put",
                        "4",
                        String.valueOf(e2.depth)));
        assertEquals(50, e2.score,
                () -> formatFailure("Lower/Upper bound behavior",
                        "score unchanged after shallower put",
                        "50",
                        String.valueOf(e2.score)));

        // 2.3) Overwrite with EXACT at same depth=4
        tt.put(key, 4, 42, TranspositionTable.FLAG_EXACT);
        TranspositionTable.Entry e3 = tt.get(key);
        assertNotNull(e3,
                () -> formatFailure("Lower/Upper bound behavior",
                        "entry presence after EXACT overwrite",
                        "non-null",
                        "null"));
        assertEquals(4, e3.depth,
                () -> formatFailure("Lower/Upper bound behavior",
                        "depth after EXACT overwrite",
                        "4",
                        String.valueOf(e3.depth)));
        assertEquals(42, e3.score,
                () -> formatFailure("Lower/Upper bound behavior",
                        "score after EXACT overwrite",
                        "42",
                        String.valueOf(e3.score)));
        assertEquals(TranspositionTable.FLAG_EXACT, e3.flag,
                () -> formatFailure("Lower/Upper bound behavior",
                        "flag after EXACT overwrite",
                        String.valueOf(TranspositionTable.FLAG_EXACT),
                        String.valueOf(e3.flag)));

        // 2.4) Overwrite with deeper depth=6
        tt.put(key, 6, 99, TranspositionTable.FLAG_UPPER);
        TranspositionTable.Entry e4 = tt.get(key);
        assertNotNull(e4,
                () -> formatFailure("Lower/Upper bound behavior",
                        "entry presence after deeper put",
                        "non-null",
                        "null"));
        assertEquals(6, e4.depth,
                () -> formatFailure("Lower/Upper bound behavior",
                        "depth after deeper put",
                        "6",
                        String.valueOf(e4.depth)));
        assertEquals(99, e4.score,
                () -> formatFailure("Lower/Upper bound behavior",
                        "score after deeper put",
                        "99",
                        String.valueOf(e4.score)));
        assertEquals(TranspositionTable.FLAG_UPPER, e4.flag,
                () -> formatFailure("Lower/Upper bound behavior",
                        "flag after deeper put",
                        String.valueOf(TranspositionTable.FLAG_UPPER),
                        String.valueOf(e4.flag)));

        // Pretty‐print result block
        System.out.println();
        System.out.println("────────────────────────────────────────────────────────");
        System.out.println("✔ [Lower/Upper bound behavior] PASSED");
        System.out.println("  Final entry: depth=" + e4.depth
                + ", score=" + e4.score
                + ", flag=" + flagName(e4.flag));
        System.out.println("────────────────────────────────────────────────────────");
        System.out.println();
    }

    @Test
    @DisplayName("3) Collision replacement")
    void testCollisionReplacement() {
        TranspositionTable tinyTT = new TranspositionTable(1);

        long keyA = 0b00000001L;  // index = 1 & (2-1) = 1
        long keyB = 0b00100001L;  // index = 1 & (2-1) = 1 also

        // 3.1) Insert keyA at depth=3
        tinyTT.put(keyA, 3, 10, TranspositionTable.FLAG_EXACT);
        TranspositionTable.Entry eA = tinyTT.get(keyA);
        assertNotNull(eA,
                () -> formatFailure("Collision replacement",
                        "first put keyA",
                        "non-null",
                        "null"));
        assertEquals(3, eA.depth,
                () -> formatFailure("Collision replacement",
                        "depth for keyA",
                        "3",
                        String.valueOf(eA.depth)));
        assertEquals(10, eA.score,
                () -> formatFailure("Collision replacement",
                        "score for keyA",
                        "10",
                        String.valueOf(eA.score)));

        // 3.2) Insert keyB at depth=2 (same slot, collision) ⇒ evicts keyA
        tinyTT.put(keyB, 2, 20, TranspositionTable.FLAG_EXACT);

        assertNull(tinyTT.get(keyA),
                () -> formatFailure("Collision replacement",
                        "keyA eviction",
                        "null",
                        describeEntry(tinyTT.get(keyA))));

        TranspositionTable.Entry eB = tinyTT.get(keyB);
        assertNotNull(eB,
                () -> formatFailure("Collision replacement",
                        "keyB presence after collision put",
                        "non-null",
                        "null"));
        assertEquals(2, eB.depth,
                () -> formatFailure("Collision replacement",
                        "depth for keyB",
                        "2",
                        String.valueOf(eB.depth)));
        assertEquals(20, eB.score,
                () -> formatFailure("Collision replacement",
                        "score for keyB",
                        "20",
                        String.valueOf(eB.score)));

        // Pretty‐print result block
        System.out.println();
        System.out.println("────────────────────────────────────────────────────────");
        System.out.println("✔ [Collision replacement] PASSED");
        System.out.println("  keyA evicted (got null)");
        System.out.println("  keyB stored: depth=" + eB.depth
                + ", score=" + eB.score);
        System.out.println("────────────────────────────────────────────────────────");
        System.out.println();
    }

    @Test
    @DisplayName("4) Rewriting with deeper entry")
    void testRewritingWithDeeperEntry() {
        TranspositionTable tt = new TranspositionTable(2);

        long key = 0xAAAAL;
        int  d1  = 4, s1 = 11;
        tt.put(key, d1, s1, TranspositionTable.FLAG_LOWER);

        TranspositionTable.Entry e1 = tt.get(key);
        assertNotNull(e1,
                () -> formatFailure("Rewriting with deeper entry",
                        "first put (depth=4, score=11, LOWER)",
                        "non-null",
                        "null"));
        assertEquals(d1, e1.depth,
                () -> formatFailure("Rewriting with deeper entry",
                        "depth after first put",
                        String.valueOf(d1),
                        String.valueOf(e1.depth)));
        assertEquals(s1, e1.score,
                () -> formatFailure("Rewriting with deeper entry",
                        "score after first put",
                        String.valueOf(s1),
                        String.valueOf(e1.score)));

        // 4.1) Put shallower entry at depth=3 (score=5) ⇒ ignored
        tt.put(key, 3, 5, TranspositionTable.FLAG_UPPER);
        TranspositionTable.Entry e2 = tt.get(key);
        assertNotNull(e2,
                () -> formatFailure("Rewriting with deeper entry",
                        "entry presence after shallower put",
                        "non-null",
                        "null"));
        assertEquals(4, e2.depth,
                () -> formatFailure("Rewriting with deeper entry",
                        "depth unchanged after shallower put",
                        "4",
                        String.valueOf(e2.depth)));
        assertEquals(11, e2.score,
                () -> formatFailure("Rewriting with deeper entry",
                        "score unchanged after shallower put",
                        "11",
                        String.valueOf(e2.score)));

        // 4.2) Now write deeper entry at depth=6 ⇒ overwrites
        tt.put(key, 6, 99, TranspositionTable.FLAG_EXACT);
        TranspositionTable.Entry e3 = tt.get(key);
        assertNotNull(e3,
                () -> formatFailure("Rewriting with deeper entry",
                        "entry presence after deeper put",
                        "non-null",
                        "null"));
        assertEquals(6, e3.depth,
                () -> formatFailure("Rewriting with deeper entry",
                        "depth after deeper put",
                        "6",
                        String.valueOf(e3.depth)));
        assertEquals(99, e3.score,
                () -> formatFailure("Rewriting with deeper entry",
                        "score after deeper put",
                        "99",
                        String.valueOf(e3.score)));

        // Pretty‐print result block
        System.out.println();
        System.out.println("────────────────────────────────────────────────────────");
        System.out.println("✔ [Rewriting with deeper entry] PASSED");
        System.out.println("  Final entry: depth=" + e3.depth
                + ", score=" + e3.score);
        System.out.println("────────────────────────────────────────────────────────");
        System.out.println();
    }

    @Test
    @DisplayName("5) Basic sanity with 2^20‐sized TT")
    void testLargeTableBasicBehavior() {
        // Use a TranspositionTable with 2^20 slots (about one million entries)
        TranspositionTable largeTT = new TranspositionTable(20);

        long key1 = 0xABCDEFL;
        int  d1   = 7, s1 = 555;
        byte f1   = TranspositionTable.FLAG_LOWER;

        // 5.1) Put an entry into largeTT
        largeTT.put(key1, d1, s1, f1);
        TranspositionTable.Entry out1 = largeTT.get(key1);
        assertNotNull(out1,
                () -> formatFailure("Basic sanity (2^20 table)",
                        "get after put",
                        "non-null",
                        "null"));
        assertAll("entry fields in largeTT",
                () -> assertEquals(d1, out1.depth,
                        () -> "Expected depth=" + d1 + ", but got " + out1.depth),
                () -> assertEquals(s1, out1.score,
                        () -> "Expected score=" + s1 + ", but got " + out1.score),
                () -> assertEquals(f1, out1.flag,
                        () -> "Expected flag=" + f1 + ", but got " + out1.flag),
                () -> assertEquals(key1, out1.key,
                        () -> "Expected key=0x" + Long.toHexString(key1)
                                + ", but got 0x" + Long.toHexString(out1.key))
        );

        // 5.2) Overwrite with EXACT at same depth
        int s2 = 999;
        byte f2 = TranspositionTable.FLAG_EXACT;
        largeTT.put(key1, d1, s2, f2);
        TranspositionTable.Entry out2 = largeTT.get(key1);
        assertNotNull(out2,
                () -> formatFailure("Basic sanity (2^20 table)",
                        "get after overwrite",
                        "non-null",
                        "null"));
        assertAll("entry fields after overwrite in largeTT",
                () -> assertEquals(d1, out2.depth,
                        () -> "Expected depth=" + d1 + ", but got " + out2.depth),
                () -> assertEquals(s2, out2.score,
                        () -> "Expected score=" + s2 + ", but got " + out2.score),
                () -> assertEquals(f2, out2.flag,
                        () -> "Expected flag=" + f2 + ", but got " + out2.flag),
                () -> assertEquals(key1, out2.key,
                        () -> "Expected key=0x" + Long.toHexString(key1)
                                + ", but got 0x" + Long.toHexString(out2.key))
        );

        System.out.println();
        System.out.println("────────────────────────────────────────────────────────");
        System.out.println("✔ [Basic sanity with 2^20‐sized TT] PASSED");
        System.out.println("  Entry after first put : depth=" + d1 + ", score=" + s1
                + ", flag=" + flagName(f1)
                + ", key=0x" + Long.toHexString(key1));
        System.out.println("  Entry after overwrite: depth=" + out2.depth
                + ", score=" + out2.score
                + ", flag=" + flagName(out2.flag)
                + ", key=0x" + Long.toHexString(out2.key));
        System.out.println("────────────────────────────────────────────────────────");
        System.out.println();
    }

    /** Helper: formats a failure message showing test name, field, expected, and got */
    private static String formatFailure(String testName, String field, String expected, String got) {
        return String.format(
                "[%s] FAILED: %s → Expected %s, but got %s",
                testName, field, expected, got
        );
    }

    /** Helper: describes an entry or “null” */
    private static String describeEntry(TranspositionTable.Entry e) {
        if (e == null) return "null";
        return String.format("(key=0x%s, depth=%d, score=%d, flag=%s)",
                Long.toHexString(e.key), e.depth, e.score, flagName(e.flag));
    }

    /** Helper: convert flag byte to human‐readable name */
    private static String flagName(byte f) {
        switch (f) {
            case TranspositionTable.FLAG_EXACT: return "EXACT";
            case TranspositionTable.FLAG_LOWER: return "LOWER";
            case TranspositionTable.FLAG_UPPER: return "UPPER";
            default: return "UNKNOWN(" + f + ")";
        }
    }
}
