import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class FileLoader {

    public static SimpleLinkedList<String[]> readRows(File file) {
        SimpleLinkedList<String[]> rows = new SimpleLinkedList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                rows.add(line.split(","));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read file: " + file.getName(), e);
        }
        return rows;
    }

    public static int loadCustomers(Agency agency, File file) {
        int count = 0;
        SimpleLinkedList<String[]> rows = readRows(file);
        for (int i = 0; i < rows.size(); i++) {
            String[] f = rows.get(i);
            if (f.length < 4) continue;
            if (f[0].trim().equalsIgnoreCase("customerId")) continue;
            String address = f[3].trim();
            for (int j = 4; j < f.length; j++) address += "," + f[j].trim();
            try {
                agency.addCustomer(new Customer(f[0].trim(), f[1].trim(), f[2].trim(), address));
                count++;
            } catch (IllegalArgumentException e) {
                System.out.println("Skipped line (customer): " + e.getMessage());
            }
        }
        return count;
    }

    public static int loadVehicles(Agency agency, File file) {
        int count = 0;
        SimpleLinkedList<String[]> rows = readRows(file);
        for (int i = 0; i < rows.size(); i++) {
            String[] f = rows.get(i);
            if (f.length < 6) continue;
            if (f[0].trim().equalsIgnoreCase("vehicleId") || f[0].trim().equalsIgnoreCase("id")) continue;
            try {
                int id = Integer.parseInt(f[0].trim());
                int year = Integer.parseInt(f[3].trim());
                double price = Double.parseDouble(f[4].trim());
                String status = f.length >= 7 && !f[6].trim().isEmpty() ? f[6].trim().toUpperCase() : Agency.AVAILABLE;
                agency.addVehicle(new Vehicle(id, f[1].trim(), f[2].trim(), year, price, f[5].trim(), status));
                count++;
            } catch (NumberFormatException e) {
                System.out.println("Skipped line (vehicle): number format error.");
            } catch (IllegalArgumentException e) {
                System.out.println("Skipped line (vehicle): " + e.getMessage());
            }
        }
        return count;
    }

    public static int loadServiceRequests(Agency agency, File file) {
        int count = 0;
        SimpleLinkedList<String[]> rows = readRows(file);
        for (int i = 0; i < rows.size(); i++) {
            String[] f = rows.get(i);
            if (f.length < 6) continue;
            if (f[0].trim().equalsIgnoreCase("requestId")) continue;
            try {
                int id = Integer.parseInt(f[1].trim());
                String status = f[5].trim().isEmpty() ? "PENDING" : f[5].trim().toUpperCase();
                // Only PENDING requests belong in the FIFO maintenance queue.
                // COMPLETED / CANCELLED are history, PROCESSING is already in progress.
                if (!status.equals("PENDING")) {
                    System.out.println("Skipped service request " + f[0].trim() + " (status " + status + " is not PENDING).");
                    continue;
                }
                agency.enqueueServiceRequest(
                        new ServiceRequest(f[0].trim(), id, f[2].trim(), f[3].trim(), f[4].trim(), status));
                count++;
            } catch (NumberFormatException e) {
                System.out.println("Skipped line (service request): number format error.");
            } catch (IllegalArgumentException e) {
                System.out.println("Skipped line (service request): " + e.getMessage());
            }
        }
        return count;
    }

    public static int loadServices(Agency agency, File file) {
        int count = 0;
        SimpleLinkedList<String[]> rows = readRows(file);
        for (int i = 0; i < rows.size(); i++) {
            String[] f = rows.get(i);
            if (f.length < 7) continue;
            if (f[0].trim().equalsIgnoreCase("serviceId")) continue;
            try {
                int id = Integer.parseInt(f[1].trim());
                double cost = Double.parseDouble(f[5].trim());
                String status = f[6].trim().isEmpty() ? "COMPLETED" : f[6].trim();
                agency.pushServiceHistory(
                        new Service(f[0].trim(), id, f[2].trim(), f[3].trim(), f[4].trim(), cost, status));
                count++;
            } catch (NumberFormatException e) {
                System.out.println("Skipped line (service): number format error.");
            } catch (IllegalArgumentException e) {
                System.out.println("Skipped line (service): " + e.getMessage());
            }
        }
        return count;
    }

    public static int loadTransactions(Agency agency, File file) {
        int count = 0;
        SimpleLinkedList<String[]> rows = readRows(file);
        for (int i = 0; i < rows.size(); i++) {
            String[] f = rows.get(i);
            if (f.length < 6) continue;
            if (f[0].trim().equalsIgnoreCase("transactionId")) continue;
            try {
                int id = Integer.parseInt(f[2].trim());
                double amount = Double.parseDouble(f[3].trim());
                agency.appendTransaction(
                        new Transaction(f[0].trim(), f[1].trim(), id, amount, f[4].trim(), f[5].trim()));
                count++;
            } catch (NumberFormatException e) {
                System.out.println("Skipped line (transaction): number format error.");
            } catch (IllegalArgumentException e) {
                System.out.println("Skipped line (transaction): " + e.getMessage());
            }
        }
        return count;
    }
}
