package graph;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import java.util.Random;

public class GraphTest {
    private HeuristicSearchGraph hsg;

    @Before
    public void setup() {
        hsg = HeuristicSearchGraph.getInstance();
    }

    /**
     * Testing 
     * {@link HeuristicSearchGraph#size()}
     * {@link HeuristicSearchGraph#traversal()} BFS order traversal
     * {@link HeuristicSearchGraph#clear()} should pass HSG_1() test case if correctly implemented
     *
     * Test case also tests singleton desgin, only one copy of object exist in the program
     */
    @Test
    public void HSG_0() {
        HeuristicSearchGraph a = HeuristicSearchGraph.getInstance();
        HeuristicSearchGraph b = HeuristicSearchGraph.getInstance();
        assertEquals(a, b);
        assertNotNull(a);

        assertFalse(a.contains("item"));
        assertArrayEquals(new String[] {}, b.traversal());

        a.insert("123");
        a.insert("456");
        assertEquals(2, b.size());
        assertArrayEquals(new String[] { "123", "456" }, b.traversal());

        b.clear();
        assertEquals(0, a.size());
    }

    /**
     * Testing 
     * {@link HeuristicSearchGraph#contains(String)}
     * {@link HeuristicSearchGraph#reform()} should not be call if {@code countdown} is not 0
     * 
     */
    @Test
    public void HSG_1() {

        String[] res = intArray(0, 20);
        for (String s : res) {
            assertTrue(!hsg.contains(s));
            hsg.insert(s);
            assertTrue(hsg.contains(s));
        }
        assertEquals(20, hsg.size());

        for (int i = 0; i < 4; ++i) {
            assertTrue(hsg.contains(res[12]));
        }
        assertArrayEquals(hsg.traversal(), res);
        hsg.clear();
    }

    /**
     * Testing 
     * {@link HeuristicSearchGraph#reform()} should be call if {@code countdown} is 0
     *     greater the heat value, closer to the graph root. {@code countdown} will 
     *     reset to {@code max(MIN, size.get() * FACTOR)}
     */
    @Test
    public void HSG_3() {
        hsg.insert("a");
        hsg.contains("a");
        hsg.contains("a");
        hsg.contains("a");
        assertArrayEquals(new String[] { "a" }, hsg.traversal());
        hsg.contains("a");
        hsg.contains("a");
        assertArrayEquals(new String[] { "a" }, hsg.traversal());
        hsg.insert("b");
        hsg.insert("c");
        hsg.insert("d");
        hsg.insert("e");
        hsg.insert("f");
        hsg.insert("g");
        hsg.insert("h");

        hsg.contains("h");
        hsg.contains("h");
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e", "f", "g", "h" }, hsg.traversal());
        hsg.contains("h");
        assertArrayEquals(new String[] { "a", "h", "c", "d", "e", "f", "g", "b" }, hsg.traversal());

        // if BUCKETSIZE is 5, then countdown should be 4 in here

        hsg.clear();
    }

    /**
    * Testing 
    * {@link HeuristicSearchGraph#insert(String)} duplicates are allowed
    * {@link HeuristicSearchGraph#contains(String)} always search the nodes closest to root first (BFS order)
    * {@link HeuristicSearchGraph#reform()}, DFS order
    */
    @Test
    public void HSG_4() {
        hsg.insert("aaa");
        hsg.insert("bbb");
        hsg.insert("bbb");
        hsg.insert("ccc");
        hsg.insert("ddd");
        hsg.insert("ddd");
        hsg.insert("bbb");

        hsg.contains("bbb");
        hsg.contains("bbb");
        hsg.contains("bbb");
        hsg.contains("bbb");

        assertArrayEquals(new String[] { "bbb", "aaa", "bbb", "ccc", "ddd", "ddd", "bbb" }, hsg.traversal());
        hsg.clear();
    }

    /** 
     * Testing concurrency 
     * {@link HeuristicSearchGraph#insert(String)} concurren insert duplicate elements
     * {@link HeuristicSearchGraph#contains(String)} concurren lookup
     * 
    */
    @Test
    public void HSG_5() {
        for (int i = 0; i < 5; ++i) {
            new Thread(() -> {
                hsg.insert("123");
                assertTrue(hsg.contains("123"));
            }).start();
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
        }

        assertArrayEquals(new String[] { "123", "123", "123", "123", "123" }, hsg.traversal());
        assertEquals(5, hsg.size());
        hsg.clear();
    }

    /** 
     * Testing concurrency, more inputs, more threads
    */
    @Test
    public void HSG_6() throws InterruptedException {
        Thread[] arr = new Thread[5];
        for (int i = 0; i < 5; ++i) {
            arr[i] = new Thread(() -> {
                String[] ret = ranStringArray(64);
                for (String s : ret) {
                    hsg.insert(s);
                    assertTrue(hsg.contains(s));
                    hsg.traversal();
                }
            });
        }
        for (Thread t : arr) {
            t.run();
        }
        for (Thread t : arr) {
            t.join();
        }
        assertEquals(5 * 64, hsg.size());
        assertEquals(5 * 64, hsg.traversal().length);
    }

    private String[] ranStringArray(int n) {
        String[] res = new String[n];
        Random r = new Random();

        for (int i = 0; i < n; ++i) {
            int t = r.nextInt(19) + 1;
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < t; ++j) {
                sb.append((char) (r.nextInt(26) + 'a'));
            }
            res[i] = sb.toString();
        }
        return res;
    }

    private String[] intArray(int start, int end) {
        int stop = (end - start);
        String[] res = new String[stop];
        for (int i = 0; i < stop; ++i) {
            res[i] = String.valueOf(start);
            ++start;
        }
        return res;
    }

}