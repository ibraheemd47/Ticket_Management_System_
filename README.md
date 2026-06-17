# 🎫 Ticket Management System

A Spring Boot web application for managing support tickets with a modern Vaadin-based user interface, JWT authentication, Spring Security, and real-time updates using WebSocket.

## 📌 Overview

The **Ticket Management System** allows users to create, view, update, and manage support tickets through a web-based interface.
The system includes secure authentication, ticket management functionality, real-time communication, and support for email notifications.

## ✨ Features

* 🔐 User authentication using JWT
* 🛡️ Secure access control with Spring Security
* 🎫 Create, view, update, and delete support tickets
* ⚡ Real-time ticket updates using WebSocket
* 🖥️ Vaadin-based responsive web interface
* 🗄️ H2 embedded database for development and testing
* 📧 Email notification support through SMTP configuration

## 🛠️ Technologies Used

| Technology           | Description                      |
| -------------------- | -------------------------------- |
| ☕ Java 21            | Main programming language        |
| 🌱 Spring Boot 3.3.5 | Backend framework                |
| 🎨 Vaadin 24.5.1     | Web UI framework                 |
| 🛡️ Spring Security  | Authentication and authorization |
| 🔑 JWT               | Token-based authentication       |
| 🔄 WebSocket         | Real-time communication          |
| 🗄️ H2 Database      | Embedded development database    |
| 📦 Maven             | Build and dependency management  |

## ✅ Prerequisites

Before running the project, make sure you have the following installed:

* ☕ Java 21 or higher
* 📦 Maven 3.6+
  Or use the included Maven wrapper
* 🧩 Git

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/ibraheemd47/Ticket_Management_System_.git
cd Ticket_Management_System_
```

Replace `<repository-url>` with your actual GitHub repository URL.

### 2. Build the Project

Using Maven:

```bash
mvn clean install
```

Or using the Maven wrapper:

#### 🪟 Windows

```bash
mvnw.cmd clean install
```

#### 🐧 Linux / macOS

```bash
./mvnw clean install
```

## ▶️ Running the Application

Run the application using Maven:

```bash
mvn spring-boot:run
```

Or using the Maven wrapper:

#### 🪟 Windows

```bash
mvnw.cmd spring-boot:run
```

#### 🐧 Linux / macOS

```bash
./mvnw spring-boot:run
```

## 🌐 Accessing the Application

After the application starts successfully, open your browser and go to:

```text
http://localhost:8080
```

You should see the Vaadin web interface.

## ⚙️ Configuration

The application configuration is located in:

```text
src/main/resources/application.properties
```



Main configurable options include:

* 🌐 Server port
* 🗄️ H2 database settings
* 🔑 JWT secret key
* 🔄 WebSocket configuration
* 📧 SMTP email settings

By default, the application runs on:

```text
http://localhost:8080
```

## 📁 Project Structure

```text
src/
├── main/
│   ├── java/
│   │   └── com/sdnah/
│   │       └── [Application classes]
│   └── resources/
│       ├── application.properties
│       └── templates/
└── test/
    └── [Test classes]
```

## 🧪 Running Tests

To run the project tests:

```bash
mvn test
```

Or with the Maven wrapper:

#### 🪟 Windows

```bash
mvnw.cmd test
```

#### 🐧 Linux / macOS

```bash
./mvnw test
```

## ⚙️ System Configuration and Initialization

The application can be configured using:

```text
src/main/resources/application.properties
```

The project supports two database configurations:

1. PostgreSQL for shared or production environments.
2. H2 for local development and testing.

### PostgreSQL Configuration

The default configuration uses PostgreSQL and reads the database credentials from environment variables:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

The required environment variables are:

| Variable        | Description                                        | Example format                                   |
| --------------- | -------------------------------------------------- | ------------------------------------------------ |
| `DB_URL`        | PostgreSQL JDBC connection URL                     | `jdbc:postgresql://localhost:5432/ticket_system` |
| `DB_USERNAME`   | PostgreSQL username                                | `postgres`                                       |
| `DB_PASSWORD`   | PostgreSQL password                                | `your-password`                                  |
| `MAIL_PASSWORD` | Gmail application password used for sending emails | `application-password`                           |
| `JWT_SECRET`    | Secret key used for signing JWT tokens             | A random string of at least 32 characters        |

