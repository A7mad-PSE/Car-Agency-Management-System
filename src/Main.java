import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Main extends Application {

    private final Agency agency = new Agency();
    private final Label status = new Label("Ready.");
    private Stage stage;
    private final TableView<Customer> customerTable = new TableView<>();
    private final TableView<Vehicle> vehicleTable = new TableView<>();
    private final TableView<ServiceRequest> srTable = new TableView<>();
    private final TableView<Service> historyTable = new TableView<>();
    private final TableView<Transaction> transactionTable = new TableView<>();
    private final TableView<Agency.Reservation> reservationTable = new TableView<>();

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(
                customerTab(), vehicleTab(), maintenanceTab(), waitingTab(),
                reservationTab(), transactionTab(), undoRedoTab());

        BorderPane root = new BorderPane();
        root.setTop(loadToolbar());
        root.setCenter(tabs);
        root.setBottom(status);

        stage.setTitle("Car Agency Management System");
        stage.setScene(new Scene(root, 950, 620));
        stage.show();
    }

    // shared helpers

    private TextField field(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setPrefWidth(110);
        return f;
    }

    private DatePicker dateField() {
        DatePicker dp = new DatePicker();
        dp.setPromptText("yyyy-MM-dd");
        dp.setPrefWidth(110);
        return dp;
    }

    private String dateText(DatePicker dp) {
        return dp.getValue() == null ? "" : dp.getValue().toString();
    }

    private Button btn(String text, Runnable action) {
        Button b = new Button(text);
        b.setOnAction(e -> action.run());
        return b;
    }

    private <T> TableColumn<T, String> col(String title, int width, Function<T, String> get) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setCellValueFactory(d -> new SimpleStringProperty(get.apply(d.getValue())));
        return c;
    }

    private <T> void refreshTable(TableView<T> table, List<T> items) {
        table.getItems().setAll(items);
    }

    // UI-boundary adapter: core Agency exposes custom SimpleLinkedList,
    // JavaFX TableView needs java.util.List. Conversion happens only here.
    private <T> List<T> javaList(SimpleLinkedList<T> simple) {
        List<T> out = new ArrayList<>(simple.size());
        for (int i = 0; i < simple.size(); i++) out.add(simple.get(i));
        return out;
    }

    private void clear(TextField... fields) {
        for (TextField f : fields) f.clear();
    }

    private void clear(DatePicker... fields) {
        for (DatePicker f : fields) f.setValue(null);
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg == null ? "Error." : msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void ok(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            alert(e.getMessage());
        }
    }

    private int parseId(String label, String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a number.");
        }
    }

    private double parseAmount(String label, String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a number.");
        }
    }

    // file loading toolbar

    private HBox loadToolbar() {
        HBox bar = new HBox(8, new Label("Load file:"));
        bar.setPadding(new Insets(8));
        bar.getChildren().add(btn("Customers", () -> loadFile("customers", FileLoader::loadCustomers)));
        bar.getChildren().add(btn("Vehicles", () -> loadFile("vehicles", FileLoader::loadVehicles)));
        bar.getChildren().add(btn("Service Requests", () -> loadFile("service requests", FileLoader::loadServiceRequests)));
        bar.getChildren().add(btn("Services", () -> loadFile("services", FileLoader::loadServices)));
        bar.getChildren().add(btn("Transactions", () -> loadFile("transactions", FileLoader::loadTransactions)));
        return bar;
    }

    private void loadFile(String kind, BiFunction<Agency, File, Integer> loader) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose the " + kind + " file");
        chooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;
        try {
            int count = loader.apply(agency, file);
            status.setText("Loaded " + count + " " + kind + (count == 0 ? " (none added - maybe already loaded)" : "") + ".");
            switch (kind) {
                case "customers": refreshTable(customerTable, javaList(agency.getCustomersList())); break;
                case "vehicles": refreshTable(vehicleTable, javaList(agency.getAscendingVehicles()));
                    refreshAllVehicleViews(); break;
                case "service requests": refreshTable(srTable, javaList(agency.getPendingRequests())); break;
                case "services": refreshTable(historyTable, javaList(agency.getServiceHistory())); break;
                case "transactions": refreshTable(transactionTable, javaList(agency.getTransactionsList()));
                    refreshAllVehicleViews(); break;
                default: break;
            }
        } catch (RuntimeException e) {
            alert(e.getMessage());
        }
    }

    private void refreshAllVehicleViews() {
        refreshTable(vehicleTable, javaList(agency.getAscendingVehicles()));
    }

    // Customers tab

    private Tab customerTab() {
        TextField id = field("Customer ID"), name = field("Name");
        TextField phone = field("Phone"), addr = field("Address");

        TableView<Customer> table = customerTable;
        table.getColumns().addAll(
                col("ID", 90, Customer::getCustomerId),
                col("Name", 150, Customer::getName),
                col("Phone", 130, Customer::getPhone),
                col("Address", 220, Customer::getAddress));

        VBox box = new VBox(10,
                new HBox(6, new Label("ID:"), id, new Label("Name:"), name, new Label("Phone:"), phone, new Label("Address:"), addr),
                new HBox(6,
                        btn("Add", () -> ok(() -> {
                            agency.addCustomer(new Customer(id.getText(), name.getText(), phone.getText(), addr.getText()));
                            status.setText("Customer " + id.getText() + " added.");
                            clear(id, name, phone, addr);
                            refreshTable(table, javaList(agency.getCustomersList()));
                        })),
                        btn("Search", () -> ok(() -> {
                            Customer c = agency.getCustomer(id.getText());
                            if (c == null) throw new IllegalArgumentException("Customer not found.");
                            name.setText(c.getName());
                            phone.setText(c.getPhone());
                            addr.setText(c.getAddress());
                            status.setText("Found customer " + c.getName() + ".");
                        })),
                        btn("Update", () -> ok(() -> {
                            Customer c = agency.getCustomer(id.getText());
                            if (c == null) throw new IllegalArgumentException("Customer not found.");
                            boolean nm = !name.getText().trim().isEmpty();
                            boolean ph = !phone.getText().trim().isEmpty();
                            boolean ad = !addr.getText().trim().isEmpty();
                            if (nm || ph || ad) {
                                agency.updateCustomer(id.getText(),
                                        nm ? name.getText().trim() : c.getName(),
                                        ph ? phone.getText().trim() : c.getPhone(),
                                        ad ? addr.getText().trim() : c.getAddress());
                                status.setText("Customer " + id.getText() + " updated.");
                            } else {
                                status.setText("No changes to apply for customer " + id.getText() + ".");
                            }
                            clear(name, phone, addr);
                            refreshTable(table, javaList(agency.getCustomersList()));
                        })),
                        btn("Delete", () -> ok(() -> {
                            agency.deleteCustomer(id.getText());
                            status.setText("Customer " + id.getText() + " deleted.");
                            clear(id, name, phone, addr);
                            refreshTable(table, javaList(agency.getCustomersList()));
                        })),
                        btn("Refresh", () -> ok(() -> refreshTable(table, javaList(agency.getCustomersList()))))),
                table);
        box.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);
        return new Tab("Customers", box);
    }

    //Vehicles tab: status is display-only, driven by sale/service/reservation workflow.

    private Tab vehicleTab() {
        TextField id = field("Vehicle ID"), make = field("Make"), model = field("Model");
        TextField year = field("Year"), price = field("Price"), color = field("Color");
        TextField discount = field("Discount %");

        TableView<Vehicle> table = vehicleTable;
        table.getColumns().addAll(
                col("ID", 70, v -> String.valueOf(v.getVehicleId())),
                col("Make", 90, Vehicle::getMake),
                col("Model", 100, Vehicle::getModel),
                col("Year", 70, v -> String.valueOf(v.getYear())),
                col("Price", 90, v -> String.valueOf(v.getPrice())),
                col("Color", 90, Vehicle::getColor),
                col("Status", 90, Vehicle::getStatus));

        VBox box = new VBox(10,
                new Label("Status is automatic (AVAILABLE -> IN_SERVICE/RESERVED/SOLD). New cars enter as AVAILABLE."),
                new HBox(6, new Label("ID:"), id, new Label("Make:"), make, new Label("Model:"), model,
                        new Label("Year:"), year, new Label("Price:"), price, new Label("Color:"), color),
                new HBox(6,
                        btn("Add (AVAILABLE)", () -> ok(() -> {
                            agency.addVehicle(new Vehicle(parseId("Vehicle ID", id.getText()), make.getText(),
                                    model.getText(), parseId("Year", year.getText()),
                                    parseAmount("Price", price.getText()), color.getText(), Agency.AVAILABLE));
                            status.setText("Vehicle " + id.getText() + " added as AVAILABLE.");
                            clear(id, make, model, year, price, color);
                            refreshTable(table, javaList(agency.getAscendingVehicles()));
                        })),
                        btn("Search", () -> ok(() -> {
                            Vehicle v = agency.getVehicle(parseId("Vehicle ID", id.getText()));
                            if (v == null) throw new IllegalArgumentException("Vehicle not found.");
                            make.setText(v.getMake());
                            model.setText(v.getModel());
                            year.setText(String.valueOf(v.getYear()));
                            price.setText(String.valueOf(v.getPrice()));
                            color.setText(v.getColor());
                            status.setText("Found vehicle " + v.getVehicleId() + " [" + v.getStatus() + "].");
                        })),
                        btn("Update", () -> ok(() -> {
                            int vid = parseId("Vehicle ID", id.getText());
                            Vehicle v = agency.getVehicle(vid);
                            if (v == null) throw new IllegalArgumentException("Vehicle not found.");
                            boolean made = !make.getText().trim().isEmpty();
                            boolean mod = !model.getText().trim().isEmpty();
                            boolean yr = !year.getText().trim().isEmpty();
                            boolean pr = !price.getText().trim().isEmpty();
                            boolean col = !color.getText().trim().isEmpty();
                            if (made || mod || yr || pr || col) {
                                agency.updateVehicle(vid,
                                        made ? make.getText().trim() : v.getMake(),
                                        mod ? model.getText().trim() : v.getModel(),
                                        yr ? parseId("Year", year.getText()) : v.getYear(),
                                        pr ? parseAmount("Price", price.getText()) : v.getPrice(),
                                        col ? color.getText().trim() : v.getColor());
                                status.setText("Vehicle " + vid + " updated.");
                            } else {
                                status.setText("No changes to apply for vehicle " + vid + ".");
                            }
                            clear(make, model, year, price, color);
                            refreshTable(table, javaList(agency.getAscendingVehicles()));
                        })),
                        btn("Delete", () -> ok(() -> {
                            agency.deleteVehicle(parseId("Vehicle ID", id.getText()));
                            status.setText("Vehicle " + id.getText() + " deleted.");
                            clear(id, make, model, year, price, color);
                            refreshTable(table, javaList(agency.getAscendingVehicles()));
                        })),
                        btn("Height", () -> ok(() -> status.setText("AVL tree height: " + agency.getTreeHeight())))),
                new HBox(6,
                        btn("Ascending", () -> ok(() -> refreshTable(table, javaList(agency.getAscendingVehicles())))),
                        btn("Descending", () -> ok(() -> refreshTable(table, javaList(agency.getDescendingVehicles())))),
                        new Label("Discount %:"), discount,
                        btn("Apply Discount", () -> ok(() -> {
                            agency.applyDiscount(parseId("Vehicle ID", id.getText()),
                                    parseAmount("Discount", discount.getText()));
                            status.setText("Discount applied to vehicle " + id.getText() + ".");
                            clear(discount);
                            refreshTable(table, javaList(agency.getAscendingVehicles()));
                        }))),
                table);
        box.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);
        return new Tab("Vehicles", box);
    }

    // Maintenance tab: IDs auto-generated.

    private Tab maintenanceTab() {
        TextField vId = field("Vehicle ID"), cId = field("Customer ID");
        TextField type = field("Service Type");
        DatePicker createDate = dateField();
        DatePicker completeDate = dateField();
        TextField cost = field("Cost");
        Label current = new Label("");

        TableView<ServiceRequest> table = srTable;
        table.getColumns().addAll(
                col("Request", 80, ServiceRequest::getRequestId),
                col("Vehicle", 70, r -> String.valueOf(r.getVehicleId())),
                col("Customer", 90, ServiceRequest::getCustomerId),
                col("Type", 130, ServiceRequest::getServiceType),
                col("Date", 100, ServiceRequest::getDate),
                col("Status", 90, ServiceRequest::getStatus));

        historyTable.getColumns().addAll(
                col("Service", 80, Service::getServiceId),
                col("Vehicle", 70, s -> String.valueOf(s.getVehicleId())),
                col("Customer", 90, Service::getCustomerId),
                col("Type", 130, Service::getServiceType),
                col("Date", 100, Service::getDate),
                col("Cost", 90, s -> String.valueOf(s.getCost())),
                col("Status", 90, Service::getStatus));

        VBox box = new VBox(10,
                new HBox(6, new Label("Vehicle:"), vId, new Label("Customer:"), cId,
                        new Label("Type:"), type, new Label("Date:"), createDate),
                new HBox(6,
                        btn("Create Request (auto ID)", () -> ok(() -> {
                            ServiceRequest r = agency.createServiceRequestAuto(parseId("Vehicle ID", vId.getText()),
                                    cId.getText(), type.getText(), dateText(createDate));
                            status.setText("Request " + r.getRequestId() + " queued.");
                            clear(vId, cId, type);
                            clear(createDate);
                            refreshTable(table, javaList(agency.getPendingRequests()));
                        })),
                        btn("Process Next", () -> ok(() -> {
                            ServiceRequest r = agency.processNextRequest();
                            current.setText("In progress: " + r.getRequestId() + " (" + r.getServiceType()
                                    + ") on vehicle " + r.getVehicleId() + " -> IN_SERVICE");
                            status.setText("Processing " + r.getRequestId() + ".");
                            refreshTable(table, javaList(agency.getPendingRequests()));
                            refreshTable(vehicleTable, javaList(agency.getAscendingVehicles()));
                        })),
                        btn("Refresh", () -> ok(() -> refreshTable(table, javaList(agency.getPendingRequests())))),
                        btn("Refresh History", () -> ok(() -> refreshTable(historyTable, javaList(agency.getServiceHistory()))))),
                new HBox(6, new Label("Cost:"), cost, new Label("Date:"), completeDate,
                        btn("Complete Service (auto ID)", () -> ok(() -> {
                            Service s = agency.completeServiceAuto(
                                    parseAmount("Cost", cost.getText()), dateText(completeDate));
                            current.setText("");
                            status.setText("Service " + s.getServiceId() + " completed, vehicle back to AVAILABLE.");
                            clear(cost);
                            clear(completeDate);
                            refreshTable(table, javaList(agency.getPendingRequests()));
                            refreshTable(historyTable, javaList(agency.getServiceHistory()));
                            refreshTable(vehicleTable, javaList(agency.getAscendingVehicles()));
                        }))),
                current,
                new Label("Pending maintenance queue (FIFO):"),
                table,
                new Label("Service history (most recent first):"),
                historyTable);
        box.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(historyTable, Priority.ALWAYS);
        return new Tab("Maintenance", box);
    }

    // Customer Waiting Queue tab

    private Tab waitingTab() {
        TextField cId = field("Customer ID");

        TableView<Customer> table = new TableView<>();
        table.getColumns().addAll(
                col("ID", 90, Customer::getCustomerId),
                col("Name", 150, Customer::getName),
                col("Phone", 130, Customer::getPhone),
                col("Address", 220, Customer::getAddress));

        VBox box = new VBox(10,
                new HBox(6, new Label("Customer ID:"), cId,
                        btn("Enqueue", () -> ok(() -> {
                            Customer c = agency.getCustomer(cId.getText());
                            if (c == null) throw new IllegalArgumentException("Customer not found.");
                            agency.enqueueWaitingCustomer(c);
                            status.setText("Customer " + c.getName() + " added to waiting queue.");
                            clear(cId);
                            refreshTable(table, javaList(agency.getWaitingQueue()));
                        })),
                        btn("Serve Next", () -> ok(() -> {
                            Customer c = agency.serveNextWaitingCustomer();
                            status.setText("Served customer " + c.getName() + " (FIFO).");
                            refreshTable(table, javaList(agency.getWaitingQueue()));
                        })),
                        btn("Count", () -> ok(() -> status.setText("Waiting customers: " + agency.getWaitingCount()))),
                        btn("Refresh", () -> ok(() -> refreshTable(table, javaList(agency.getWaitingQueue()))))),
                table);
        box.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);
        return new Tab("Waiting Queue", box);
    }

    // Reservations tab: IDs auto-generated.

    private Tab reservationTab() {
        TextField vId = field("Vehicle ID"), cId = field("Customer ID");
        DatePicker date = dateField();
        TextField processV = field("Vehicle ID");

        TableView<Agency.Reservation> table = reservationTable;
        table.getColumns().addAll(
                col("Reservation", 90, Agency.Reservation::getReservationId),
                col("Vehicle", 80, r -> String.valueOf(r.getVehicleId())),
                col("Customer", 90, Agency.Reservation::getCustomerId),
                col("Date", 110, Agency.Reservation::getDate),
                col("Status", 90, Agency.Reservation::getStatus));

        VBox box = new VBox(10,
                new HBox(6, new Label("Vehicle ID:"), vId,
                        new Label("Customer ID:"), cId, new Label("Date:"), date),
                new HBox(6,
                        btn("Add Reservation (auto ID)", () -> ok(() -> {
                            Agency.Reservation r = agency.addReservationAuto(parseId("Vehicle ID", vId.getText()),
                                    cId.getText(), dateText(date));
                            status.setText("Reservation " + r.getReservationId() + " created. Vehicle " + vId.getText() + " held.");
                            clear(vId, cId);
                            clear(date);
                            refreshTable(table, javaList(agency.getAllReservations()));
                            refreshTable(vehicleTable, javaList(agency.getAscendingVehicles()));
                        })),
                        btn("Refresh", () -> ok(() -> refreshTable(table, javaList(agency.getAllReservations()))))),
                new HBox(6, new Label("Vehicle ready:"), processV,
                        btn("Process Next", () -> ok(() -> {
                            Agency.Reservation r = agency.processNextReservation(parseId("Vehicle ID", processV.getText()));
                            if (r == null) {
                                status.setText("No reservation waiting for that vehicle.");
                            } else {
                                status.setText("Assigned " + r.getCustomerId() + " to vehicle " + r.getVehicleId() + " (RESERVED).");
                                refreshTable(table, javaList(agency.getAllReservations()));
                                refreshTable(vehicleTable, javaList(agency.getAscendingVehicles()));
                            }
                        }))),
                table);
        box.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);
        return new Tab("Reservations", box);
    }

    // Transactions tab: IDs auto-generated, vehicle table follows automatically.

    private Tab transactionTab() {
        TextField cId = field("Customer ID"), vId = field("Vehicle ID"), amount = field("Amount");
        DatePicker date = dateField();
        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("SALE", "SERVICE_PAYMENT", "DEPOSIT", "REFUND");
        typeBox.setValue("SALE");

        TableView<Transaction> table = transactionTable;
        table.getColumns().addAll(
                col("ID", 90, Transaction::getTransactionId),
                col("Customer", 90, Transaction::getCustomerId),
                col("Vehicle", 80, t -> String.valueOf(t.getVehicleId())),
                col("Amount", 90, t -> String.valueOf(t.getAmount())),
                col("Type", 130, Transaction::getTransactionType),
                col("Date", 100, Transaction::getDate));

        VBox box = new VBox(10,
                new Label("ID is automatic. SALE -> SOLD, DEPOSIT -> RESERVED, REFUND -> AVAILABLE (if held)."),
                new HBox(6, new Label("Customer:"), cId, new Label("Vehicle:"), vId,
                        new Label("Amount:"), amount, new Label("Date:"), date),
                new HBox(6, typeBox,
                        btn("Create (auto ID)", () -> ok(() -> {
                            int vid = parseId("Vehicle ID", vId.getText());
                            double amt = parseAmount("Amount", amount.getText());
                            Transaction t;
                            switch (typeBox.getValue()) {
                                case "SALE": t = agency.createSaleAuto(cId.getText(), vid, amt, dateText(date)); break;
                                case "SERVICE_PAYMENT": t = agency.createServicePaymentAuto(cId.getText(), vid, amt, dateText(date)); break;
                                case "DEPOSIT": t = agency.recordDepositAuto(cId.getText(), vid, amt, dateText(date)); break;
                                default: t = agency.recordRefundAuto(cId.getText(), vid, amt, dateText(date)); break;
                            }
                            status.setText(typeBox.getValue() + " " + t.getTransactionId() + " recorded. Vehicle " + vid + " is now " + agency.getVehicle(vid).getStatus() + ".");
                            clear(cId, vId, amount);
                            clear(date);
                            refreshTable(table, javaList(agency.getTransactionsList()));
                            refreshTable(vehicleTable, javaList(agency.getAscendingVehicles()));
                            refreshTable(reservationTable, javaList(agency.getAllReservations()));
                        })),
                        btn("Refresh", () -> ok(() -> refreshTable(table, javaList(agency.getTransactionsList()))))),
                table);
        box.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);
        return new Tab("Transactions", box);
    }

    // Undo / Redo tab

    private Tab undoRedoTab() {
        ListView<Agency.Operation> undoList = new ListView<>();
        ListView<Agency.Operation> redoList = new ListView<>();
        Label peek = new Label();

        VBox undoBox = new VBox(6, new Label("Undo Stack (most recent on top)"), undoList);
        VBox redoBox = new VBox(6, new Label("Redo Stack (most recent on top)"), redoList);

        HBox lists = new HBox(10, undoBox, redoBox);
        HBox.setHgrow(undoBox, Priority.ALWAYS);
        HBox.setHgrow(redoBox, Priority.ALWAYS);
        VBox.setVgrow(undoList, Priority.ALWAYS);
        VBox.setVgrow(redoList, Priority.ALWAYS);

        VBox box = new VBox(10,
                new HBox(6,
                        btn("Undo", () -> ok(() -> {
                            if (agency.undo()) status.setText("Undone last operation.");
                            else status.setText("Nothing to undo.");
                            refreshUndo(undoList, redoList, peek);
                        })),
                        btn("Redo", () -> ok(() -> {
                            if (agency.redo()) status.setText("Redone last undone operation.");
                            else status.setText("Nothing to redo.");
                            refreshUndo(undoList, redoList, peek);
                        })),
                        btn("Refresh", () -> ok(() -> refreshUndo(undoList, redoList, peek)))),
                peek,
                lists);
        box.setPadding(new Insets(10));
        VBox.setVgrow(lists, Priority.ALWAYS);
        return new Tab("Undo / Redo", box);
    }

    private void refreshUndo(ListView<Agency.Operation> undo, ListView<Agency.Operation> redo, Label peek) {
        undo.getItems().setAll(new ArrayList<>(javaList(agency.getUndoStack())));
        redo.getItems().setAll(new ArrayList<>(javaList(agency.getRedoStack())));
        peek.setText("Next undo: " + agency.peekUndo() + "   |   Next redo: " + agency.peekRedo());
        refreshTable(customerTable, javaList(agency.getCustomersList()));
        refreshTable(vehicleTable, javaList(agency.getAscendingVehicles()));
        refreshTable(reservationTable, javaList(agency.getAllReservations()));
        refreshTable(transactionTable, javaList(agency.getTransactionsList()));
        refreshTable(srTable, javaList(agency.getPendingRequests()));
        refreshTable(historyTable, javaList(agency.getServiceHistory()));
    }
}
