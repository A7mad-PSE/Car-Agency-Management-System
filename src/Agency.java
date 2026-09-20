import java.time.LocalDate;
import java.time.Year;

public class Agency {

    public static final String AVAILABLE = "AVAILABLE";
    public static final String SOLD = "SOLD";
    public static final String IN_SERVICE = "IN_SERVICE";
    public static final String RESERVED = "RESERVED";

    // Core storage uses ONLY custom linked structures (no java.util collections).
    // SimpleLinkedList = singly linked list, Queue/Stack = linked nodes, AVL = linked nodes.
    private final AVL tree = new AVL();
    private final SimpleLinkedList<Customer> customers = new SimpleLinkedList<>();
    private final SimpleLinkedList<Transaction> transactions = new SimpleLinkedList<>();
    private final Queue<ServiceRequest> maintenanceQueue = new Queue<>();
    private final Queue<Customer> waitingQueue = new Queue<>();
    private final Queue<Reservation> reservationQueue = new Queue<>();
    private final Stack<Service> serviceHistory = new Stack<>();
    private final SimpleLinkedList<HistoryEntry> historyByVehicle = new SimpleLinkedList<>();
    private final Stack<Operation> undoStack = new Stack<>();
    private final Stack<Operation> redoStack = new Stack<>();
    private final SimpleLinkedList<String> requestIds = new SimpleLinkedList<>();
    private final SimpleLinkedList<String> serviceIds = new SimpleLinkedList<>();
    private ServiceRequest inProgressRequest;

    private int reqSeq = 1;
    private int resSeq = 1;
    private int txnSeq = 1;
    private int svcSeq = 1;

    private static class HistoryEntry {
        final int vehicleId;
        final Stack<Service> stack = new Stack<>();
        HistoryEntry(int vehicleId) { this.vehicleId = vehicleId; }
    }

    private HistoryEntry historyFor(int vehicleId, boolean create) {
        for (int i = 0; i < historyByVehicle.size(); i++) {
            HistoryEntry e = historyByVehicle.get(i);
            if (e.vehicleId == vehicleId) return e;
        }
        if (!create) return null;
        HistoryEntry e = new HistoryEntry(vehicleId);
        historyByVehicle.add(e);
        return e;
    }

