public class Stack<T>{
    // Linked-list based LIFO. No java.util collections used internally.
    private Node<T> top;
    private int size;
    private class Node<T>{
        T data;
        Node<T> next;
        Node(T data){
            this.data=data;
            this.next=null;
        }
    }
public Stack(){
  top=null;
  size=0;
}
public boolean isEmpty(){
        return top==null;
}
public void clear(){
        top=null;
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
     return top.data;
}
public void push(T data){
        Node<T> newnode=new Node<T>(data);
        newnode.next=top;
        top=newnode;
        size++;
}
public T pop(){
        if(isEmpty()){
            return null;
        }
        T value=top.data;
        top=top.next;
        size--;
        return value;
    }
public Object[] toArray() {
        Object[] arr = new Object[size];
        Node<T> cur = top;
        for (int i = 0; cur != null; i++) {
            arr[i] = cur.data;
            cur = cur.next;
        }
        return arr;
    }
}