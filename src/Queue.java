public class Queue<T> {
    // Linked-list based FIFO with front/rear pointers. No java.util collections used internally.
    private Node<T> front;
    private Node<T> rear;
    private int size;
    private class Node<T>{
        T data;
        Node<T> next;
        Node(T data){
            this.data=data;
            this.next=null;
        }
    }
public Queue(){
        front=null;
        rear=null;
        size=0;
    }
public boolean isEmpty(){
        return front==null;
    }
    public void clear(){
        front=null;
        rear=null;
        size=0;
    }
public int getSize(){
        return size;
    }
public int size(){
        return size;
    }
public T peek(){
        if(isEmpty()){
            return null;
        }
   return front.data;
}
public void enqueue(T data){
        Node<T> newnode=new Node<T>(data);
        if(isEmpty()){
            front=newnode;
            rear=newnode;
        }else{
            rear.next=newnode;
            rear=newnode;
        }
        size++;
    }
public T dequeue(){
        if(isEmpty()){
            return null;

        }
        T value=front.data;
        front=front.next;
        if(front==null){
            rear=null;
        }
        size--;
        return value;
    }
public Object[] toArray() {
        Object[] arr = new Object[size];
        Node<T> cur = front;
        for (int i = 0; cur != null; i++) {
            arr[i] = cur.data;
            cur = cur.next;
        }
        return arr;
    }
public void insertAt(int index, T data){
        if (index <= 0) {
            Node<T> n = new Node<T>(data);
            n.next = front;
            front = n;
            if (rear == null) rear = n;
            size++;
            return;
        }
        if (index >= size) { enqueue(data); return; }
        Node<T> cur = front;
        for (int i = 0; i < index - 1; i++) cur = cur.next;
        Node<T> n = new Node<T>(data);
        n.next = cur.next;
        cur.next = n;
        size++;
    }
}
