package graph;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Node uses by Graph class
 * 
 * This class provides locked multi-producer multi-consumer graph node 
 *     for {@link HeuristicSearchGraph}. Both lock are implemented using 
 *     {@link AtomicInteger}.
 * 
 * There is only one copy of static method exist in memory within one program.
 *     Which will reduces memory consumption, but also able to access private 
 *     variable. Reinfore encapsulation.
 * 
 * 
 * {@code consumer} read-lock
 * {@code producer} write-lock
 * 
 * ! Warnning, beware of starvation. For example, muliple greedy write-locks
 * !    collide with muliple greedy read-locks
 * 
 */
class GraphNode {
    // thread-safe, also reduce total overhead
    private AtomicInteger consumer = new AtomicInteger(0); // read-lock
    private AtomicInteger producer = new AtomicInteger(0); // write-lock

    protected String data;
    protected GraphNode prev;
    protected GraphNode next[];
    protected int heat;

    /**
     * read-lock (consumer lock)
     * Allowing mutiple read concurrenly. Will place lock if write-lock is off.
     * 
     * @param node target
     * @param wait indicator for wait indefinitely
     * @return {@code true} if placing lock successful, {@code false} otherwise
     */
    public static boolean re_lock(GraphNode node, boolean wait) {
        node.consumer.incrementAndGet();

        if (wait) {
            while (node.producer.get() > 0) {
                // wait indefinitely
            }
            return true;
        }

        if (node.producer.get() > 0) {
            // give up
            node.consumer.decrementAndGet();
            return false;
        }
        return true;
    }

    /**
    * write-lock (producer lock)
    * Allowing mutiple write concurrenly. Will place lock if read-lock is off.
    * 
    * @param node target
    * @param wait indicator for wait indefinitely
    * @return {@code true} if placing lock successful, {@code false} otherwise
    */
    protected static boolean wr_lock(GraphNode node, boolean wait) {
        node.producer.incrementAndGet();

        if (wait) {
            while (node.consumer.get() > 0) {
                // wait until all clear
            }
            return true;
        }

        if (node.consumer.get() > 0) {
            // give up
            node.producer.decrementAndGet();
            return false;
        }
        return true;

    }

    /**
     * Reduce read-lock by 1 (reduce consumer lock by 1)
     * 
     * @param node target
     */
    protected static void re_free(GraphNode node) {
        node.consumer.decrementAndGet();
    }

    /**
     * Reduce write-lock by 1 (reduce producer lock by 1)
     * 
     * @param node target
     */
    protected static void wr_free(GraphNode node) {
        node.producer.decrementAndGet();
    }

    /**
    * Content swap. 
    * 
    * @param a first {@code GraphNode}
    * @param b second {@code GraphNode}
    * @exception NullPointerException at lease one parameter is {@code null}
    */
    protected static void swap(GraphNode a, GraphNode b) {
        String tmpD = a.data;
        a.data = b.data;
        b.data = tmpD;
        int tmp = a.heat;
        a.heat = b.heat;
        b.heat = tmp;

        a.producer.set(b.producer.getAndSet(a.producer.get()));
        a.consumer.set(b.consumer.getAndSet(a.consumer.get()));
    }

    /**
     * remove all references
     */
    protected static void clear(GraphNode node) {
        node.data = null;
        node.prev = null;
        Arrays.fill(node.next, null);
        node.next = null;
    }

    @Override
    public String toString() {
        String nt;
        if (prev == null) {
            nt = "null [ ";
        } else {
            nt = prev.data + " [ ";
        }
        for (GraphNode n : next) {
            if (n != null) {
                nt += n.data + " ";
            } else {
                nt += "null ";
            }
        }
        return String.format("%s %s] %d %d %d", data, nt, heat, consumer.get(), producer.get());
    }
}