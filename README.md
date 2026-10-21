# 💰 Loan Management System

> A full-stack web application for managing loan applications, EMI calculations, repayments, and outstanding loan balances — built with **Java, Spring Boot, Spring Data MongoDB, and JSP**.

---

## 📌 Overview

The **Loan Management System** is a web-based application designed to simplify the process of managing customer loans and repayments.

The application allows users to create an account, securely log in, apply for a loan, calculate EMI based on loan details, make repayments, and track their remaining loan balance.

The project follows a **layered architecture** using Spring Boot, with separate Controller, Service, Repository, and Entity layers for better maintainability and scalability.

---

## ✨ Key Features

### 👤 User Management

* User registration
* User login/logout
* Session-based user authentication
* User-specific loan information

### 🏦 Loan Management

* Apply for a new loan
* Store loan details in MongoDB
* View active loan information
* Associate loans with users
* Track outstanding loan balance

### 💵 EMI & Repayment

* Automatic EMI calculation
* Interest-based loan calculation
* Loan repayment functionality
* Automatic remaining balance calculation
* Payment tracking

### 🗄️ Database Management

* MongoDB database integration
* Spring Data MongoDB
* MongoDB document mapping
* Document references
* Automatic index creation

---

## 🛠️ Technology Stack

| Category            | Technologies                                |
| ------------------- | ------------------------------------------- |
| **Language**        | Java 17                                     |
| **Backend**         | Spring Boot 3.5.10                          |
| **Web Framework**   | Spring MVC                                  |
| **Persistence**     | Spring Data MongoDB                        |
| **Data Access**     | Spring Data MongoDB                             |
| **Frontend**        | JSP, HTML, CSS                              |
| **Database**        | MongoDB                                       |
| **Build Tool**      | Maven                                       |
| **Version Control** | Git & GitHub                                |
| **IDE**             | Eclipse / Spring Tool Suite / IntelliJ IDEA |

---

## 🏗️ Architecture

The application follows a clean **Layered Architecture**:

```text
                    ┌──────────────────────┐
                    │      JSP / UI        │
                    │   HTML + CSS + JSP   │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │     Controller       │
                    │    Spring MVC        │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │       Service        │
                    │   Business Logic     │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │      Repository      │
                    │   Spring Data MongoDB    │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │       MongoDB          │
                    │      Database        │
                    └──────────────────────┘
```

This separation makes the application easier to:

* Maintain
* Debug
* Test
* Extend
* Scale

---

## 📂 Project Structure

```text
loan_app
│
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com.loanproject.loan_app
│   │   │       │
│   │   │       ├── bean
│   │   │       │   ├── User.java
│   │   │       │   └── Loan.java
│   │   │       │
│   │   │       ├── controller
│   │   │       │   ├── HomeController.java
│   │   │       │   └── LoanController.java
│   │   │       │
│   │   │       ├── repository
│   │   │       │   ├── UserRepository.java
│   │   │       │   └── LoanRepository.java
│   │   │       │
│   │   │       ├── services
│   │   │       │   ├── UserService.java
│   │   │       │   └── LoanService.java
│   │   │       │
│   │   │       └── LoanAppApplication.java
│   │   │
│   │   └── resources
│   │       ├── META-INF
│   │       │   └── resources
│   │       │       └── WEB-INF
│   │       │           └── pages
│   │       │               ├── index.jsp
│   │       │               ├── login.jsp
│   │       │               ├── register.jsp
│   │       │               ├── dashboard.jsp
│   │       │               ├── applyLoan.jsp
│   │       │               ├── pay.jsp
│   │       │               └── header.jsp
│   │       │
│   │       └── application.properties
│   │
│   └── test
│
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .gitignore
└── README.md
```

---

# 🔄 Application Workflow

```text
                    START
                      │
                      ▼
                ┌───────────┐
                │  Register │
                └─────┬─────┘
                      │
                      ▼
                ┌───────────┐
                │   Login   │
                └─────┬─────┘
                      │
                      ▼
                ┌──────────────┐
                │   Dashboard  │
                └──────┬───────┘
                       │
              ┌────────┴────────┐
              ▼                 ▼
        ┌───────────┐     ┌────────────┐
        │ Apply Loan│     │ View Loans │
        └─────┬─────┘     └──────┬─────┘
              │                  │
              ▼                  │
        ┌─────────────┐          │
        │ EMI         │          │
        │ Calculation │          │
        └──────┬──────┘          │
               │                 │
               ▼                 │
        ┌─────────────┐          │
        │ Loan Stored │◄─────────┘
        │  in MongoDB   │
        └──────┬──────┘
               │
               ▼
        ┌─────────────┐
        │    Pay      │
        │    Loan     │
        └──────┬──────┘
               │
               ▼
        ┌─────────────┐
        │  Remaining  │
        │   Balance   │
        └─────────────┘
```

---

# 🧮 EMI Calculation

