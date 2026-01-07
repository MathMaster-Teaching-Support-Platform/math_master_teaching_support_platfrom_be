# MathMaster Backend

MathMaster Backend is the server-side system of **MathMaster – Teaching Support Platform for Mathematics Educators**.  
The system provides RESTful APIs to support AI-powered mathematical content generation, teaching material management, assessments, and user management for teachers, students, and system administrators.

---

## 📖 Project Information

- **Project Name**: MathMaster – Teaching Support Platform for Mathematics Educators  
- **Vietnamese Name**: MathMaster – Nền tảng hỗ trợ giảng dạy dành cho giáo viên môn Toán  
- **Abbreviation**: SP26SE026  
- **Project Type**: Capstone Project  
- **Domain**: Education Technology (EdTech – Mathematics)

---

## 🏗️ Architecture Overview

- Architecture Style: Layered Architecture  
  - Controller → Service → Repository  
- API Style: RESTful APIs  
- Authentication: JWT-based Authentication  
- API Documentation: OpenAPI / Swagger  

---

## 🛠️ Technology Stack

### Backend
- Java 17  
- Spring Boot  
- Spring Security  
- Maven  

### Database
- PostgreSQL (relational data)  
- MongoDB (AI-generated content & documents)  
- Flyway (database migration)  

### DevOps
- Docker  
- GitHub Actions (CI)  

---

## 📂 Project Structure

src/main/java/com/mathmaster/
├── config/ # Security, Swagger, CORS configuration
├── controller/ # REST controllers
├── service/ # Business logic
├── repository/ # JPA & Mongo repositories
├── domain/ # Entities / Documents
├── dto/ # Request / Response DTOs
├── mapper/ # DTO - Entity mapping
├── exception/ # Global exception handling
└── MathMasterApplication.java

markdown
Copy code

---

## 🔑 Functional Features

### Teacher
- Register and manage account  
- Generate AI-powered math content  
- Create diagrams, graphs, mindmaps, and lesson plans  
- Generate exercises, quizzes, and exams  
- Manage teaching resources  
- Export materials (PDF, PPTX, DOCX)  

### Student
- Register and manage account  
- Access learning materials published by teachers  
- Solve assignments, quizzes, and practice tests  
- View explanations, diagrams, and solutions  
- Track learning progress and performance analytics  

### System Admin
- Manage user accounts and permissions  
- Configure AI engine settings  
- Monitor system logs and performance  
- Manage templates and platform content  

---

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher  
- Maven 3.8+  
- PostgreSQL  
- MongoDB  

### Run Locally

```bash
mvn clean install
mvn spring-boot:run
⚙️ Environment Configuration
Example application-dev.yml:

yaml
Copy code
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mathmaster
    username: ${DB_USER}
    password: ${DB_PASSWORD}
Sensitive information must be provided via environment variables or GitHub Secrets.

📘 API Documentation
Swagger UI:

bash
Copy code
http://localhost:8080/swagger-ui.html
🧪 Testing
Unit Tests: JUnit 5

Integration Tests: Spring Boot Test

bash
Copy code
mvn test
🔄 CI/CD
GitHub Actions runs automatically on Pull Requests:

Build

Test

