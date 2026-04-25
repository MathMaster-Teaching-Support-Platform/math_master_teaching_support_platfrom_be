## 1. Scope of Testing

### 1.1 In scope

- Below are the functional and non-functional requirements described in the Software Requirement Specifications that will be tested:
  | Module Name | Applicable Role | Description |
  |---|---|---|
  | Authentication | Student, Teacher, Admin | Verify that users can authenticate using valid credentials including registration, login, Google OAuth, email verification, password recovery, token refresh, and role selection. |
  | User Management | Admin | Verify the Admin can create, view, manage user accounts, roles, permissions, account status operations, password resets, and export user data. |
  | Teacher Profile Verification | Student, Admin | Verify that Students can submit teacher profile applications with documents and Admins can review, approve, or reject profiles with OCR verification. |
  | Manage Course | Teacher, Admin | Verify the Teacher can create, edit, update, publish courses and Admin can review, approve, or reject course submissions. |
  | Manage Course Enrollment | Student | Verify the Student can enroll in courses, drop enrollments, and view their enrolled courses. |
  | Manage Lesson Progress | Student | Verify the Student can track lesson progress, mark lessons as complete, and update watched video duration. |
  | Manage Course Reviews | Student, Teacher | Verify the Student can submit, update, and delete course reviews and Teacher can reply to reviews. |
  | Manage Lesson Plan | Teacher | Verify the Teacher can create, edit, view, and delete lesson plans for specific lessons. |
  | Manage Lesson Slides | Teacher | Verify the Teacher can generate lesson slides with AI, manage slide templates, publish/unpublish slides, and export slides as PPTX or PDF. |
  | View Lesson Slides | Student | Verify the Student can view published lesson slides, download generated slides, and preview slides as PDF. |
  | Manage Mindmap | Teacher | Verify the Teacher can create mindmaps manually or generate with AI, edit nodes, publish/archive mindmaps, and export as image or PDF. |
  | View Mindmap | Student | Verify the Student can view published mindmaps, browse mindmap nodes, and export mindmaps as image or PDF. |
  | Manage Exam Matrix | Teacher, Admin | Verify the Teacher can create exam matrices, build matrix structure, validate coverage, approve matrices, and export as PDF. |
  | Manage Question Template | Teacher, Admin | Verify the Teacher can create question templates with parameters, test templates with AI, publish/unpublish templates, and import templates from files or Excel. |
  | Manage Question Bank | Teacher, Admin | Verify the Teacher can create question banks, organize questions by topic, map templates to banks, and toggle public/private visibility. |
  | Manage Assessment | Teacher, Admin | Verify the Teacher can create, edit, publish, clone assessments with exam-matrix-based generation and question bank management. |
  | Take Assessment | Student | Verify the Student can start assessments, save answers, flag questions, view draft snapshots, submit assessments, and save progress. |
  | Manage Grading | Teacher, Admin | Verify the Teacher can manually grade submissions, override grades, respond to regrade requests, release grades, export analytics, and trigger AI review. |
  | Request Regrade | Student | Verify the Student can request regrade for specific questions and view regrade request status. |
  | Render Diagram | Student, Teacher, Admin | Verify users can render LaTeX diagrams with caching support for mathematical formulas and expressions. |
  | Manage Wallet | Student, Teacher, Admin | Verify users can view wallet balance, create wallets, and view transaction history with status filtering. |
  | Manage Payment | Student, Teacher, Admin | Verify users can create deposit payments via PayOS, poll order status, and receive webhook notifications. |
  | Manage Subscription | Student, Teacher, Admin | Verify users can view available subscription plans, purchase plans with wallet balance, and view active subscriptions. |
  | Manage Subscription Plans | Admin | Verify the Admin can create, edit, activate/deactivate subscription plans with pricing and token allocation. |
  | Manage Notifications | Student, Teacher, Admin | Verify users can receive in-app notifications, mark as read, view unread count, register push tokens, and manage notification preferences. |
  | Manage Learning Roadmap | Student, Admin | Verify the Student can browse roadmaps, take entry tests, view topic materials, submit feedback and Admin can manage roadmap content. |
  | View Student Dashboard | Student | Verify the Student can view personalized learning statistics, upcoming tasks, recent grades, weekly activity, and study streaks. |
  | View Teacher Earnings | Teacher | Verify the Teacher can view earnings statistics, monthly revenue breakdown, top performing courses, and transaction history. |
  | View Admin Dashboard | Admin | Verify the Admin can view platform statistics, monthly revenue, transaction management, system health monitoring, and export data. |

### 1.2 Out of scope

All of these will not be tested because they are NOT included in the Software Requirement Specification:

- User interfaces
- Hardware Interfaces.
- Software Interfaces.
- Database logic.
- Communications Interfaces.
- Website Security and Performance.

## 2. Test Strategy

