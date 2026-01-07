# MathMaster Teaching Support Platform - Backend

A comprehensive, enterprise-grade backend solution for the MathMaster Teaching Support Platform, designed to deliver robust, scalable, and secure mathematics education services using a monolithic architecture.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Development](#development)
- [Testing](#testing)
- [Deployment](#deployment)
- [Contributing](#contributing)
- [License](#license)
- [Support](#support)

## Overview

MathMaster Teaching Support Platform Backend is a sophisticated, monolithic backend infrastructure designed to support educators and students with advanced mathematics tutoring, assessment, and learning analytics capabilities. Built with enterprise-grade standards for security, performance, and reliability. 

**Version:** 1.0.0  
**Last Updated:** 2026-01-07

## Features

### Core Capabilities
- **User Management**: Comprehensive authentication, authorization, and role-based access control (RBAC)
- **Content Delivery**:  Scalable mathematical content and curriculum management
- **Assessment Engine**: Advanced quiz and exam creation with intelligent evaluation
- **Learning Analytics**: Real-time performance tracking and insights for educators
- **Real-time Collaboration**: WebSocket-based interactive tutoring sessions
- **Progress Tracking**: Granular student progress monitoring and reporting
- **Notification System**:  Multi-channel notifications for events and milestones

### Security
- JWT-based authentication with refresh token rotation
- End-to-end encryption for sensitive data
- Rate limiting and DDoS protection
- Comprehensive audit logging
- GDPR and data privacy compliance

### Performance & Scalability
- Optimized monolithic architecture for high performance
- Redis caching layer for optimal performance
- Database connection pooling and optimization
- CDN-ready content delivery
- Load balancing support

## Technology Stack

### Backend Framework
- **Runtime**: Java 17+
- **Framework**: Spring Boot 3.x
- **Build Tool**: Maven / Gradle
- **Language**: Java

### Data & Storage
- **Primary Database**: PostgreSQL 13+
- **Cache Layer**: Redis 6+
- **File Storage**: AWS S3 / MinIO
- **Search**:  Elasticsearch (optional, for advanced analytics)

### Infrastructure & DevOps
- **Containerization**: Docker
- **CI/CD Pipeline**: GitHub Actions / Jenkins
- **Monitoring**: Prometheus + Grafana
- **Logging**: ELK Stack / CloudWatch

### Development Tools
- **Version Control**: Git
- **Package Manager**: Maven / Gradle
- **Testing Framework**: JUnit 5 / Mockito / TestNG
- **API Documentation**: Springdoc OpenAPI (Swagger)
- **Code Quality**: SonarQube / Checkstyle

## Architecture

### Monolithic Architecture
The project is designed following a **Monolithic** architecture with clear layered separation: 

```
┌─────────────────────────────────────────────────┐
│        Presentation Layer                       │
│   (REST Controllers / WebSocket Handlers)       │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│      Business Logic Layer (Services)            │
│  ├─ User Service                                │
│  ├─ Content Service                             │
│  ├─ Assessment Service                          │
│  ├─ Analytics Service                           │
│  └─ Notification Service                        │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│    Data Access Layer (Repositories)             │
│         (JPA Repositories)                      │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│           Database Layer                        │
│          (PostgreSQL)                           │
└─────────────────────────────────────────────────┘

Shared Components (across all layers):
├─ Security (JWT, RBAC)
├─ Caching (Redis)
├─ File Storage (S3/MinIO)
└─ Utilities & Helpers
```

### Benefits of Monolithic Architecture
- ✅ Simple deployment and maintenance
- ✅ Easy debugging and development
- ✅ High performance for moderate traffic
- ✅ Easy data sharing between modules
- ✅ Strong transaction management support
- ✅ Simplified dependency management

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.8+ or Gradle 7+
- PostgreSQL 13+
- Redis 6+
- Docker & Docker Compose (for containerized setup)
- Git

### Installation

1. **Clone the Repository**
   ```bash
   git clone https://github.com/MathMaster-Teaching-Support-Platform/math_master_teaching_support_platfrom_be.git
   cd math_master_teaching_support_platfrom_be
   ```

2. **Install Dependencies**
   ```bash
   # Using Maven
   mvn clean install
   
   # Or using Gradle
   gradle build
   ```

3. **Set Up Environment**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

4. **Database Setup**
   ```bash
   # Flyway will automatically run migrations on application startup
   # Or run manually:  
   mvn flyway:migrate
   ```

5. **Start Development Server**
   ```bash
   mvn spring-boot:run
   
   # Or using Gradle
   gradle bootRun
   ```

The backend will be available at `http://localhost:8080` (or configured port)

### Docker Setup
```bash
docker-compose up -d
```

## Configuration

### Environment Variables

Create a `.env` file in the project root with the following variables:

```env
# Server Configuration
SPRING_APPLICATION_NAME=mathmaster-backend
SERVER_PORT=8080
SERVER_SERVLET_CONTEXT_PATH=/api

# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mathmaster_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=secure_password
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SPRING_JPA_SHOW_SQL=false

# Redis Configuration
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=redis_password

# Authentication
JWT_SECRET=your_jwt_secret_key_here_min_32_characters_long
JWT_EXPIRY=3600000
JWT_REFRESH_EXPIRY=604800000

# AWS S3 / File Storage
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
S3_BUCKET_NAME=mathmaster-content
S3_ENDPOINT=https://s3.amazonaws.com

# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=notifications@mathmaster.com
MAIL_PASSWORD=email_password
MAIL_FROM=noreply@mathmaster.com

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_MATHMASTER=DEBUG
LOGGING_PATTERN_CONSOLE=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

### Spring Boot Configuration
Edit `application.properties` or `application.yml`:

```yaml
spring:
  application:
    name: mathmaster-backend
  datasource:
    url: jdbc: postgresql://localhost:5432/mathmaster_db
    username: postgres
    password: secure_password
  jpa:
    hibernate: 
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  jackson:
    default-property-inclusion: non_null
    serialization: 
      write-dates-as-timestamps: false
  redis:
    host: localhost
    port: 6379

server:
  port: 8080
  servlet:
    context-path: /api

logging:
  level:
    root: INFO
    com.mathmaster: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"

management: 
  endpoints:
    web: 
      exposure:
        include: health,info,metrics,prometheus
```

## API Documentation

### Authentication Endpoints
- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/refresh` - Refresh access token
- `POST /api/v1/auth/logout` - User logout

### User Endpoints
- `GET /api/v1/users/{id}` - Get user profile
- `PUT /api/v1/users/{id}` - Update user profile
- `GET /api/v1/users/{id}/progress` - Get user learning progress
- `GET /api/v1/users` - List all users (admin only)

### Content Endpoints
- `GET /api/v1/content` - List all content
- `GET /api/v1/content/{id}` - Get content details
- `POST /api/v1/content` - Create new content (admin)
- `PUT /api/v1/content/{id}` - Update content (admin)
- `DELETE /api/v1/content/{id}` - Delete content (admin)

### Assessment Endpoints
- `GET /api/v1/assessments` - List assessments
- `GET /api/v1/assessments/{id}` - Get assessment details
- `POST /api/v1/assessments/{id}/submit` - Submit assessment
- `GET /api/v1/assessments/{id}/results` - Get assessment results

For comprehensive API documentation, visit `/api/v1/swagger-ui.html` when the server is running or see [API_DOCS.md](docs/API_DOCS.md).

## Development

### Project Structure
```
src/
├── main/
│   ├── java/com/mathmaster/
│   │   ├── config/              # Spring Configuration (Security, Database, etc.)
│   │   ├── controller/          # REST Controllers
│   │   ├── service/             # Business Logic / Services
│   │   ├── repository/          # Data Access Layer (JPA Repositories)
│   │   ├── entity/              # JPA Entities / Domain Models
│   │   ├── dto/                 # Data Transfer Objects
│   │   ├── exception/           # Custom Exceptions
│   │   ├── security/            # Security Configuration (JWT, etc.)
│   │   ├── util/                # Utility Classes
│   │   ├── validator/           # Input Validation
│   │   ├── aspect/              # AOP Aspects
│   │   └── MathMasterApplication. java
│   └── resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-prod.yml
│       └── db/migration/        # Flyway Migrations
└── test/
    └── java/com/mathmaster/
        ├── controller/          # Controller Tests
        ├── service/             # Service Tests
        └── integration/         # Integration Tests
```

### Code Style Guide
- Follow Google Java Style Guide
- Use Checkstyle for code linting
- Use Spotless for automatic code formatting
- Write meaningful commit messages
- Follow Clean Code principles

### Running Locally
```bash
# Development server with hot reload
mvn spring-boot:run

# Or using Gradle
gradle bootRun

# Run checkstyle
mvn checkstyle:check

# Fix formatting issues
mvn spotless:apply

# Run SonarQube analysis
mvn sonar:sonar
```

## Testing

### Running Tests
```bash
# Run all tests
mvn test

# Run tests with coverage report
mvn test jacoco:report

# Run tests in debug mode
mvn test -Dmaven.surefire.debug

# Run integration tests
mvn verify -Pit

# Run specific test class
mvn test -Dtest=UserServiceTest
```

### Test Coverage
- Target: 80%+ code coverage
- Unit tests for all services and repositories
- Integration tests for all API endpoints
- E2E tests for critical workflows
- Mock external dependencies appropriately

### Testing Tools
- **JUnit 5**: Testing framework
- **Mockito**:  Mocking framework
- **Spring Boot Test**: Testing support
- **TestContainers**: Container-based testing
- **REST Assured**: REST API testing
- **JaCoCo**: Code coverage

## Deployment

### Production Build
```bash
mvn clean package -DskipTests

# Or using Gradle
gradle build -x test
```

### Docker Production Build
```bash
docker build -f Dockerfile -t mathmaster-backend: 1.0.0 .
docker push your-registry/mathmaster-backend: 1.0.0
```

### Docker Compose
```bash
# Development
docker-compose -f docker-compose.yml up -d

# Production
docker-compose -f docker-compose. prod.yml up -d
```

### Kubernetes Deployment
```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml
```

### Environment-Specific Configurations
- **Development**: Local database with debug logging enabled
- **Staging**: Production-like environment with test data
- **Production**:  Optimized settings with security hardening and monitoring

### Health Checks & Monitoring
- Health endpoint: `GET /api/actuator/health`
- Readiness endpoint: `GET /api/actuator/health/readiness`
- Liveness endpoint: `GET /api/actuator/health/liveness`
- Metrics endpoint: `GET /api/actuator/metrics`
- Prometheus metrics: `GET /api/actuator/prometheus`

## Contributing

We welcome contributions from the community.  Please follow these guidelines:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/your-feature`)
3. **Commit** your changes (`git commit -am 'Add feature description'`)
4. **Push** to the branch (`git push origin feature/your-feature`)
5. **Submit** a Pull Request with a clear description

### Code Standards
- Write clean, maintainable, and well-documented code
- Add comprehensive tests for new features
- Update documentation accordingly
- Follow the existing code style and conventions
- Ensure all tests pass and coverage requirements are met

### Pull Request Process
1. Update README.md with any new features or changes
2. Ensure code passes all tests and linting checks
3. Provide clear PR description explaining the changes
4. Request review from maintainers
5. Address review comments promptly

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

### Documentation
- [Architecture Guide](docs/ARCHITECTURE.md)
- [API Reference](docs/API_REFERENCE.md)
- [Setup Guide](docs/SETUP_GUIDE.md)
- [Troubleshooting](docs/TROUBLESHOOTING.md)
- [Contributing Guidelines](CONTRIBUTING.md)
- [Developer Guide](docs/DEVELOPER_GUIDE.md)

### Getting Help
- **Email**: support@mathmaster.com
- **Issues**: [GitHub Issues](https://github.com/MathMaster-Teaching-Support-Platform/math_master_teaching_support_platfrom_be/issues)
- **Discussions**: [GitHub Discussions](https://github.com/MathMaster-Teaching-Support-Platform/math_master_teaching_support_platfrom_be/discussions)
- **Documentation**: [https://docs.mathmaster.com](https://docs.mathmaster.com)

### Reporting Issues
Please use GitHub Issues to report bugs and feature requests. For security vulnerabilities, please report privately to security@mathmaster.com

---

**Maintained by:** MathMaster Teaching Support Platform Team  
**Last Updated:** 2026-01-07  
**Status:** Active Development  
**Java Version:** 17+  
**Spring Boot Version:** 3.x
