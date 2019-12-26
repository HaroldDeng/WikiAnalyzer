package graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safed data structure
 * 
 * This an undirectional star graph implementation using {@link GraphNode}. The graph
 *     is sorted by search history. Reorder is control via. a counter, reorder if it 
 *     reaches 0.
 * The node with more visits should closer to root compare to current subgraph nodes. Means
 *     no lateral node replacement. // TODO, wait for future update
 * 
 * This graph supports multi-thread multi-read multi-write operation. For single node, 
 *     support multi-thread multi-read or multi-thread multi-write, but not both.
 * 
 * Singleton pattern design allow all reference refer to the same graph, one grpah accross
 *     entire program.
 * 
 * 
 * ! Warnning, muliple greedy write-locks collide with muliple greedy read-locks on one node
 * !     would cause starvation. // TODO, wait for future update
 */
public class HeuristicSearchGraph implements Graph {
    private static HeuristicSearchGraph HSG;

    private final int BUCKETSIZE = 5;
    private final int MIN = 4;
    private final double FACTOR = 0.25;

    // Norminal size of the graph, indicate number of nodes attach to the graph
    private AtomicInteger size = new AtomicInteger(0);
    private AtomicInteger countdown = new AtomicInteger(MIN);

    private GraphNode graphRoot;

    // path to insert next node
    private int[] path = new int[0];

    /**
     * Insert new element into the graph
     * 
     * All threads are synchronized at {@link #genPath()} method. This is the only 
     *     golbal synchronization point in this method. It allows each element has 
     *     a unique path to insertion point.  If path node havn't completed yet,
     *     wait indefinitely until path node completes.
     * 
     * @param item new data insert to graph
     * 
     */
    @Override
    public void insert(String item) {
        int[] targetPath = genPath();
        if (targetPath.length == 0) {
            // graph root node, very first node
            graphRoot = genNode(item, null);
            size.incrementAndGet();
            return;
        }

        while (graphRoot == null) {
            // wait until root is builded
        }
        GraphNode node = graphRoot;
        for (int i = 0; i < targetPath.length - 1; ++i) {

            // multi-write write lock
            GraphNode.wr_lock(node, true);
            while (node.next[targetPath[i]] == null) {
                // path node havn't build yet
            }

            node = node.next[targetPath[i]];
            GraphNode.wr_free(node.prev);
        }

        GraphNode.wr_lock(node, true);
        node.next[targetPath[targetPath.length - 1]] = genNode(item, node);
        size.incrementAndGet();
        GraphNode.wr_free(node);
    }

    /**
     * Search target in graph. If {@code countdown} is 0, reform the graph 
     *     accodring to node's heat (search history), higher the heat value,
     *     closer to the root.
     * 
     * No global synchronized needed because lookup doesn't modify nodes.
     * 
     * @param item target
     * @return {@code true} if target found, {@code false} otherwise
     */
    @Override
    public boolean contains(String item) {
        if (size.get() == 0) {
            return false;
        }

        GraphNode node = retrieveNodes(item, graphRoot);
        if (node == null) {
            // item not found
            return false;
        }
        ++node.heat;

        if (countdown.decrementAndGet() == 0) {
            reform(graphRoot);
            countdown.set(Math.max(MIN, (int) (size.get() * FACTOR)));
        }

        return true;
    }

    /**
     * @return norminal size of the graph
     */
    @Override
    public int size() {
        return size.get();
    }

    /**
     * Empting the graph.
     * 
     * ! Warnning, using {@link #clear()} concurrently with other methods such as {@link #insert(String)} 
     * !    or {@link #contains(String)} might cause error. 
     * 
     */
    @Override
    public synchronized void clear() {
        if (size.get() == 0) {
            return;
        }

        for (Object obj : traversal(graphRoot)) {
            GraphNode node = (GraphNode) obj;
            GraphNode.clear(node);
        }

        size.set(0);
        countdown.set(MIN);
        graphRoot = null;
        path = new int[0];

        System.gc();
    }

    /**
     * BFS order traversal.
     * 
     * No global synchronized needed because lookup doesn't modify nodes.
     * 
     * @return array of contents in graph in BFS order
     */
    @Override
    public String[] traversal() {
        if (size.get() == 0) {
            return new String[0];
        }

        Object[] ret = traversal(graphRoot);
        String[] res = new String[ret.length];
        for (int i = 0; i < res.length; ++i) {
            res[i] = ((GraphNode) ret[i]).data;
        }

        return res;
    }

