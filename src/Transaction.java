public class Transaction {
    private String transactionId;
    private String customerId;
    private int vehicleId;
    private double amount;
    private String transactionType;
    private String date;
    public Transaction(String transactionId, String customerId, int vehicleId, double amount, String transactionType, String date){
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.vehicleId = vehicleId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.date = date;
    }
    public String getTransactionId(){
        return transactionId;
    }
    // No setTransactionId: transactionId is immutable.
    public String getCustomerId(){
        return customerId;
    }
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    public int getVehicleId(){
        return vehicleId;
    }
    public void setVehicleId(int vehicleId){
        this.vehicleId = vehicleId;
    }
    public double getAmount(){
        return amount;
    }
    public void setAmount(double amount){
        this.amount = amount;
    }
    public String getTransactionType(){
        return transactionType;
    }
    public void setTransactionType(String transactionType){
        this.transactionType = transactionType;
    }
    public String getDate(){
        return date;
    }
    public void setDate(String date){
        this.date = date;
    }
    public Transaction(Transaction other) {
        this(other.transactionId, other.customerId, other.vehicleId, other.amount, other.transactionType, other.date);
    }

    public void copyFrom(Transaction other) {
        // transactionId intentionally not copied.
        this.customerId = other.customerId;
        this.vehicleId = other.vehicleId;
        this.amount = other.amount;
        this.transactionType = other.transactionType;
        this.date = other.date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction)) return false;
        Transaction t = (Transaction) o;
        return transactionId != null && transactionId.equals(t.transactionId);
    }
    @Override
    public int hashCode() { return transactionId == null ? 0 : transactionId.hashCode(); }

    public String toString(){
        return "Transaction ID: " + transactionId + ", Customer ID: " + customerId + ", Vehicle ID: " + vehicleId +
                ", Amount: " + amount + ", Type: " + transactionType + ", Date: " + date;
    }
}