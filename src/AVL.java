public class AVL {
    // Vehicle inventory ordered by vehicleId. Storage is AVLNode chain only;
    // snapshots are handed out as custom SimpleLinkedList (no java.util collections).
    private AVLNode root;

    public void insert(Vehicle vehicle) {
        root = insertRec(root, vehicle);
    }

    private AVLNode insertRec(AVLNode node, Vehicle vehicle) {
        if (node == null) {
            return new AVLNode(vehicle);
        }
        if (vehicle.getVehicleId() < node.getKey()) {
            node.setLeft(insertRec(node.getLeft(), vehicle));
        } else if (vehicle.getVehicleId() > node.getKey()) {
            node.setRight(insertRec(node.getRight(), vehicle));
        } else {
            return node;
        }
        updateHeight(node);
        return rebalance(node);
    }

    public Vehicle find(int vehicleId) {
        AVLNode node = findRec(root, vehicleId);
        return node == null ? null : node.getVehicle();
    }

    public boolean search(int vehicleId) {
        return findRec(root, vehicleId) != null;
    }

    private AVLNode findRec(AVLNode node, int vehicleId) {
        if (node == null) return null;
        if (vehicleId == node.getKey()) return node;
        if (vehicleId < node.getKey()) return findRec(node.getLeft(), vehicleId);
        return findRec(node.getRight(), vehicleId);
    }

    public void delete(int vehicleId) {
        root = deleteRec(root, vehicleId);
    }

    private AVLNode deleteRec(AVLNode node, int vehicleId) {
        if (node == null) {
            return null;
        }
        if (vehicleId < node.getKey()) {
            node.setLeft(deleteRec(node.getLeft(), vehicleId));
        } else if (vehicleId > node.getKey()) {
            node.setRight(deleteRec(node.getRight(), vehicleId));
        } else {
            if (node.getLeft() == null || node.getRight() == null) {
                node = (node.getLeft() != null) ? node.getLeft() : node.getRight();
            } else {
                AVLNode successor = findMin(node.getRight());
                node.setVehicle(successor.getVehicle());
                node.setKey(successor.getKey());
                node.setRight(deleteRec(node.getRight(), successor.getKey()));
            }
        }
        if (node == null) {
            return null;
        }
        updateHeight(node);
        return rebalance(node);
    }

    private AVLNode findMin(AVLNode node) {
        if (node.getLeft() == null) {
            return node;
        }
        return findMin(node.getLeft());
    }

    public int getTreeHeight() {
        return heightOf(root);
    }

    private int heightOf(AVLNode node) {
        return node == null ? 0 : node.getHeight();
    }

    private void updateHeight(AVLNode node) {
        node.setHeight(1 + Math.max(heightOf(node.getLeft()), heightOf(node.getRight())));
    }

    private int getBalanceFactor(AVLNode node) {
        return node == null ? 0 : heightOf(node.getLeft()) - heightOf(node.getRight());
    }

    private AVLNode rebalance(AVLNode node) {
        int bf = getBalanceFactor(node);
        if (bf > 1 && getBalanceFactor(node.getLeft()) >= 0) {
            return rightRotate(node);
        }
        if (bf > 1 && getBalanceFactor(node.getLeft()) < 0) {
            node.setLeft(leftRotate(node.getLeft()));
            return rightRotate(node);
        }
        if (bf < -1 && getBalanceFactor(node.getRight()) <= 0) {
            return leftRotate(node);
        }
        if (bf < -1 && getBalanceFactor(node.getRight()) > 0) {
            node.setRight(rightRotate(node.getRight()));
            return leftRotate(node);
        }
        return node;
    }

    private AVLNode rightRotate(AVLNode y) {
        AVLNode x = y.getLeft();
        y.setLeft(x.getRight());
        x.setRight(y);
        updateHeight(y);
        updateHeight(x);
        return x;
    }

    private AVLNode leftRotate(AVLNode x) {
        AVLNode y = x.getRight();
        x.setRight(y.getLeft());
        y.setLeft(x);
        updateHeight(x);
        updateHeight(y);
        return y;
    }

    public SimpleLinkedList<Vehicle> inOrderVehicles() {
        SimpleLinkedList<Vehicle> list = new SimpleLinkedList<>();
        inOrderRec(root, list);
        return list;
    }

    private void inOrderRec(AVLNode node, SimpleLinkedList<Vehicle> list) {
        if (node == null) return;
        inOrderRec(node.getLeft(), list);
        list.add(node.getVehicle());
        inOrderRec(node.getRight(), list);
    }

    public SimpleLinkedList<Vehicle> reverseInOrderVehicles() {
        SimpleLinkedList<Vehicle> list = new SimpleLinkedList<>();
        reverseInOrderRec(root, list);
        return list;
    }

    private void reverseInOrderRec(AVLNode node, SimpleLinkedList<Vehicle> list) {
        if (node == null) return;
        reverseInOrderRec(node.getRight(), list);
        list.add(node.getVehicle());
        reverseInOrderRec(node.getLeft(), list);
    }

}