The system calculates the monthly EMI based on:

* Principal loan amount
* Annual interest rate
* Loan tenure

The standard EMI formula is:

```text
EMI = P × R × (1 + R)^N / ((1 + R)^N - 1)
```

Where:

```text
P = Principal loan amount
R = Monthly interest rate
N = Loan tenure in months
```

This provides users with an estimated monthly repayment amount before managing their loan.

---

# 🗃️ Database Design

The application uses **MongoDB** with Spring Data MongoDB.

### User

```text
User
-------------------------
id
name
email
password
```

### Loan

```text
Loan
-------------------------
id
loanId
amount
interest
months
emi
remaining
user
```

### Entity Relationship

```text
             USER
              │
              │ 1
              │
              │
              │ *
             LOAN
```

A user can have one or more associated loan records.

---

# 🌐 Main Endpoints

| Method | Endpoint     | Purpose                   |
| ------ | ------------ | ------------------------- |
| `GET`  | `/`          | Home page                 |
| `GET`  | `/login`     | Display login page        |
| `POST` | `/login`     | Authenticate user         |
| `GET`  | `/register`  | Display registration page |
| `POST` | `/register`  | Register new user         |
| `GET`  | `/dashboard` | Display user dashboard    |
| `GET`  | `/applyLoan` | Display loan application  |
| `POST` | `/applyLoan` | Submit loan application   |
| `GET`  | `/payPage`   | Display payment page      |
| `POST` | `/pay`       | Process loan payment      |

---

# ⚙️ Setup & Installation

## 1️⃣ Clone the Repository

```bash
git clone https://github.com/YOUR-USERNAME/loan-management-system.git
```

Navigate into the project:

```bash
cd loan-management-system
```

---

## 2 Configure MongoDB

For local development, the application defaults to `mongodb://localhost:27017/Bank-application`.
For a cloud MongoDB provider such as Atlas, set `MONGODB_URI` or
`SPRING_DATA_MONGODB_URI` to the provider connection string. Never commit database
credentials to Git. On Render, set `MONGODB_URI` as a secret environment variable.

---

## 4 Build the Project

Using Maven:

```bash
mvn clean install
```

Windows:

```bash
mvnw.cmd clean install
```

---

## 5️⃣ Run the Application

```bash
mvn spring-boot:run
```

Or run:

```text
LoanAppApplication.java
```

from your IDE.

---

## 6️⃣ Open the Application

Once the application starts, open:

```text
http://localhost:8080/
```

---

# 🧪 Testing

Run the automated tests using:

```bash
mvn test
```

or on Windows:

```bash
mvnw.cmd test
```

---

# 🔐 Security Considerations

The current project uses session-based authentication for user access.

For a production-ready version, the following improvements are recommended:

* Spring Security
* BCrypt password hashing
* Role-based access control
* CSRF protection
* Input validation
* Global exception handling
* Secure environment variables
* HTTPS

---

# 🚀 Future Enhancements

The project can be extended with:

* [ ] Spring Security authentication
* [ ] BCrypt password encryption
* [ ] Admin dashboard
* [ ] Loan approval/rejection workflow
* [ ] Payment history
* [ ] Detailed repayment schedule
* [ ] Email notifications
* [ ] REST APIs
* [ ] React.js frontend
* [ ] JWT authentication
* [ ] Docker containerization
* [ ] Cloud deployment
* [ ] Loan status tracking
* [ ] PDF loan statement generation

---

# 📈 What This Project Demonstrates

This project demonstrates practical experience with:

```text
Java
  ↓
Spring Boot
  ↓
Spring MVC
  ↓
Service Layer
  ↓
Spring Data MongoDB
MongoDB
  ↓
JSP
```

### Core Development Concepts

* Object-Oriented Programming
* MVC Architecture
* Layered Architecture
* Dependency Injection
* Inversion of Control
* Spring Data MongoDB repositories
* Entity Relationships
* CRUD Operations
* Database Integration
* Session Management
* Business Logic Implementation
* Exception Handling
* Maven Project Management

---

# 🎯 Project Highlights

⭐ **Java 17 + Spring Boot** backend

⭐ **Spring Data MongoDB** for persistence

⭐ **MongoDB** document database

⭐ **JSP-based web interface**

⭐ **Layered architecture** for maintainable code

⭐ **Loan & repayment workflow**

⭐ **Dynamic EMI calculation**

⭐ **User-specific loan management**

⭐ **Maven-based project**

---

# 👨‍💻 Author

## Onkar Karande

**Java Developer | Spring Boot | MongoDB | Full Stack Development**

Interested in building scalable and maintainable web applications using Java and modern software development practices.

---

## ⭐ If you find this project useful

If this project helped you understand Java Spring Boot and loan management workflows, consider giving the repository a **⭐ Star**.

---

## 📄 License

This project is developed for **educational and portfolio purposes**.