Example for Windows PowerShell:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/ticket_system"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your-password"
$env:MAIL_PASSWORD="your-mail-application-password"
$env:JWT_SECRET="your-long-random-jwt-secret"

mvn spring-boot:run
```

Example for Linux or macOS:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/ticket_system"
export DB_USERNAME="postgres"
export DB_PASSWORD="your-password"
export MAIL_PASSWORD="your-mail-application-password"
export JWT_SECRET="your-long-random-jwt-secret"

./mvnw spring-boot:run
```

Secrets and passwords should not be committed to GitHub.

### H2 Configuration

For local development using the in-memory H2 database, comment out the PostgreSQL configuration and enable the following properties:

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

After starting the application, the H2 console is available at:

```text
http://localhost:8080/h2-console
```

Because H2 is configured as an in-memory database, its data is deleted when the application stops.

---

## 🌱 Initial-State File

The system supports automatic initialization from a JSON file.

The default file location is:

```text
src/main/resources/initial-state.json
```

The feature is controlled by the following properties:

```properties
app.initial-state.enabled=false
app.initial-state.file=classpath:initial-state.json
```

To load the initial state when the application starts, change the configuration to:

```properties
app.initial-state.enabled=true
app.initial-state.file=classpath:initial-state.json
```

To disable automatic initialization:

```properties
app.initial-state.enabled=false
```

An external file can also be used:

```properties
app.initial-state.enabled=true
app.initial-state.file=file:./initial-state.json
```

The initial-state file is processed automatically during Spring Boot startup.

### General File Format

The file must contain a JSON array:

```json
[
  {
    "action": "register",
    "username": "u1",
    "password": "123456",
    "email": "u1@test.com"
  },
  {
    "action": "login",
    "username": "u1",
    "password": "123456",
    "saveTokenAs": "u1Token"
  }
]
```

Each object represents one action.

Actions are executed sequentially, from the first object to the last object. Therefore, an action may only reference users, tokens, companies, or events created by an earlier action.

If an action is invalid, system initialization stops and the error message identifies the number and content of the failed action.

### Saved Variables

Some actions create values that can be saved under aliases and used by later actions.

For example:

```json
{
  "action": "login",
  "username": "u1",
  "password": "123456",
  "saveTokenAs": "u1Token"
}
```

The saved token can later be referenced using `$`:

```json
{
  "action": "openCompany",
  "token": "$u1Token",
  "companyName": "p1",
  "saveCompanyAs": "p1"
}
```

The saved company can then be referenced as:

```json
{
  "action": "appointOwner",
  "token": "$u1Token",
  "company": "$p1",
  "ownerUsername": "u2"
}
```

Supported saved values are:

| Saved value          | Created by      | Referenced as   |
| -------------------- | --------------- | --------------- |
| Authentication token | `saveTokenAs`   | `$variableName` |
| Company ID           | `saveCompanyAs` | `$variableName` |
| Event ID             | `saveEventAs`   | `$variableName` |

The variables exist only while the initial-state file is being processed.

---

## 📋 Supported Initial-State Actions

### Register a User

```json
{
  "action": "register",
  "username": "u1",
  "password": "123456",
  "email": "u1@test.com",
  "phone": "0500000001",
  "age": 23
}
```

Required fields:

* `action`
* `username`
* `password`
* `email`

Optional fields:

* `phone` — default value: `0500000000`
* `age` — default value: `18`

### Verify a User

```json
{
  "action": "verify",
  "username": "u1",
  "code": "000000"
}
```

Required field:

* `username`

Optional field:

* `code` — default value: `000000`

### Log In

```json
{
  "action": "login",
  "username": "u1",
  "password": "123456",
  "saveTokenAs": "u1Token"
}
```

The generated authentication token is stored under the name provided in `saveTokenAs`.

### Log Out

```json
{
  "action": "logout",
  "token": "$u1Token"
}
```

### Open a Production Company

```json
{
  "action": "openCompany",
  "token": "$u1Token",
  "companyName": "p1",
  "saveCompanyAs": "p1"
}
```

The generated company UUID is stored under the name provided in `saveCompanyAs`.

### Appoint an Additional Owner

```json
{
  "action": "appointOwner",
  "token": "$u1Token",
  "company": "$p1",
  "ownerUsername": "u2"
}
```

### Confirm an Owner Appointment

```json
{
  "action": "confirmOwnerAppointment",
  "token": "$u2Token",
  "company": "$p1"
}
```

