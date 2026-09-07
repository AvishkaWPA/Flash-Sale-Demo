# ⚡ Flash Sale Concurrency & Multi-Threading Demo

A high-performance **Spring Boot** web application designed to demonstrate and test **multi-threading, concurrency control, and race conditions** during high-demand Flash Sale scenarios.

This application provides a real-time interactive dashboard to monitor product inventory, simulate concurrent purchase spikes, track oversold metrics, and inspect order stream logs.

---

## 🚀 Features

- ⚡ **High Concurrency Simulation:** Simulates simultaneous customer purchase requests to demonstrate race conditions and inventory overselling.
- 📊 **Real-time Admin Dashboard:** Built with **Thymeleaf**, **Bootstrap 5**, and custom CSS with dynamic scorecards for:
  - **Total Orders** (Total purchase attempts)
  - **Succeeded Orders** (Successfully placed orders)
  - **Out of Stock** (Rejected purchase attempts due to zero stock)
  - **Oversold Count** (Identifies race conditions where succeeded orders exceed original inventory)
- 🛒 **Product Management:** Dynamic product catalog with automatic fallback seeding, image rendering, and single-click stock reset capability.
- 📜 **Order Processing Stream:** Tracks order attempts with precise timestamps and execution status (`SUCCEEDED`, `FAILED_OUT_OF_STOCK`).
- 🛠️ **REST API Endpoints:** Clean REST APIs for placing orders, adding products, fetching product details, and clearing transaction history.

---

## 🛠️ Tech Stack

- **Backend:** Java 21, Spring Boot 3.4+ / 4.x
- **Persistence:** Spring Data JPA, Hibernate, PostgreSQL
- **Frontend / UI:** Thymeleaf, Bootstrap 5, Bootstrap Icons, Google Fonts (*Plus Jakarta Sans*)
- **Build & Management:** Apache Maven, Lombok

---

## 📂 Project Structure

```
FlashSale/
├── src/
│   ├── main/
│   │   ├── java/com/avishka/FlashSale/
│   │   │   ├── controller/
│   │   │   │   ├── OrderController.java       # Handles order placement & order management
│   │   │   │   └── ProductController.java     # Web dashboard & product APIs
│   │   │   ├── dtos/
│   │   │   │   └── req/
│   │   │   │       ├── CreateOrderRequest.java
│   │   │   │       └── ProductDto.java
│   │   │   ├── entity/
│   │   │   │   ├── Order.java                 # Order entity
│   │   │   │   └── Product.java               # Product inventory entity
│   │   │   ├── repositories/
│   │   │   │   ├── OrderRepository.java
│   │   │   │   └── ProductRepository.java
│   │   │   ├── service/
│   │   │   │   ├── OrderService.java          # Order & concurrency logic
│   │   │   │   └── ProductService.java        # Product management & initial seeders
│   │   │   └── FlashSaleApplication.java      # Spring Boot main entrypoint
│   │   └── resources/
│   │       ├── application.yaml               # Database & Server configuration
│   │       └── templates/
│   │           └── allProducts.html           # Dashboard UI
├── pom.xml                                    # Maven dependencies
└── README.md                                  # Project documentation
```

---

## 📋 Prerequisites

Before running the application, ensure you have the following installed:

- **Java JDK 21** or higher
- **PostgreSQL Database** running on port `5433` (or customize via environment variables)
- **Maven** (optional, wrapper `./mvnw` included)

---

## ⚙️ Configuration & Environment Setup

The database connection can be configured in `src/main/resources/application.yaml` or set using environment variables:

| Environment Variable | Default Value | Description |
| :--- | :--- | :--- |
| `PORT` | `8080` | Application HTTP Server Port |
| `DB_URL` | `jdbc:postgresql://localhost:5433/flashsale` | PostgreSQL Database Connection URL |
| `DB_USERNAME` | `postgres` | Database User |
| `DB_PASSWORD` | `Mylearn123@` | Database Password |

### 🗄️ Database Setup
Make sure to create a PostgreSQL database named `flashsale`:
```sql
CREATE DATABASE flashsale;
```

---

## 🏃 Getting Started

### 1. Clone the repository
```bash
git clone https://github.com/AvishkaWPA/Flash-Sale-Demo.git
cd FlashSale
```

### 2. Build and Run the Application
Using the Maven wrapper:
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

### 3. Access the Dashboard
Open your browser and navigate to:
```
http://localhost:8080/api/v1/product
```

---

## 📡 API Reference

### Product Endpoints

- **`GET /api/v1/product`**
  - Renders the Thymeleaf Flash Sale Dashboard UI.
- **`GET /api/v1/product/{productId}`**
  - Fetches details for a specific product by ID.
- **`POST /api/v1/product`**
  - Creates a new product.
  - **Body:**
    ```json
    {
      "name": "Gaming Laptop",
      "price": 1299.99,
      "stock": 100,
      "imageUrl": "https://example.com/image.jpg"
    }
    ```
- **`POST /api/v1/product/reset-stock`**
  - Resets all product stocks back to 100 and clears all recorded orders.

### Order Endpoints

- **`POST /api/v1/orders/buy`**
  - Places an order for a product.
  - **Body:**
    ```json
    {
      "productId": 1,
      "customerId": 101,
      "quantity": 1
    }
    ```
- **`POST /api/v1/orders/clear`**
  - Clears all order logs.

---

## 🔬 Understanding the Concurrency Problem

In high-traffic e-commerce flash sales, naive read-modify-write patterns:

```java
if (product.getStock() >= createOrderRequest.getQuantity()) {
    product.setStock(product.getStock() - createOrderRequest.getQuantity());
    productRepository.save(product);
    status = "SUCCEEDED";
}
```

suffer from **Race Conditions**. When hundreds of concurrent threads execute this check simultaneously:
1. Multiple threads read the stock value before any thread updates it.
2. All threads proceed to subtract stock and save, leading to **overselling** (negative effective stock / total orders exceeding initial capacity).

This project highlights this behavior and serves as a testing ground for implementing concurrency controls such as:
- **Pessimistic Locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`)**
- **Optimistic Locking (`@Version`)**
- **Distributed Locks (Redis / Redisson)**
- **Database Atomic Decrement Queries**

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
