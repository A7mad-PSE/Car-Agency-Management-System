public class Service {
    private String serviceId;
    private int vehicleId;
    private String customerId;
    private String serviceType;
    private String date;
    private double cost;
    private String status;
    public Service(String serviceId, int vehicleId, String customerId, String serviceType, String date, double cost, String status){
        this.serviceId = serviceId;
        this.vehicleId = vehicleId;
        this.customerId = customerId;
        this.serviceType = serviceType;
        this.date = date;
        this.cost = cost;
        this.status = status;
    }
    public String getServiceId(){
        return serviceId;
    }
    // No setServiceId: serviceId is immutable.
    public int getVehicleId(){
        return vehicleId;
    }
    public void setVehicleId(int vehicleId){
        this.vehicleId = vehicleId;
    }
    public String getCustomerId(){
        return customerId;
    }
    public void setCustomerId(String customerId){
        this.customerId = customerId;
    }
    public String getServiceType(){
        return serviceType;
    }
    public void setServiceType(String serviceType){
        this.serviceType = serviceType;
    }
    public String getDate(){
        return date;
    }
    public void setDate(String date){
        this.date = date;
    }
    public double getCost(){
        return cost;
    }
    public void setCost(double cost){
        this.cost = cost;
    }
    public String getStatus(){
        return status;
    }
    public void setStatus(String status){
        this.status = status;
    }
    public String toString(){
        return "Service ID: " + serviceId + ", Vehicle ID: " + vehicleId + ", Customer ID: " + customerId +
                ", Service Type: " + serviceType + ", Date: " + date + ", Cost: " + cost + ", Status: " + status;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Service)) return false;
        Service s = (Service) o;
        return serviceId != null && serviceId.equals(s.serviceId);
    }
    @Override
    public int hashCode() { return serviceId == null ? 0 : serviceId.hashCode(); }
}