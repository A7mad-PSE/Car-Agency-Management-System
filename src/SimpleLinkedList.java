public class SimpleLinkedList<T> {
    private static class Node<T> {
        T data;
        Node<T> next;
        Node(T data) { this.data = data; }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size;

    public SimpleLinkedList() { head = null; tail = null; size = 0; }

    public void add(T item) {
        Node<T> n = new Node<>(item);
        if (head == null) head = tail = n;
        else { tail.next = n; tail = n; }
        size++;
    }

    public void addFirst(T item) {
        Node<T> n = new Node<>(item);
        n.next = head;
        head = n;
        if (tail == null) tail = n;
        size++;
    }

    public void insertAt(int index, T item) {
        if (index <= 0) { addFirst(item); return; }
        if (index >= size) { add(item); return; }
        Node<T> cur = head;
        for (int i = 0; i < index - 1; i++) cur = cur.next;
        Node<T> n = new Node<>(item);
        n.next = cur.next;
        cur.next = n;
        size++;
    }

    public boolean remove(T item) {
        Node<T> cur = head, prev = null;
        while (cur != null) {
            boolean eq = (item == null) ? cur.data == null : item.equals(cur.data);
            if (eq) {
                if (prev == null) {
                    head = cur.next;
                    if (head == null) tail = null;
                } else {
                    prev.next = cur.next;
                    if (cur == tail) tail = prev;
                }
                size--;
                return true;
            }
            prev = cur;
            cur = cur.next;
        }
        return false;
    }

    public boolean contains(T item) {
        return indexOf(item) >= 0;
    }

    public int indexOf(T item) {
        Node<T> cur = head;
        int i = 0;
        while (cur != null) {
            boolean eq = (item == null) ? cur.data == null : item.equals(cur.data);
            if (eq) return i;
            cur = cur.next;
            i++;
        }
        return -1;
    }

    public T get(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
        Node<T> cur = head;
        for (int i = 0; i < index; i++) cur = cur.next;
        return cur.data;
    }

    public int size() { return size; }
    public int getSize() { return size; }
    public boolean isEmpty() { return size == 0; }

    public void clear() { head = null; tail = null; size = 0; }

    public Object[] toArray() {
        Object[] arr = new Object[size];
        Node<T> cur = head;
        int i = 0;
        while (cur != null) { arr[i++] = cur.data; cur = cur.next; }
        return arr;
    }
}
