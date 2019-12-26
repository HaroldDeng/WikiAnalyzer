package graph;

public interface Graph {
    void insert(String item);

    // TODO, wait for future update
    // void pop(String item);

    boolean contains(String item);

    int size();

    void clear();

    String[] traversal();
}
