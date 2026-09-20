public class ServiceRequest {
    private String requestId;
    private int vehicleId;
    private String customerId;
    private String serviceType;
    private String date;
    private String status;
    public ServiceRequest(String requestId, int vehicleId, String customerId, String serviceType, String date, String status){
        this.requestId = requestId;
        this.vehicleId = vehicleId;
        this.customerId = customerId;
        this.serviceType = serviceType;
        this.date = date;
        this.status = status;
    }
    public String getRequestId(){
        return requestId;
    }
    // No setRequestId: requestId is immutable.
    public int getVehicleId(){
        return vehicleId;
    }
    public void setVehicleId(int vehicleId){
        this.vehicleId = vehicleId;
    }
    public String getCustomerId() {
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
    public String getStatus(){
        return status;
    }
    public void setStatus(String status){
        this.status = status;
    }
    public String toString(){
        return "Request ID: " + requestId + ", Vehicle ID: " + vehicleId + ", Customer ID: " + customerId +
                ", Service Type: " + serviceType + ", Date: " + date + ", Status: " + status;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceRequest)) return false;
        ServiceRequest r = (ServiceRequest) o;
        return requestId != null && requestId.equals(r.requestId);
    }
    @Override
    public int hashCode() { return requestId == null ? 0 : requestId.hashCode(); }
}