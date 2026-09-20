# Car Agency Management System

A desktop car agency management application built in **Java** with **JavaFX**, developed as a Data Structures course project. It manages customers, vehicles, and reservations end-to-end — from maintenance requests to sales with automatic status tracking — using custom-built data structures and text-file loading.

## Features

### 🚗 Vehicles

- Add, update, and remove vehicles (new cars always enter as `AVAILABLE`)

- Search vehicles by ID

- View all vehicles ascending / descending (AVL in-order traversal)

- AVL tree height display and discount support

- Vehicle status tracking: `AVAILABLE`, `IN_SERVICE`, `RESERVED`, `SOLD` (workflow-driven, never typed by hand)

### 👥 Customers

- Register, update, and remove customer profiles

- Search customers by ID

- Waiting queue with fair FIFO serve and duplicate protection

### 🔧 Maintenance

- Create service requests with automatic IDs (vehicle must exist and be `AVAILABLE`)

- FIFO maintenance queue with front-to-rear display

- Process next request: `AVAILABLE → IN_SERVICE`

- Complete service with automatic IDs: record cost, push history stack, `IN_SERVICE → AVAILABLE`

### 📅 Reservations

- Reserve unavailable vehicles with automatic IDs and full FIFO order per vehicle

- `SOLD` cars can never be reserved; `DEPOSIT` holds a car as `RESERVED`

- Process the front reservation first when the vehicle is ready

### 💰 Transactions

- Create sales, service payments, deposits, and refunds with automatic IDs

- `SALE → SOLD` (amount must equal the current vehicle price), `DEPOSIT → RESERVED`, `REFUND → AVAILABLE`

- Vehicle table follows every transaction automatically

- Single atomic undo entry per sale / deposit / refund

### ↩️ Undo / Redo

- Two-stack undo/redo for reversible operations

- Sale, deposit, refund, reservation, discount, and CRUD operations all reversible

- Maintenance process/complete steps are intentional single workflow actions (no split undo entries)

## Data Structures

This is a Data Structures project — all collections are implemented **from scratch**, without using `java.util` collection classes in the core:

| Structure | Implementation | Used For |
| --- | --- | --- |
| `AVL` | Balanced binary search tree keyed by `vehicleId` | Vehicle inventory |
| `Queue<T>` | Linked list with `front`/`rear` pointers | Maintenance queue, waiting queue, reservation queue |
| `Stack<T>` | Linked list with `top` pointer | Undo stack, redo stack, service-history stack |
| `SimpleLinkedList<T>` | Singly linked list | Customers, transactions, ID sets, per-vehicle history index |

Core supports insert, search, delete, rotations, height/balance, in-order and reverse in-order traversal, `push`/`pop`/`peek`/`isEmpty`/`size`, and `enqueue`/`dequeue`/`peek`/`isEmpty`/`size`.

## Project Structure

```
src/
├── Main.java              # JavaFX entry point (tabbed UI)
├── MainLauncher.java      # Launcher
├── Agency.java            # Business logic + undo/redo + workflows
├── AVL.java               # Balanced vehicle inventory
├── AVLNode.java           # AVL tree node
├── Queue.java             # Custom FIFO queue
├── Stack.java             # Custom LIFO stack
├── SimpleLinkedList.java  # Custom singly linked list
├── Customer.java          # Customer model
├── Vehicle.java           # Vehicle model
├── ServiceRequest.java    # Queued maintenance request model
├── Service.java           # Completed service record model
├── Transaction.java       # Financial transaction model
└── FileLoader.java        # CSV loading via FileChooser
```

## Getting Started

### Prerequisites

- **JDK 26+**

- **JavaFX SDK** — [download here](https://openjfx.io/)

- IntelliJ IDEA (recommended)

### Run in IntelliJ IDEA

1. Clone the repository:

```
git clone https://github.com/A7mad-PSE/Car-Agency-Management-System.git
```

2. Open the project folder in IntelliJ IDEA.

3. Go to **File → Project Structure → Libraries** and add the JavaFX SDK's `lib` folder as a library.

4. Run `MainLauncher.main()`.

### Run from the command line

```
# Compile (set PATH_TO_FX to your JavaFX lib directory)
javac --module-path $PATH_TO_FX --add-modules javafx.controls -d out src/*.java

# Run
java --module-path $PATH_TO_FX --add-modules javafx.controls -cp out MainLauncher
```

## Data Persistence

Data is loaded from plain-text CSV files through the toolbar's **FileChooser**. Load them in this order so references resolve. The app validates every line and skips bad rows with a warning instead of crashing.

| File | Format |
| --- | --- |
| `customers.txt` | `id,name,phone,address` |
| `vehicles.txt` | `id,make,model,year,price,color,status` |
| `service_requests.txt` | `requestId,vehicleId,customerId,type,date,status` |
| `services.txt` | `serviceId,vehicleId,customerId,type,date,cost,status` |
| `transactions.txt` | `transactionId,customerId,vehicleId,amount,type,date` |

Example — `vehicles.txt`:

```
10,Toyota,Corolla,2022,25000,White,AVAILABLE
20,Ford,Focus,2020,18000,Red,SOLD
30,Honda,Civic,2021,22000,Silver,IN_SERVICE
```

Only `PENDING` requests enter the maintenance queue — `COMPLETED` / `CANCELLED` / `PROCESSING` rows are reported and skipped. File vehicle statuses are preserved as-is.

## Validation Rules

- **Vehicle ID / price / year:** positive ID, non-negative price, year between 1886 and next year

- **Make / color / customer name:** letters, spaces, hyphens, and apostrophes only

- **Phone:** digits only, 7–15 characters

- **Dates:** strict `YYYY-MM-DD` calendar dates (check `2026-09-01`, not `2026-13-99`)

- **Vehicle status:** only `AVAILABLE`, `SOLD`, `IN_SERVICE`, `RESERVED`

- **Sale amount:** must equal the current vehicle price (discounts included)

- **Deposit:** must be within the vehicle price; **refund:** non-zero and within price magnitude

- **No duplicates:** customer, vehicle, request, service, transaction, and reservation IDs are all unique

- **Status rules:** `SOLD` can never be re-sold or reserved; `IN_SERVICE` cannot be sold or assigned

## Author

**Ahmad Ali Hasan** — [A7mad-PSE](https://github.com/A7mad-PSE)
