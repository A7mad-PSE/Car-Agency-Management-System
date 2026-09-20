public class AVLNode {
    private int key;
    private Vehicle vehicle;
    private AVLNode left;
    private AVLNode right;
    private int height;

    public AVLNode(Vehicle vehicle) {
        this.key = vehicle.getVehicleId();
        this.vehicle = vehicle;
        this.left = null;
        this.right = null;
        this.height = 1;
    }

    public int getKey() { return key; }
    public void setKey(int key) { this.key = key; }
    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
    public AVLNode getLeft() { return left; }
    public void setLeft(AVLNode left) { this.left = left; }
    public AVLNode getRight() { return right; }
    public void setRight(AVLNode right) { this.right = right; }
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
}