In the current implementation, the owner role is assigned during the `appointOwner` action. The confirmation action validates the confirming user and the referenced company.

### Appoint a Manager

```json
{
  "action": "appointManager",
  "token": "$u2Token",
  "company": "$p1",
  "managerUsername": "u3",
  "permissions": [
    "EDIT_VENUE_LAYOUT"
  ]
}
```

`permissions` must be a JSON array.

The initialization loader maps:

```text
EDIT_VENUE_LAYOUT
```

to the internal permission:

```text
MANAGE_EVENTS
```

### Confirm a Manager Appointment

```json
{
  "action": "confirmManagerAppointment",
  "token": "$u3Token",
  "company": "$p1"
}
```

In the current implementation, the manager role is assigned during the `appointManager` action. The confirmation action validates the confirming user and the referenced company.

### Create an Event with a Venue Layout

```json
{
  "action": "createEventWithLayout",
  "token": "$u2Token",
  "company": "$p1",
  "eventName": "e1",
  "eventType": "PERFORMANCE",
  "venue": "Main Hall",
  "startDate": "2026-07-01",
  "endDate": "2026-07-01",
  "saveEventAs": "e1",
  "show": {
    "name": "e1-show",
    "description": "Initial event",
    "singer": "N/A",
    "showDate": "2026-07-01",
    "standingCapacity": 30,
    "standingPrice": 50,
    "numberOfBlocks": 1,
    "rowsPerBlock": 10,
    "seatsPerRow": 10,
    "seatedPrice": 100
  }
}
```

Date fields must use the following format:

```text
YYYY-MM-DD
```

For example:

```text
2026-07-01
```

The total number of seated tickets is:

```text
numberOfBlocks × rowsPerBlock × seatsPerRow
```

For example:

```text
1 × 10 × 10 = 100 seated tickets
```

The example also creates 30 standing tickets, with:

* standing ticket price: `50`
* seated ticket price: `100`

Optional show fields:

| Field              | Default value        |
| ------------------ | -------------------- |
| `description`      | `Initial-state show` |
| `singer`           | `N/A`                |
| `standingCapacity` | `0`                  |
| `numberOfBlocks`   | `0`                  |
| `rowsPerBlock`     | `0`                  |
| `seatsPerRow`      | `0`                  |

### Add a Company Coupon

```json
{
  "action": "addCompanyCoupon",
  "token": "$u2Token",
  "company": "$p1",
  "couponCode": "sale123",
  "percentage": 20
}
```

The `percentage` field must contain a numeric value.

In this example, the coupon code `sale123` provides a 20% company discount.

---

## ▶️ Initializing the Required Example State

The supplied `initial-state.json` initializes the following state:

1. Registers users `u1`, `u2`, `u3`, and `u4`.
2. Verifies all four users.
3. Logs in `u1`.
4. Creates production company `p1`, owned by `u1`.
5. Appoints `u2` as an additional owner.
6. Logs in `u2` and confirms the owner appointment.
7. Appoints `u3` as a manager with venue-layout editing permission.
8. Logs in `u3` and confirms the manager appointment.
9. Creates event `e1`.
10. Creates 30 standing tickets priced at `50`.
11. Creates one seated block containing 10 rows with 10 seats per row, for a total of 100 seated tickets priced at `100`.
12. Adds a 20% company coupon with the code `sale123`.
13. Logs out all logged-in users.

To load this state:

```properties
app.initial-state.enabled=true
app.initial-state.file=classpath:initial-state.json
```

Then start the application:

```bash
mvn spring-boot:run
```

A successful initialization prints:

```text
========== Loading initial state file ==========
Initial state action #1 completed: register
...
========== Initial state loaded successfully ==========
```

### Important Initialization Note

The example initial-state file creates users and a company with fixed names.

Running it again against a database that already contains the same users or company may fail because usernames, email addresses, and company names may be unique.

Use one of the following before loading the file again:

* start with an empty database;
* remove the previously initialized records;
* change the usernames, email addresses, and company name in the JSON file;
* disable initialization after the first successful execution:

```properties
app.initial-state.enabled=false
```


This project uses an embedded H2 database, which is useful for development and testing.
For production use, it is recommended to configure an external database and secure the JWT secret and email credentials properly.

## 📄 License

This project is for academic and learning purposes.
