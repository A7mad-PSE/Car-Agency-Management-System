public class Vehicle implements Comparable<Vehicle> {
    private int vehicleId;
    private String make;
    private String model;
    private int year;
    private double price;
    private String color;
    private String status;
    public Vehicle(int vehicleId, String make, String model, int year, double price, String color, String status){
        this.vehicleId = vehicleId;
        this.make = make;
        this.model = model;
        this.year = year;
        this.price = price;
        this.color = color;
        this.status = status;
    }
    public int getVehicleId(){
        return vehicleId;
    }
    // No setVehicleId: vehicleId is the AVL key and must never change.
    public String getMake(){
        return make;
    }
    public void setMake(String make){
        this.make = make;
    }
    public String getModel(){
        return model;
    }
    public void setModel(String model){
        this.model = model;
    }
    public int getYear(){
        return year;
    }
    public void setYear(int year){
        this.year = year;
    }
    public double getPrice(){
        return price;
    }
    public void setPrice(double price){
        this.price = price;
    }
    public String getColor(){
        return color;
    }
    public void setColor(String color){
        this.color = color;
    }
    public String getStatus(){
        return status;
    }
    public void setStatus(String status){
        this.status = status;
    }
    public Vehicle(Vehicle other) {
        this(other.vehicleId, other.make, other.model, other.year, other.price, other.color, other.status);
    }

    public void copyFrom(Vehicle other) {
        // Key vehicleId intentionally not copied.
        this.make = other.make;
        this.model = other.model;
        this.year = other.year;
        this.price = other.price;
        this.color = other.color;
        this.status = other.status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vehicle)) return false;
        return vehicleId == ((Vehicle) o).vehicleId;
    }
    @Override
    public int hashCode() { return Integer.hashCode(vehicleId); }

    public int compareTo(Vehicle other){
        if(this.vehicleId < other.vehicleId){
            return -1;
        }
        if(this.vehicleId > other.vehicleId){
            return 1;
        }
        return 0;
    }
    public String toString(){
        return "Vehicle ID: " + vehicleId + ", Make: " + make + ", Model: " + model +
                ", Year: " + year + ", Price: " + price + ", Color: " + color + ", Status: " + status;
    }
}