    private void record(Operation op) {
        undoStack.push(op);
        redoStack.clear();
    }

    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        Operation op = undoStack.pop();
        apply(op, true);
        redoStack.push(op);
        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) return false;
        Operation op = redoStack.pop();
        apply(op, false);
        undoStack.push(op);
        return true;
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }
    public int undoCount() { return undoStack.getSize(); }
    public int redoCount() { return redoStack.getSize(); }

    public String peekUndo() {
        Operation op = undoStack.peek();
        return op == null ? "Nothing to undo" : op.getDescription();
    }

    public String peekRedo() {
        Operation op = redoStack.peek();
        return op == null ? "Nothing to redo" : op.getDescription();
    }

    public SimpleLinkedList<Operation> getUndoStack() { return fromArray(undoStack.toArray()); }
    public SimpleLinkedList<Operation> getRedoStack() { return fromArray(redoStack.toArray()); }

    @SuppressWarnings("unchecked")
    private static <T> SimpleLinkedList<T> fromArray(Object[] arr) {
        SimpleLinkedList<T> list = new SimpleLinkedList<>();
        for (Object o : arr) list.add((T) o);
        return list;
    }

    private void apply(Operation op, boolean isUndo) {
        switch (op.getType()) {
            case Operation.ADD_CUSTOMER: {
                Customer c = (Customer) op.getTarget();
                if (isUndo) customers.remove(c);
                else if (!customers.contains(c)) customers.add(new Customer(c));
                break;
            }
            case Operation.DELETE_CUSTOMER: {
                Customer c = (Customer) op.getTarget();
                if (isUndo) { if (!customers.contains(c)) customers.add(new Customer(c)); }
                else customers.remove(c);
                break;
            }
            case Operation.UPDATE_CUSTOMER: {
                Customer c = (Customer) op.getTarget();
                Customer live = getCustomer(c.getCustomerId());
                if (live != null) live.copyFrom(isUndo ? (Customer) op.getBefore() : (Customer) op.getAfter());
                else c.copyFrom(isUndo ? (Customer) op.getBefore() : (Customer) op.getAfter());
                break;
            }
            case Operation.ADD_VEHICLE: {
                Vehicle v = (Vehicle) op.getTarget();
                if (isUndo) tree.delete(v.getVehicleId());
                else if (!existsVehicle(v.getVehicleId())) tree.insert(new Vehicle(v));
                break;
            }
            case Operation.DELETE_VEHICLE: {
                Vehicle v = (Vehicle) op.getTarget();
                if (isUndo) { if (!existsVehicle(v.getVehicleId())) tree.insert(new Vehicle(v)); }
                else tree.delete(v.getVehicleId());
                break;
            }
            case Operation.UPDATE_VEHICLE:
            case Operation.CHANGE_STATUS:
            case Operation.APPLY_DISCOUNT: {
                Vehicle snapTarget = (Vehicle) op.getTarget();
                Vehicle live = getVehicle(snapTarget.getVehicleId());
                Vehicle snap = isUndo ? (Vehicle) op.getBefore() : (Vehicle) op.getAfter();
                if (live != null) live.copyFrom(snap);
                break;
            }
            case Operation.ADD_TRANSACTION: {
                Transaction t = (Transaction) op.getTarget();
                if (isUndo) transactions.remove(t);
                else if (!transactions.contains(t)) transactions.add(new Transaction(t));
                break;
            }
            case Operation.SALE:
            case Operation.DEPOSIT_HOLD:
            case Operation.REFUND_RELEASE: {
                // Atomic transaction + vehicle-status change.
                Transaction t = (Transaction) op.getTarget();
                Vehicle before = (Vehicle) op.getBefore();
                Vehicle after = (Vehicle) op.getAfter();
                Vehicle live = getVehicle(t.getVehicleId());
                if (isUndo) {
                    transactions.remove(t);
                    if (live != null && before != null) live.copyFrom(before);
                } else {
                    if (live == null) throw new IllegalStateException(
                            "Cannot redo: vehicle " + t.getVehicleId() + " no longer exists.");
                    if (after != null) live.copyFrom(after);
                    if (!transactions.contains(t)) transactions.add(new Transaction(t));
                }
                break;
            }
            case Operation.ADD_RESERVATION: {
                Reservation r = (Reservation) op.getTarget();
                int idx = op.getBefore() instanceof Integer ? (Integer) op.getBefore() : reservationQueue.getSize();
                Vehicle v = getVehicle(r.getVehicleId());
                if (isUndo) {
                    removeReservation(r.getReservationId());
                    if (v != null && RESERVED.equals(v.getStatus())
                            && getReservationsForVehicle(r.getVehicleId()).isEmpty()) {
                        v.setStatus(AVAILABLE);
                    }
                } else {
                    if (v == null) throw new IllegalStateException(
                            "Cannot redo: vehicle " + r.getVehicleId() + " no longer exists.");
                    if (SOLD.equals(v.getStatus())) throw new IllegalStateException(
                            "Cannot redo: vehicle " + r.getVehicleId() + " is SOLD.");
                    insertReservationAt(r, idx);
                    if (AVAILABLE.equals(v.getStatus())) v.setStatus(RESERVED);
                }
                break;
            }
        }
    }

    private void insertReservationAt(Reservation r, int idx) {
        Reservation copy = new Reservation(r.getReservationId(), r.getVehicleId(),
                r.getCustomerId(), r.getDate(), r.getStatus());
        if (idx < 0) idx = 0;
        if (idx >= reservationQueue.getSize()) reservationQueue.enqueue(copy);
        else reservationQueue.insertAt(idx, copy);
    }

    private void setStatusNoRecord(int vehicleId, String newStatus) {
        Vehicle v = getVehicle(vehicleId);
        if (v == null) throw new IllegalArgumentException("Vehicle " + vehicleId + " not found.");
        validateStatus(newStatus);
        v.setStatus(newStatus);
    }

    // ---------- auto IDs (no manual entry needed in UI) ----------
    public String nextRequestId() {
        String id;
        do { id = "R" + (reqSeq++); } while (requestIdUsed(id));
        return id;
    }

    public String nextReservationId() {
        String id;
        do { id = "RV" + (resSeq++); } while (reservationIdUsed(id));
        return id;
    }

    public String nextTransactionId() {
        String id;
        do { id = "T" + (txnSeq++); } while (transactionIdUsed(id));
        return id;
    }

    public String nextServiceId() {
        String id;
        do { id = "S" + (svcSeq++); } while (serviceIdUsed(id));
        return id;
    }

    private boolean requestIdUsed(String id) {
        if (requestIds.contains(id)) return true;
        if (inProgressRequest != null && inProgressRequest.getRequestId().equals(id)) return true;
        Object[] arr = maintenanceQueue.toArray();
        for (Object o : arr) if (((ServiceRequest) o).getRequestId().equals(id)) return true;
        return false;
    }

    private boolean serviceIdUsed(String id) {
        if (serviceIds.contains(id)) return true;
        Object[] arr = serviceHistory.toArray();
        for (Object o : arr) if (((Service) o).getServiceId().equals(id)) return true;
        return false;
    }

    private boolean transactionIdUsed(String id) {
        for (int i = 0; i < transactions.size(); i++) {
            if (transactions.get(i).getTransactionId().equals(id)) return true;
        }
        return false;
    }

    private boolean reservationIdUsed(String id) {
        Object[] arr = reservationQueue.toArray();
        for (Object o : arr) if (((Reservation) o).getReservationId().equals(id)) return true;
        return false;
    }

    // Customer IDs may be zero-padded in files ("09" vs "9"): compare numerically when both are numeric.
    private static boolean sameCustomerId(String a, String b) {
        if (a == null || b == null) return false;
        String x = a.trim(), y = b.trim();
        if (x.equals(y)) return true;
        try {
            return Long.parseLong(x) == Long.parseLong(y);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ---------- customers ----------
    public Customer getCustomer(String customerId) {
        for (int i = 0; i < customers.size(); i++) {
            Customer c = customers.get(i);
            if (sameCustomerId(c.getCustomerId(), customerId)) return c;
        }
        return null;
    }

    public boolean existsCustomer(String customerId) {
        return getCustomer(customerId) != null;
    }

    public SimpleLinkedList<Customer> getCustomersList() {
        SimpleLinkedList<Customer> out = new SimpleLinkedList<>();
        for (int i = 0; i < customers.size(); i++) out.add(customers.get(i));
        return out;
    }

    public void addCustomer(Customer customer) {
        if (customer == null) throw new IllegalArgumentException("Customer cannot be null.");
        requireText(customer.getCustomerId(), "Customer ID");
        requireLetters(customer.getName(), "Name");
        requireDigits(customer.getPhone(), "Phone");
        requireText(customer.getAddress(), "Address");
        if (existsCustomer(customer.getCustomerId()))
            throw new IllegalArgumentException("Customer ID " + customer.getCustomerId() + " already exists.");
        customers.add(new Customer(customer));
        record(new Operation(Operation.ADD_CUSTOMER, "Add Customer: " + customer.getCustomerId(),
                new Customer(customer), null, null));
    }

    public void updateCustomer(String customerId, String newName, String newPhone, String newAddress) {
        requireText(customerId, "Customer ID");
        requireLetters(newName, "Name");
        requireDigits(newPhone, "Phone");
        requireText(newAddress, "Address");
        Customer c = getCustomer(customerId);
        if (c == null) throw new IllegalArgumentException("Customer " + customerId + " not found.");
        Customer before = new Customer(c);
        c.setName(newName);
        c.setPhone(newPhone);
        c.setAddress(newAddress);
        Customer after = new Customer(c);
        record(new Operation(Operation.UPDATE_CUSTOMER, "Update Customer: " + customerId, c, before, after));
    }

    public void deleteCustomer(String customerId) {
        Customer c = getCustomer(customerId);
        if (c == null) throw new IllegalArgumentException("Customer " + customerId + " not found.");
        Customer snapshot = new Customer(c);
        customers.remove(c);
        record(new Operation(Operation.DELETE_CUSTOMER, "Delete Customer: " + customerId, snapshot, null, null));
    }

    public void displayCustomers() {
        if (customers.isEmpty()) { System.out.println("No customers on record."); return; }
        for (int i = 0; i < customers.size(); i++) System.out.println(customers.get(i));
    }

    // ---------- vehicles (status is workflow-driven, never typed by hand) ----------
    public Vehicle getVehicle(int vehicleId) {
        return tree.find(vehicleId);
    }

    public boolean existsVehicle(int vehicleId) {
        return getVehicle(vehicleId) != null;
    }

    public void addVehicle(Vehicle vehicle) {
        if (vehicle == null) throw new IllegalArgumentException("Vehicle cannot be null.");
        requirePositiveNumber(vehicle.getVehicleId(), "Vehicle ID");
        requireLetters(vehicle.getMake(), "Make");
        requireText(vehicle.getModel(), "Model");
        requireValidYear(vehicle.getYear());
        requireLetters(vehicle.getColor(), "Color");
        if (vehicle.getPrice() < 0) throw new IllegalArgumentException("Vehicle price cannot be negative.");
        validateStatus(vehicle.getStatus());
        // Status comes from caller: UI passes AVAILABLE, file loader passes file value.
        if (existsVehicle(vehicle.getVehicleId()))
            throw new IllegalArgumentException("Vehicle ID " + vehicle.getVehicleId() + " already exists.");
        tree.insert(new Vehicle(vehicle));
        record(new Operation(Operation.ADD_VEHICLE, "Add Vehicle: " + vehicle.getVehicleId(),
                new Vehicle(vehicle), null, null));
    }

    public void updateVehicle(int vehicleId, String make, String model, int year, double price, String color) {
        requirePositiveNumber(vehicleId, "Vehicle ID");
        requireLetters(make, "Make");
        requireText(model, "Model");
        requireValidYear(year);
        requireLetters(color, "Color");
        if (price < 0) throw new IllegalArgumentException("Vehicle price cannot be negative.");
        Vehicle v = getVehicle(vehicleId);
        if (v == null) throw new IllegalArgumentException("Vehicle " + vehicleId + " not found.");
        Vehicle before = new Vehicle(v);
        v.setMake(make);
        v.setModel(model);
        v.setYear(year);
        v.setPrice(price);
        v.setColor(color);
        Vehicle after = new Vehicle(v);
        record(new Operation(Operation.UPDATE_VEHICLE, "Update Vehicle: " + vehicleId, new Vehicle(v), before, after));
    }

    public void applyDiscount(int vehicleId, double percent) {
        requirePositiveNumber(vehicleId, "Vehicle ID");
        if (percent <= 0 || percent >= 100)
            throw new IllegalArgumentException("Discount percent must be > 0 and < 100.");
        Vehicle v = getVehicle(vehicleId);
        if (v == null) throw new IllegalArgumentException("Vehicle " + vehicleId + " not found.");
        Vehicle before = new Vehicle(v);
        v.setPrice(Math.round(v.getPrice() * (1 - percent / 100.0) * 100.0) / 100.0);
        Vehicle after = new Vehicle(v);
        record(new Operation(Operation.APPLY_DISCOUNT,
                "Apply " + percent + "% discount to vehicle " + vehicleId, new Vehicle(v), before, after));
    }

    public void setVehicleStatus(int vehicleId, String newStatus) {
        validateStatus(newStatus);
        Vehicle v = getVehicle(vehicleId);
        if (v == null) throw new IllegalArgumentException("Vehicle " + vehicleId + " not found.");
        if (v.getStatus().equals(newStatus)) return;
        Vehicle before = new Vehicle(v);
        v.setStatus(newStatus);
        Vehicle after = new Vehicle(v);
        record(new Operation(Operation.CHANGE_STATUS,
                "Change Vehicle " + vehicleId + " status: " + before.getStatus() + " -> " + newStatus,
                new Vehicle(v), before, after));
    }

    public void deleteVehicle(int vehicleId) {
        Vehicle v = getVehicle(vehicleId);
        if (v == null) throw new IllegalArgumentException("Vehicle " + vehicleId + " not found.");
        Vehicle snapshot = new Vehicle(v);
        tree.delete(vehicleId);
        record(new Operation(Operation.DELETE_VEHICLE, "Delete Vehicle: " + vehicleId, snapshot, null, null));
    }

    public int getTreeHeight() { return tree.getTreeHeight(); }

    public SimpleLinkedList<Vehicle> getAscendingVehicles() { return tree.inOrderVehicles(); }
    public SimpleLinkedList<Vehicle> getDescendingVehicles() { return tree.reverseInOrderVehicles(); }

    public void displayVehiclesAscending() {
        SimpleLinkedList<Vehicle> list = getAscendingVehicles();
        if (list.isEmpty()) { System.out.println("No vehicles in inventory."); return; }
        System.out.println("=== Vehicles (Ascending by ID) ===");
        for (int i = 0; i < list.size(); i++) System.out.println(list.get(i));
    }

    public void displayVehiclesDescending() {
        SimpleLinkedList<Vehicle> list = getDescendingVehicles();
        if (list.isEmpty()) { System.out.println("No vehicles in inventory."); return; }
        System.out.println("=== Vehicles (Descending by ID) ===");
        for (int i = 0; i < list.size(); i++) System.out.println(list.get(i));
    }

    public void displayTreeHeight() {
        System.out.println("AVL tree height: " + getTreeHeight());
    }

    // ---------- maintenance ----------
    public void createServiceRequest(String requestId, int vehicleId, String customerId, String serviceType, String date) {
        requireText(requestId, "Request ID");
        requirePositiveNumber(vehicleId, "Vehicle ID");
        requireText(customerId, "Customer ID");
        requireText(serviceType, "Service type");
        requireDate(date);
        if (requestIdUsed(requestId))
            throw new IllegalArgumentException("Service request ID " + requestId + " already exists.");
        if (!existsCustomer(customerId))
            throw new IllegalArgumentException("Customer " + customerId + " not found. Cannot create service request.");
        customerId = getCustomer(customerId).getCustomerId(); // canonical form ("09" -> "9")
        if (!existsVehicle(vehicleId))
            throw new IllegalArgumentException("Vehicle " + vehicleId + " not found. Cannot create service request.");
        Vehicle v = getVehicle(vehicleId);
        if (!AVAILABLE.equals(v.getStatus()))
            throw new IllegalArgumentException("Vehicle " + vehicleId + " is " + v.getStatus() + ", not AVAILABLE.");
        maintenanceQueue.enqueue(new ServiceRequest(requestId, vehicleId, customerId, serviceType, date, "PENDING"));
        requestIds.add(requestId);
    }

    public ServiceRequest createServiceRequestAuto(int vehicleId, String customerId, String serviceType, String date) {
        String id = nextRequestId();
        createServiceRequest(id, vehicleId, customerId, serviceType, date);
        Object[] arr = maintenanceQueue.toArray();
        for (int i = arr.length - 1; i >= 0; i--) {
            ServiceRequest r = (ServiceRequest) arr[i];
            if (r.getRequestId().equals(id)) return r;
        }
        return maintenanceQueue.peek();
    }

    public ServiceRequest peekNextRequest() { return maintenanceQueue.peek(); }

    public void enqueueServiceRequest(ServiceRequest request) {
        if (request == null) throw new IllegalArgumentException("Request cannot be null.");
        requireText(request.getRequestId(), "Request ID");
        requirePositiveNumber(request.getVehicleId(), "Vehicle ID");
        requireText(request.getCustomerId(), "Customer ID");
        requireText(request.getServiceType(), "Service type");
        requireDate(request.getDate());
        if (requestIdUsed(request.getRequestId()))
            throw new IllegalArgumentException("Service request ID " + request.getRequestId() + " already exists.");
        if (!existsCustomer(request.getCustomerId()))
            throw new IllegalArgumentException("Customer " + request.getCustomerId() + " not found.");
        if (!existsVehicle(request.getVehicleId()))
            throw new IllegalArgumentException("Vehicle " + request.getVehicleId() + " not found.");
        String canonCust = getCustomer(request.getCustomerId()).getCustomerId();
        maintenanceQueue.enqueue(new ServiceRequest(request.getRequestId(), request.getVehicleId(),
                canonCust, request.getServiceType(), request.getDate(),
                request.getStatus() == null || request.getStatus().trim().isEmpty() ? "PENDING" : request.getStatus()));
        requestIds.add(request.getRequestId());
        // keep auto counter ahead of manually loaded IDs like R5
        syncSeq(request.getRequestId());
    }

    public int getPendingCount() { return maintenanceQueue.getSize(); }
    public boolean isMaintenanceQueueEmpty() { return maintenanceQueue.isEmpty(); }
    public SimpleLinkedList<ServiceRequest> getPendingRequests() { return fromArray(maintenanceQueue.toArray()); }

    public ServiceRequest processNextRequest() {
        if (maintenanceQueue.isEmpty()) throw new IllegalStateException("Maintenance queue is empty.");
        if (inProgressRequest != null) throw new IllegalStateException(
                "A service is already in progress (request " + inProgressRequest.getRequestId() + "). Complete it first.");
        ServiceRequest req = maintenanceQueue.dequeue();
        if (!existsVehicle(req.getVehicleId())) {
            maintenanceQueue.enqueue(req);
            throw new IllegalStateException("Vehicle " + req.getVehicleId() + " no longer exists.");
        }
        Vehicle v = getVehicle(req.getVehicleId());
        if (!AVAILABLE.equals(v.getStatus()))
            throw new IllegalStateException("Vehicle " + req.getVehicleId() + " is " + v.getStatus() + ", not AVAILABLE.");
        setStatusNoRecord(req.getVehicleId(), IN_SERVICE);
        req.setStatus("IN_PROGRESS");
        inProgressRequest = req;
        return req;
    }

    public Service completeService(String serviceId, double cost, String date) {
        if (inProgressRequest == null) throw new IllegalStateException("No service is currently being processed.");
        requireText(serviceId, "Service ID");
        requireDate(date);
        if (cost < 0) throw new IllegalArgumentException("Service cost cannot be negative.");
        if (serviceIdUsed(serviceId))
            throw new IllegalArgumentException("Service ID " + serviceId + " already exists.");
        Service completed = new Service(serviceId, inProgressRequest.getVehicleId(),
                inProgressRequest.getCustomerId(), inProgressRequest.getServiceType(), date, cost, "COMPLETED");
        pushHistoryInternal(completed);
        setStatusNoRecord(inProgressRequest.getVehicleId(), AVAILABLE);
        inProgressRequest.setStatus("COMPLETED");
        inProgressRequest = null;
        return completed;
    }

    public Service completeServiceAuto(double cost, String date) {
        return completeService(nextServiceId(), cost, date);
    }

    private void pushHistoryInternal(Service service) {
        serviceHistory.push(service);
        serviceIds.add(service.getServiceId());
        historyFor(service.getVehicleId(), true).stack.push(service);
        syncSeq(service.getServiceId());
    }

    public ServiceRequest getCurrentRequest() { return inProgressRequest; }
    public boolean isProcessing() { return inProgressRequest != null; }

    public void displayPendingRequests() {
        Object[] arr = maintenanceQueue.toArray();
        if (arr.length == 0) { System.out.println("Maintenance queue is empty."); return; }
        System.out.println("=== Pending Maintenance Requests (FIFO) ===");
        for (Object o : arr) System.out.println(o);
    }

    public Service getMostRecentService() { return serviceHistory.peek(); }

    public void pushServiceHistory(Service service) {
        if (service == null) throw new IllegalArgumentException("Service cannot be null.");
        requireText(service.getServiceId(), "Service ID");
        requireDate(service.getDate());
        if (service.getCost() < 0) throw new IllegalArgumentException("Service cost cannot be negative.");
        if (serviceIdUsed(service.getServiceId()))
            throw new IllegalArgumentException("Service ID " + service.getServiceId() + " already exists.");
        if (!existsVehicle(service.getVehicleId()))
            throw new IllegalArgumentException("Vehicle " + service.getVehicleId() + " not found.");
        if (!existsCustomer(service.getCustomerId()))
            throw new IllegalArgumentException("Customer " + service.getCustomerId() + " not found.");
        pushHistoryInternal(new Service(service.getServiceId(), service.getVehicleId(),
                service.getCustomerId(), service.getServiceType(), service.getDate(),
                service.getCost(), service.getStatus() == null ? "COMPLETED" : service.getStatus()));
    }

    public int getHistorySize() { return serviceHistory.getSize(); }
    public boolean isHistoryEmpty() { return serviceHistory.isEmpty(); }
    public SimpleLinkedList<Service> getServiceHistory() { return fromArray(serviceHistory.toArray()); }

    public SimpleLinkedList<Service> getServiceHistoryForVehicle(int vehicleId) {
        HistoryEntry e = historyFor(vehicleId, false);
        if (e == null) return new SimpleLinkedList<>();
        return fromArray(e.stack.toArray());
    }

    public void displayServiceHistory() {
        if (serviceHistory.isEmpty()) { System.out.println("Service history is empty."); return; }
        System.out.println("=== Service History (most recent first) ===");
        Object[] arr = serviceHistory.toArray();
        for (Object o : arr) System.out.println(o);
    }

    // ---------- waiting queue ----------
    public void enqueueWaitingCustomer(Customer customer) {
        if (customer == null) throw new IllegalArgumentException("Customer cannot be null.");
        Customer full = getCustomer(customer.getCustomerId());
        if (full == null) throw new IllegalArgumentException("Customer " + customer.getCustomerId() + " not found.");
        Object[] arr = waitingQueue.toArray();
        for (Object o : arr) {
            if (sameCustomerId(((Customer) o).getCustomerId(), full.getCustomerId()))
                throw new IllegalArgumentException("Customer " + full.getCustomerId() + " is already waiting.");
        }
        waitingQueue.enqueue(new Customer(full));
    }

    public Customer serveNextWaitingCustomer() {
        if (waitingQueue.isEmpty()) throw new IllegalStateException("Waiting queue is empty. No customer to serve.");
        return waitingQueue.dequeue();
    }

    public Customer peekNextWaitingCustomer() { return waitingQueue.peek(); }
    public boolean isWaitingQueueEmpty() { return waitingQueue.isEmpty(); }
    public int getWaitingCount() { return waitingQueue.getSize(); }
    public SimpleLinkedList<Customer> getWaitingQueue() { return fromArray(waitingQueue.toArray()); }

    public void displayWaitingQueue() {
        Object[] arr = waitingQueue.toArray();
        if (arr.length == 0) { System.out.println("Customer waiting queue is empty."); return; }
        System.out.println("=== Customer Waiting Queue (front to rear) ===");
        for (int i = 0; i < arr.length; i++) System.out.println((i + 1) + ". " + arr[i]);
        System.out.println("Total waiting: " + arr.length);
    }

    // ---------- reservations ----------
    public void addReservation(String reservationId, int vehicleId, String customerId, String date) {
        requireText(reservationId, "Reservation ID");
        requirePositiveNumber(vehicleId, "Vehicle ID");
        requireText(customerId, "Customer ID");
        requireDate(date);
        if (!existsCustomer(customerId))
            throw new IllegalArgumentException("Customer " + customerId + " not found. Cannot reserve.");
        customerId = getCustomer(customerId).getCustomerId(); // canonical form
        Vehicle v = getVehicle(vehicleId);
        if (v == null) throw new IllegalArgumentException("Vehicle " + vehicleId + " not found.");
        if (SOLD.equals(v.getStatus()))
            throw new IllegalArgumentException("Vehicle " + vehicleId + " is SOLD and cannot be reserved.");
        if (reservationIdUsed(reservationId))
            throw new IllegalArgumentException("Reservation ID " + reservationId + " already exists.");
        int index = reservationQueue.getSize();
        Reservation r = new Reservation(reservationId, vehicleId, customerId, date, "WAITING");
        reservationQueue.enqueue(r);
        if (AVAILABLE.equals(v.getStatus())) v.setStatus(RESERVED);
        record(new Operation(Operation.ADD_RESERVATION,
                "Reserve vehicle " + vehicleId + " for customer " + customerId, new Reservation(
                        r.getReservationId(), r.getVehicleId(), r.getCustomerId(), r.getDate(), r.getStatus()),
                index, null));
        syncSeq(reservationId);
    }

    public Reservation addReservationAuto(int vehicleId, String customerId, String date) {
        String id = nextReservationId();
        addReservation(id, vehicleId, customerId, date);
        Object[] arr = reservationQueue.toArray();
        for (int i = arr.length - 1; i >= 0; i--) {
            Reservation r = (Reservation) arr[i];
            if (r.getReservationId().equals(id)) return r;
        }
        return null;
    }

    private void removeReservation(String reservationId) {
        int n = reservationQueue.getSize();
        boolean removed = false;
        for (int i = 0; i < n; i++) {
            Reservation r = reservationQueue.dequeue();
            if (!removed && r.getReservationId().equals(reservationId)) removed = true;
            else reservationQueue.enqueue(r);
        }
    }

    public Reservation processNextReservation(int vehicleId) {
        if (reservationQueue.isEmpty()) return null;
        if (!existsVehicle(vehicleId)) throw new IllegalArgumentException("Vehicle " + vehicleId + " not found.");
        Vehicle v = getVehicle(vehicleId);
        if (SOLD.equals(v.getStatus()))
            throw new IllegalStateException("Vehicle " + vehicleId + " is SOLD and cannot be assigned.");
        if (IN_SERVICE.equals(v.getStatus()))
            throw new IllegalStateException("Vehicle " + vehicleId + " is IN_SERVICE. Complete maintenance first.");
        int count = reservationQueue.getSize();
        Reservation found = null;
        for (int i = 0; i < count; i++) {
            Reservation r = reservationQueue.dequeue();
            if (found == null && r.getVehicleId() == vehicleId) {
                found = r;
            } else {
                reservationQueue.enqueue(r);
            }
        }
        if (found != null) {
            setStatusNoRecord(vehicleId, RESERVED);
            found.setStatus("ASSIGNED");
        }
        return found;
    }

    public SimpleLinkedList<Reservation> getReservationsForVehicle(int vehicleId) {
        SimpleLinkedList<Reservation> result = new SimpleLinkedList<>();
        Object[] arr = reservationQueue.toArray();
        for (Object o : arr) {
            Reservation r = (Reservation) o;
            if (r.getVehicleId() == vehicleId) result.add(r);
        }
        return result;
    }

    public SimpleLinkedList<Reservation> getAllReservations() { return fromArray(reservationQueue.toArray()); }
    public boolean isReservationQueueEmpty() { return reservationQueue.isEmpty(); }
    public int getReservationCount() { return reservationQueue.getSize(); }

    public void displayReservations() {
        Object[] arr = reservationQueue.toArray();
        if (arr.length == 0) { System.out.println("Reservation queue is empty."); return; }
        System.out.println("=== Reservation Queue (FIFO) ===");
        for (int i = 0; i < arr.length; i++) System.out.println((i + 1) + ". " + arr[i]);
    }

    public void displayReservationsForVehicle(int vehicleId) {
        SimpleLinkedList<Reservation> list = getReservationsForVehicle(vehicleId);
        if (list.isEmpty()) { System.out.println("No reservations for vehicle " + vehicleId + "."); return; }
        System.out.println("=== Reservations for Vehicle " + vehicleId + " (FIFO) ===");
        for (int i = 0; i < list.size(); i++) System.out.println((i + 1) + ". " + list.get(i));
    }

    // ---------- transactions (IDs auto-generated in UI; vehicle status follows automatically) ----------
    public Transaction getTransaction(String transactionId) {
        for (int i = 0; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);
            if (t.getTransactionId().equals(transactionId)) return t;
        }
        return null;
    }

    public SimpleLinkedList<Transaction> getTransactionsList() {
        SimpleLinkedList<Transaction> out = new SimpleLinkedList<>();
        for (int i = 0; i < transactions.size(); i++) out.add(transactions.get(i));
        return out;
    }

    public void appendTransaction(Transaction transaction) {
        if (transaction == null) throw new IllegalArgumentException("Transaction cannot be null.");
        validateTransactionFields(transaction.getTransactionId(), transaction.getCustomerId(),
                transaction.getVehicleId(), transaction.getTransactionType(),
                transaction.getAmount(), transaction.getDate());
        if (transactionIdUsed(transaction.getTransactionId()))
            throw new IllegalArgumentException("Transaction ID " + transaction.getTransactionId() + " already exists.");
        if (!existsCustomer(transaction.getCustomerId()))
            throw new IllegalArgumentException("Customer " + transaction.getCustomerId() + " not found.");
        if (!existsVehicle(transaction.getVehicleId()))
            throw new IllegalArgumentException("Vehicle " + transaction.getVehicleId() + " not found.");
        transactions.add(new Transaction(transaction));
        syncSeq(transaction.getTransactionId());
    }

    private void checkTransaction(String transactionId, String customerId, int vehicleId, String type, double amount, String date, boolean allowNegative) {
        requireText(transactionId, "Transaction ID");
        requireText(customerId, "Customer ID");
        requirePositiveNumber(vehicleId, "Vehicle ID");
        requireText(type, "Transaction type");
        requireDate(date);
        if (amount < 0 && !allowNegative)
            throw new IllegalArgumentException(type + " amount cannot be negative.");
    }

    private void validateTransactionFields(String transactionId, String customerId, int vehicleId, String type, double amount, String date) {
        requireText(transactionId, "Transaction ID");
        requireText(customerId, "Customer ID");
        requirePositiveNumber(vehicleId, "Vehicle ID");
        requireText(type, "Transaction type");
        if (!type.equals("SALE") && !type.equals("SERVICE_PAYMENT") && !type.equals("DEPOSIT") && !type.equals("REFUND"))
            throw new IllegalArgumentException("Invalid transaction type: " + type);
        requireDate(date);
        if (type.equals("REFUND")) return;
        if (amount < 0) throw new IllegalArgumentException(type + " amount cannot be negative.");
        if ((type.equals("SALE") || type.equals("SERVICE_PAYMENT") || type.equals("DEPOSIT")) && amount <= 0)
            throw new IllegalArgumentException(type + " amount must be positive.");
    }

    private void recordAtomicTx(Transaction t, Vehicle before, Vehicle after, int opType, String desc) {
        transactions.add(t);
        record(new Operation(opType, desc, new Transaction(t), before, after));
        syncSeq(t.getTransactionId());
    }

    public void createSale(String transactionId, String customerId, int vehicleId, double amount, String date) {
        validateTransactionFields(transactionId, customerId, vehicleId, "SALE", amount, date);
        if (transactionIdUsed(transactionId))
            throw new IllegalArgumentException("Transaction ID " + transactionId + " already exists.");
        if (!existsCustomer(customerId))
            throw new IllegalArgumentException("Customer " + customerId + " not found.");
        Vehicle v = getVehicle(vehicleId);
        if (v == null) throw new IllegalArgumentException("Vehicle " + vehicleId + " not found.");
        if (SOLD.equals(v.getStatus()))
            throw new IllegalArgumentException("Vehicle " + vehicleId + " is already SOLD and cannot be sold again.");
        if (IN_SERVICE.equals(v.getStatus()))
            throw new IllegalArgumentException("Vehicle " + vehicleId + " is under maintenance (IN_SERVICE) and cannot be sold now.");
        if (Math.abs(amount - v.getPrice()) > 0.01)
            throw new IllegalArgumentException("Sale amount " + amount + " does not match vehicle "
                    + vehicleId + " price " + v.getPrice() + ". Sale must equal the current vehicle price.");
        Vehicle before = new Vehicle(v);
        v.setStatus(SOLD);
        Vehicle after = new Vehicle(v);
        recordAtomicTx(new Transaction(transactionId, customerId, vehicleId, amount, "SALE", date),
                before, after, Operation.SALE, "Sell vehicle " + vehicleId + " (" + transactionId + ")");
    }

    public Transaction createSaleAuto(String customerId, int vehicleId, double amount, String date) {
        String id = nextTransactionId();
        createSale(id, customerId, vehicleId, amount, date);
        return getTransaction(id);
    }

    public void createServicePayment(String transactionId, String customerId, int vehicleId, double amount, String date) {
        validateTransactionFields(transactionId, customerId, vehicleId, "SERVICE_PAYMENT", amount, date);
        if (transactionIdUsed(transactionId))
            throw new IllegalArgumentException("Transaction ID " + transactionId + " already exists.");
        if (!existsCustomer(customerId))
            throw new IllegalArgumentException("Customer " + customerId + " not found.");
        if (!existsVehicle(vehicleId))
            throw new IllegalArgumentException("Vehicle " + vehicleId + " not found.");
        addTransaction(new Transaction(transactionId, customerId, vehicleId, amount, "SERVICE_PAYMENT", date));
    }

    public Transaction createServicePaymentAuto(String customerId, int vehicleId, double amount, String date) {
        String id = nextTransactionId();
        createServicePayment(id, customerId, vehicleId, amount, date);
        return getTransaction(id);
    }

    public void recordDeposit(String transactionId, String customerId, int vehicleId, double amount, String date) {
        validateTransactionFields(transactionId, customerId, vehicleId, "DEPOSIT", amount, date);
        if (transactionIdUsed(transactionId))
            throw new IllegalArgumentException("Transaction ID " + transactionId + " already exists.");
        if (!existsCustomer(customerId))
            throw new IllegalArgumentException("Customer " + customerId + " not found.");
        Vehicle v = getVehicle(vehicleId);
        if (v == null) throw new IllegalArgumentException("Vehicle " + vehicleId + " not found.");
        if (SOLD.equals(v.getStatus()))
            throw new IllegalArgumentException("Vehicle " + vehicleId + " is SOLD and cannot take a deposit.");
        if (IN_SERVICE.equals(v.getStatus()))
            throw new IllegalArgumentException("Vehicle " + vehicleId + " is IN_SERVICE and cannot take a deposit now.");
        if (amount > v.getPrice() + 0.01)
            throw new IllegalArgumentException("Deposit " + amount + " exceeds vehicle "
                    + vehicleId + " price " + v.getPrice() + ". Deposit must be within the vehicle price.");
        // Deposit holds the car: AVAILABLE -> RESERVED, atomically with the transaction row.
        Vehicle before = new Vehicle(v);
        if (AVAILABLE.equals(v.getStatus())) v.setStatus(RESERVED);
        Vehicle after = new Vehicle(v);
        recordAtomicTx(new Transaction(transactionId, customerId, vehicleId, amount, "DEPOSIT", date),
                before, after, Operation.DEPOSIT_HOLD, "Deposit on vehicle " + vehicleId + " (" + transactionId + ")");
    }

    public Transaction recordDepositAuto(String customerId, int vehicleId, double amount, String date) {
        String id = nextTransactionId();
        recordDeposit(id, customerId, vehicleId, amount, date);
        return getTransaction(id);
    }

    public void recordRefund(String transactionId, String customerId, int vehicleId, double amount, String date) {
        validateTransactionFields(transactionId, customerId, vehicleId, "REFUND", amount, date);
        if (transactionIdUsed(transactionId))
            throw new IllegalArgumentException("Transaction ID " + transactionId + " already exists.");
        if (!existsCustomer(customerId))
            throw new IllegalArgumentException("Customer " + customerId + " not found.");
        Vehicle v = getVehicle(vehicleId);
        if (v == null) throw new IllegalArgumentException("Vehicle " + vehicleId + " not found.");
        if (amount == 0)
            throw new IllegalArgumentException("Refund amount cannot be zero.");
        if (Math.abs(amount) > v.getPrice() + 0.01)
            throw new IllegalArgumentException("Refund " + amount + " exceeds vehicle "
                    + vehicleId + " price " + v.getPrice() + ".");
        // Refund releases a held car: RESERVED -> AVAILABLE, atomically.
        Vehicle before = new Vehicle(v);
        if (RESERVED.equals(v.getStatus()) && getReservationsForVehicle(vehicleId).isEmpty()) {
            v.setStatus(AVAILABLE);
        }
        Vehicle after = new Vehicle(v);
        recordAtomicTx(new Transaction(transactionId, customerId, vehicleId, amount, "REFUND", date),
                before, after, Operation.REFUND_RELEASE, "Refund on vehicle " + vehicleId + " (" + transactionId + ")");
    }

    public Transaction recordRefundAuto(String customerId, int vehicleId, double amount, String date) {
        String id = nextTransactionId();
        recordRefund(id, customerId, vehicleId, amount, date);
        return getTransaction(id);
    }

    private void addTransaction(Transaction t) {
        if (transactionIdUsed(t.getTransactionId()))
            throw new IllegalArgumentException("Transaction ID " + t.getTransactionId() + " already exists.");
        transactions.add(new Transaction(t));
        record(new Operation(Operation.ADD_TRANSACTION,
                "Create " + t.getTransactionType() + " transaction: " + t.getTransactionId(),
                new Transaction(t), null, null));
        syncSeq(t.getTransactionId());
    }

    public void displayTransactions() {
        if (transactions.isEmpty()) { System.out.println("No transactions recorded."); return; }
        System.out.println("=== Transactions ===");
        for (int i = 0; i < transactions.size(); i++) System.out.println(transactions.get(i));
    }

    private void validateStatus(String status) {
        if (!AVAILABLE.equals(status) && !SOLD.equals(status)
                && !IN_SERVICE.equals(status) && !RESERVED.equals(status)) {
            throw new IllegalArgumentException("Invalid vehicle status: " + status
                    + ". Must be AVAILABLE, SOLD, IN_SERVICE, or RESERVED.");
        }
    }

    // Keep auto counters ahead of file-loaded IDs such as R5 / T8 / S4 / RV3.
    private void syncSeq(String id) {
        if (id == null || id.length() < 2) return;
        int i = 0;
        while (i < id.length() && !Character.isDigit(id.charAt(i))) i++;
        if (i == 0 || i >= id.length()) return;
        try {
            int n = Integer.parseInt(id.substring(i));
            if (id.startsWith("R") && !id.startsWith("RV")) reqSeq = Math.max(reqSeq, n + 1);
            else if (id.startsWith("RV")) resSeq = Math.max(resSeq, n + 1);
            else if (id.startsWith("T")) txnSeq = Math.max(txnSeq, n + 1);
            else if (id.startsWith("S")) svcSeq = Math.max(svcSeq, n + 1);
        } catch (NumberFormatException ignored) { }
    }

    // ---- validation helpers ----

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty())
            throw new IllegalArgumentException(field + " cannot be empty.");
    }

    private void requireLetters(String value, String field) {
        if (value == null || !value.trim().matches("[\\p{L}][\\p{L}\\-\\' ]*"))
            throw new IllegalArgumentException(field + " must contain letters only.");
    }

    private void requireDigits(String value, String field) {
        if (value == null || !value.trim().matches("\\d{7,15}"))
            throw new IllegalArgumentException(field + " must contain digits only (7-15).");
    }

    private void requirePositiveNumber(int value, String field) {
        if (value <= 0) throw new IllegalArgumentException(field + " must be a positive number.");
    }

    private void requireValidYear(int year) {
        int maxYear = Year.now().getValue() + 1;
        if (year < 1886 || year > maxYear)
            throw new IllegalArgumentException("Year must be between 1886 and " + maxYear + ".");
    }

    private void requireDate(String date) {
        if (date == null || !date.matches("\\d{4}-\\d{2}-\\d{2}"))
            throw new IllegalArgumentException("Date must be in format YYYY-MM-DD.");
        try {
            LocalDate.parse(date);
        } catch (Exception e) {
            throw new IllegalArgumentException("Date must be a real calendar date YYYY-MM-DD.");
        }
    }


    public static class Operation {
        public static final int ADD_CUSTOMER = 1;
        public static final int UPDATE_CUSTOMER = 2;
        public static final int DELETE_CUSTOMER = 3;
        public static final int ADD_VEHICLE = 4;
        public static final int UPDATE_VEHICLE = 5;
        public static final int DELETE_VEHICLE = 6;
        public static final int CHANGE_STATUS = 7;
        public static final int ADD_TRANSACTION = 8;
        public static final int ADD_RESERVATION = 9;
        public static final int SALE = 10;
        public static final int APPLY_DISCOUNT = 11;
        public static final int DEPOSIT_HOLD = 12;
        public static final int REFUND_RELEASE = 13;

        private final int type;
        private final String description;
        private final Object target;
        private final Object before;
        private final Object after;

        public Operation(int type, String description, Object target, Object before, Object after) {
            this.type = type;
            this.description = description;
            this.target = target;
            this.before = before;
            this.after = after;
        }

        public int getType() { return type; }
        public String getDescription() { return description; }
        public Object getTarget() { return target; }
        public Object getBefore() { return before; }
        public Object getAfter() { return after; }
        public String toString() { return description; }
    }


    public static class Reservation {
        private final String reservationId;
        private final int vehicleId;
        private final String customerId;
        private final String date;
        private String status;

        public Reservation(String reservationId, int vehicleId, String customerId, String date, String status) {
            this.reservationId = reservationId;
            this.vehicleId = vehicleId;
            this.customerId = customerId;
            this.date = date;
            this.status = status;
        }

        public String getReservationId() { return reservationId; }
        public int getVehicleId() { return vehicleId; }
        public String getCustomerId() { return customerId; }
        public String getDate() { return date; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String toString() {
            return "Reservation ID: " + reservationId + ", Vehicle ID: " + vehicleId +
                    ", Customer ID: " + customerId + ", Date: " + date + ", Status: " + status;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Reservation)) return false;
            Reservation r = (Reservation) o;
            return reservationId != null && reservationId.equals(r.reservationId);
        }
        @Override
        public int hashCode() { return reservationId == null ? 0 : reservationId.hashCode(); }
    }
}
