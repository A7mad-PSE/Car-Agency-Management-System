public class Customer {
    private String customerId;
    private String name;
    private String phone;
    private String address;
    public Customer(String customerId, String name, String phone, String address){
        this.customerId = customerId;
        this.name = name;
        this.phone = phone;
        this.address = address;
    }
    public String getCustomerId(){
        return customerId;
    }
    // No setCustomerId: customerId is the primary key and must never change.
    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name = name;
    }
    public String getPhone(){
        return phone;
    }
    public void setPhone(String phone){
        this.phone = phone;
    }
    public String getAddress(){
        return address;
    }
    public void setAddress(String address){
        this.address = address;
    }
    public Customer(Customer other) {
        this(other.customerId, other.name, other.phone, other.address);
    }

    public void copyFrom(Customer other) {
        // Key customerId intentionally not copied.
        this.name = other.name;
        this.phone = other.phone;
        this.address = other.address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer)) return false;
        Customer c = (Customer) o;
        return customerId != null && customerId.equals(c.customerId);
    }
    @Override
    public int hashCode() { return customerId == null ? 0 : customerId.hashCode(); }

    public String toString(){
        return "Customer ID: " + customerId + ", Name: " + name + ", Phone: " + phone + ", Address: " + address;
    }
}