In our MathMaster system, we employ System Testing as part of our testing strategy. This approach aims to ensure the reliability of the system and meet user expectations.

### 2.1 Testing Types

- **Objective**: To test the functions of the application and ensure that the end result meets the business and user requirements.
- **Test Type**: Functional Testing
- **Test Levels**: Unit, Integration, System, and Acceptance testing.
- **Completion criteria**: 
  - **Unit testing**: The testing of individual functional components and methods in isolation.
  - **Integration testing**: The testing performed on interfaces, interactions between components, modules, and external services.
  - **System testing**: The process of testing the complete integrated system to verify that it meets specified requirements.
  - **Acceptance testing**: The last phase of functional testing before the application is made available to the end user, validating business requirements.

### 2.2 Test Levels

| Test Type       | Unit | Integration | System | Acceptance |
| --------------- | ---- | ----------- | ------ | ---------- |
| Functional Test | X    | X           | X      | X          |

### 2.3 Supporting Tools

| Purpose                  | Tool                      | Vendor/In-house | Version |
| ------------------------ | ------------------------- | --------------- | ------- |
| Unit Testing Framework   | JUnit                     | JUnit Team      | 5.10    |
| Mocking Framework        | Mockito                   | Mockito         | 5.8     |
| API Testing              | Postman                   | Postman         | 10.20   |
| Integration Testing      | Spring Boot Test          | Spring          | 3.2     |
| Database Testing         | H2 Database (in-memory)   | H2              | 2.2     |
| Test Coverage            | JaCoCo                    | JaCoCo          | 0.8.11  |
| IDE                      | Visual Studio Code        | Microsoft       | 1.85    |

## 3. Test Plan

### 3.1 Human Resources

| Worker/Doer               | Role   | Specific Responsibilities/Comments                           |
| ------------------------- | ------ | ------------------------------------------------------------ |
| Phạm Đăng Khôi            | Leader | Perform functional and UI testing according to the test plan |
| Nguyễn Sơn Nam            | Member | Perform functional and UI testing according to the test plan |
| Nguyễn Hoàng Đức Minh     | Member | Perform functional and UI testing according to the test plan |
| Phan Quang Huy            | Member | Perform functional and UI testing according to the test plan |

### 3.2 Test Environment

| Purpose           | Tool          | Provider  | Version        |
| ----------------- | ------------- | --------- | -------------- |
| Run testing tools | Windows       | Microsoft | 8 or above     |
| Web testing       | Google Chrome | Google    | 119.0.6045.200 |

### 3.3 Test Milestones

| Milestone Task                    | Start Date | End Date   |
| --------------------------------- | ---------- | ---------- |
| Authentication                    | 15/01/2026 | 18/01/2026 |
| User Management                   | 19/01/2026 | 22/01/2026 |
| Teacher Profile Verification      | 23/01/2026 | 27/01/2026 |
| Manage Course                     | 28/01/2026 | 02/02/2026 |
| Manage Course Enrollment          | 03/02/2026 | 05/02/2026 |
| Manage Lesson Progress            | 06/02/2026 | 08/02/2026 |
| Manage Course Reviews             | 09/02/2026 | 11/02/2026 |
| Manage Lesson Plan                | 12/02/2026 | 15/02/2026 |
| Manage Lesson Slides              | 16/02/2026 | 20/02/2026 |
| View Lesson Slides                | 21/02/2026 | 23/02/2026 |
| Manage Mindmap                    | 24/02/2026 | 28/02/2026 |
| View Mindmap                      | 01/03/2026 | 03/03/2026 |
| Manage Exam Matrix                | 04/03/2026 | 08/03/2026 |
| Manage Question Template          | 09/03/2026 | 13/03/2026 |
| Manage Question Bank              | 14/03/2026 | 17/03/2026 |
| Manage Assessment                 | 18/03/2026 | 22/03/2026 |
| Take Assessment                   | 23/03/2026 | 26/03/2026 |
| Manage Grading                    | 27/03/2026 | 31/03/2026 |
| Request Regrade                   | 01/04/2026 | 03/04/2026 |
| Render Diagram                    | 04/04/2026 | 05/04/2026 |
| Manage Wallet                     | 06/04/2026 | 08/04/2026 |
| Manage Payment                    | 09/04/2026 | 11/04/2026 |
| Manage Subscription               | 12/04/2026 | 14/04/2026 |
| Manage Subscription Plans         | 15/04/2026 | 16/04/2026 |
| Manage Notifications              | 17/04/2026 | 19/04/2026 |
| Manage Learning Roadmap           | 20/04/2026 | 22/04/2026 |
| View Student Dashboard            | 15/04/2026 | 17/04/2026 |
| View Teacher Earnings             | 18/04/2026 | 20/04/2026 |
| View Admin Dashboard              | 21/04/2026 | 22/04/2026 |