    /**
     * singleton desgin
     * 
     * This method is synchronized, one thread access at a time
     * 
     * @return {code HeuristicSearchGraph} object
     */
    public static synchronized HeuristicSearchGraph getInstance() {
        if (HSG == null) {
            HSG = new HeuristicSearchGraph();
        }
        return HSG;
    }

    /**
     * private constructor
     */
    private HeuristicSearchGraph() {
    }

    /**
     * BFS traversal, read {@code root} and all subgraph nodes under it. Stop if an 
     *     empty child encounted. 
     * 
     * Local synchronization (read-lock) needed because lookup does accessing 
     *     content.
     * 
     * @param root top node
     * @exception NullPointerException parameter is {@code null}
     */
    private Object[] traversal(GraphNode root) {
        ArrayList<GraphNode> list = new ArrayList<GraphNode>();
        int index = 0;
        list.add(root);

        // traversal until no more node
        while (true) {
            root = list.get(index);

            // read-lock
            GraphNode.re_lock(root, true);

            for (GraphNode node : root.next) {
                if (node != null) {
                    list.add(node);
                } else {
                    // free read-lock
                    GraphNode.re_free(root);
                    return list.toArray();
                }
            }
            ++index;

            // free read lock
            GraphNode.re_free(root);
        }
    }

    /**
     * From the subgraph, retrieve node with given data
     * 
     * @param item target
     * @param root top node
     * @return a {@code GraphNode} contains item, or {@code null} 
     *     if taget not found
     * @exception NullPointerException parameters are {@code null}
     */
    private GraphNode retrieveNodes(String item, GraphNode root) {
        LinkedList<GraphNode> queue = new LinkedList<GraphNode>();
        queue.addLast(root);

        while (queue.size() > 0) {
            GraphNode node = queue.pollFirst();

            if (GraphNode.re_lock(node, false)) {
                if (node.data.equals(item)) {
                    GraphNode.re_free(node);
                    return node; // found
                }

                // insert child to the queue
                for (GraphNode child : node.next) {
                    if (child == null) {
                        break;
                    }
                    queue.addLast(child);
                }

                GraphNode.re_free(node);

            } else {
                // Fail to acquire read lock, put back to queue
                queue.push(node);
            }
        }

        // no match found
        return null;
    }

    /**
     * Reform subgraph in DFS order. {@code GraphNode} with higher {@code head}
     *     value should closer to root than others
     * 
     * @param root subgraph root
     * @return root of sorted subgraph
     */
    private GraphNode reform(GraphNode root) {
        for (GraphNode child : root.next) {
            if (child != null) {
                reform(child);
                if (root.heat < child.heat) {
                    GraphNode.swap(root, child);
                }
                continue;
            }
            break;
        }
        return root;
    }

    /** 
     * Generate a {@code GraphNode}
     * 
     * @param item data stores in node
     * @param  parent node
     * @return newly generate graph node
     * @exception OutOfMemoryError insufficient memory
     * */
    private GraphNode genNode(String item, GraphNode prev) {
        GraphNode node = new GraphNode();
        node.data = item;
        node.prev = prev;
        node.next = new GraphNode[BUCKETSIZE];
        return node;
    }

    /**
     * Generate path to next new node
     * 
     * @return path to next new node
     */
    private synchronized int[] genPath() {
        int[] res = Arrays.copyOf(path, path.length);
        boolean carry = false;

        for (int i = path.length - 1; i > -1; --i) {
            path[i] += 1;
            if (path[i] >= BUCKETSIZE) {
                // node is full, not empty space
                path[i] = 0;
                carry = true;
            } else {
                carry = false;
                break;
            }
        }

        if (carry || res.length == 0) {
            path = new int[path.length + 1];
        }

        return res;
    }

    /**
     * Print nodes infomation in BFS order
     */
    protected void DEBUG() {
        Object[] ret = traversal(graphRoot);
        for (Object obj : ret) {
            GraphNode node = (GraphNode) obj;
            System.out.println(node.toString());
        }
    }
}