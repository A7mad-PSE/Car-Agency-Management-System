# Car Agency Management System

A data-structures-driven desktop app for managing a car agency's customers, vehicle inventory,
maintenance workflow, reservations, and financial transactions — built with Java and JavaFX.

Core structures (AVL tree, linked-list Queue, linked-list Stack) are implemented from scratch.
No `ArrayList`, `LinkedList`, or other built-in collections are used inside the core —
custom linked structures back everything except the JavaFX table adapters.

## Features

- **Customers** — add, search, update, delete
- **Vehicles (AVL inventory)** — ordered by `vehicleId`, ascending/descending traversal, tree height,
  discount support; status is workflow-driven (`AVAILABLE` / `IN_SERVICE` / `RESERVED` / `SOLD`)
- **Maintenance** — service requests queue (FIFO), process next (`AVAILABLE` → `IN_SERVICE`),
  complete service (→ `AVAILABLE`, pushes history stack); request/service IDs auto-generated
- **Waiting queue** — fair FIFO enqueue/serve/count
- **Reservations** — per-vehicle FIFO queue for unavailable cars, auto IDs, assign front-first
- **Transactions** — sale / service-payment / deposit / refund with auto IDs;
  `SALE → SOLD`, `DEPOSIT → RESERVED`, `REFUND → AVAILABLE`, linked live to the vehicle table
- **Undo / Redo** — two-stack support for reversible operations (sale/deposit/refund are single atomic entries)
- **File loading** — `FileChooser` CSV import for customers, vehicles, requests, services, transactions
- **Validation** — duplicates, bad dates/phones/years, empty queues/stacks, and status rules are all guarded

## Data Structures

| Structure | Backing | Operations | Complexity |
|---|---|---|---|
| `AVL` (`AVL.java`, `AVLNode.java`) | Linked nodes, height-balanced | insert / delete / find | `O(log n)` |
| `AVL` traversal | In-order / reverse in-order | ascending / descending, height | `O(n)` / `O(1)` cached |
| `Queue<T>` (`Queue.java`) | Linked `front`/`rear` | enqueue / dequeue / peek / size | `O(1)` |
| `Stack<T>` (`Stack.java`) | Linked `top` | push / pop / peek / size | `O(1)` |
| `SimpleLinkedList<T>` | Singly linked | add / remove / get / contains | `O(1)` add, `O(n)` search/remove |
| Undo / Redo | Two `Stack<Operation>` | undo / redo | `O(1)` |

Business rules worth knowing:

- New cars enter as `AVAILABLE`. The vehicle file keeps its own statuses on import.
- A `SALE` amount must equal the current vehicle price (discounts included) — otherwise it is
  rejected and the car stays unsold. Historical file rows load as records without this check.
- `DEPOSIT` holds a car (`AVAILABLE` → `RESERVED`); `REFUND` releases it (`RESERVED` → `AVAILABLE`).
- Only `PENDING` requests enter the maintenance queue; `COMPLETED` / `CANCELLED` / `PROCESSING`
  rows are skipped with a notice.
- Customer IDs match numerically, so file ID `09` resolves to customer `9`.

See [COMPLEXITY.txt](COMPLEXITY.txt) for the full time-complexity analysis.

## Project Layout

```
src/
  Agency.java            # business logic + undo/redo + workflows
  AVL.java / AVLNode.java# balanced vehicle inventory
  Queue.java / Stack.java# linked-list FIFO / LIFO
  SimpleLinkedList.java  # custom list for entities (no java.util in core)
  Customer/Vehicle/ServiceRequest/Service/Transaction.java
  FileLoader.java        # CSV import via FileChooser
  Main.java              # JavaFX UI (7 tabs)
  MainLauncher.java      # entry point
customers.txt / vehicles.txt / service_requests.txt / services.txt / transactions.txt
COMPLEXITY.txt
```

## Run

Requires JDK 17+ and the JavaFX SDK (tested with JavaFX 26).

```bash
# compile
javac --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls \
  -d out src/*.java

# launch
java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls \
  -cp out MainLauncher
```

## Data File Formats (comma-separated, no header required)

```
# customers:  id,name,phone(7-15 digits),address (commas allowed)
C1,Ahmed,0777123456,Amman

# vehicles:  id,make,model,year,price,color,status
10,Toyota,Corolla,2022,25000,White,AVAILABLE

# service requests:  requestId,vehicleId,customerId,type,date(YYYY-MM-DD),status
R1,10,C1,Oil Change,2026-09-01,PENDING

# services:  serviceId,vehicleId,customerId,type,date,cost,status
S1,10,C1,Oil Change,2026-08-10,120.0,COMPLETED

# transactions:  transactionId,customerId,vehicleId,amount,type,date
T1,C1,10,25000,SALE,2026-08-10
```

Load order matters: **Customers → Vehicles → Service Requests → Services → Transactions**.

## UI Tabs

| Tab | Actions |
|---|---|
| Customers | Add, search, update, delete, refresh |
| Vehicles | Add (as `AVAILABLE`), search, update, delete, ascending/descending, height, apply discount |
| Maintenance | Create request (auto ID), process next, complete service (auto ID), queue + history views |
| Waiting Queue | Enqueue by customer ID, serve next (FIFO), count |
| Reservations | Add (auto ID), process next for a vehicle |
| Transactions | Create sale / service-payment / deposit / refund (auto ID); vehicle status follows |
| Undo / Redo | Undo, redo, peek next, full refresh |

## Demo Scenarios

1. **AVL search** — insert `50, 30, 70, 20, 40, 60, 80`, search `60`, change status, confirm the node stays.
2. **Waiting FIFO** — enqueue Ahmed, Sara, Omar; serve once → Ahmed leaves, Sara is front.
3. **Maintenance FIFO** — queue Oil Change / Brake Service / Tire Replacement; they process in arrival order.
4. **Undo** — Add Customer → Add Vehicle → Create Transaction → Apply Discount; one Undo reverses the discount first.
5. **Integrated flow** — request maintenance for vehicle `60`: search AVL → verify → enqueue →
   dequeue (`IN_SERVICE`) → complete (history push, `AVAILABLE`).
