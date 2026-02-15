# Receipt Scanner & AI Budget Predictor - Technical Handbook

## Table of Contents
1. [Project Overview](#1-project-overview)
2. [Architecture & Tech Stack](#2-architecture--tech-stack)
3. [Backend Architecture](#3-backend-architecture)
4. [Frontend Architecture](#4-frontend-architecture)
5. [Database Design](#5-database-design)
6. [Authentication & Security](#6-authentication--security)
7. [Receipt Processing Pipeline](#7-receipt-processing-pipeline)
8. [AI Integration (Google Gemini)](#8-ai-integration-google-gemini)
9. [API Reference](#9-api-reference)
10. [Data Flow & State Management](#10-data-flow--state-management)
11. [Design Patterns & Decisions](#11-design-patterns--decisions)
12. [Interview Q&A Guide](#12-interview-qa-guide)

---

## 1. Project Overview

A full-stack personal finance application that uses OCR and AI to automatically scan receipts, categorize expenses, track budgets, and predict spending patterns.

**Key Capabilities:**
- Receipt scanning via Tesseract OCR + Google Gemini Vision (multimodal)
- AI-powered receipt parsing with three-tier fallback strategy
- Budget management with real-time alerts and AI-generated savings suggestions
- Analytics dashboard with D3.js visualizations (line, bar, pie, diverging charts)
- Recurring expense tracking with automated transaction generation
- AI chat assistant for financial guidance
- Income tracking and savings analysis

---

## 2. Architecture & Tech Stack

```
┌──────────────────────────────────────────────────────────┐
│                    FRONTEND (Angular 21)                  │
│  Components → Services → HTTP Client → Auth Interceptor  │
│  D3.js Charts │ Angular Material │ Reactive Forms         │
└──────────────────────┬───────────────────────────────────┘
                       │ REST API (JSON over HTTPS)
                       │ JWT Bearer Token Auth
┌──────────────────────┴───────────────────────────────────┐
│                 BACKEND (Spring Boot 3.5)                 │
│  Controllers → Services → Repositories → PostgreSQL      │
│  Security Filter Chain │ Scheduled Tasks                  │
├──────────────────────────────────────────────────────────┤
│  EXTERNAL SERVICES                                        │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────┐  │
│  │ Cloudinary   │  │ Google       │  │ Tesseract OCR  │  │
│  │ (Image CDN)  │  │ Gemini AI    │  │ (On-premise)   │  │
│  └─────────────┘  └──────────────┘  └────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

| Layer | Technology | Version |
|-------|-----------|---------|
| Frontend Framework | Angular (Standalone Components) | 21.1 |
| UI Library | Angular Material + CDK | 21.1 |
| Charting | D3.js | 7.9 |
| Backend Framework | Spring Boot | 3.5.10 |
| Language | Java | 21 |
| Database | PostgreSQL | 16+ |
| ORM | Spring Data JPA (Hibernate) | via Spring Boot |
| Migrations | Flyway | via Spring Boot |
| Auth | JWT (jjwt) | 0.12.3 |
| OCR Engine | Tesseract (tess4j) | 5.9 |
| AI/LLM | Google Gemini 2.0 Flash | API |
| Image Storage | Cloudinary | 1.36 |
| Build (Backend) | Gradle | 8.11 |
| Build (Frontend) | Angular CLI + Vite | 21.1 |

---

## 3. Backend Architecture

### 3.1 Project Structure

```
backend/src/main/java/com/receiptscan/
├── config/
│   ├── SecurityConfig.java          # CORS, CSRF, filter chain, auth
│   └── CloudinaryConfig.java       # Cloudinary bean
├── controller/
│   ├── AuthController.java          # /api/auth
│   ├── TransactionController.java   # /api/transactions
│   ├── BudgetController.java        # /api/budgets
│   ├── BudgetAlertController.java   # /api/alerts
│   ├── CategoryController.java      # /api/categories
│   ├── CustomCategoryController.java# /api/categories/custom
│   ├── IncomeController.java        # /api/income
│   ├── RecurringExpenseController.java # /api/recurring
│   ├── AnalyticsController.java     # /api/analytics
│   ├── NotificationController.java  # /api/notifications
│   ├── AIChatController.java        # /api/chat
│   └── HealthController.java        # /api/health
├── service/
│   ├── AuthService.java
│   ├── TransactionService.java      # Core business logic + receipt parsing
│   ├── BudgetService.java
│   ├── BudgetAlertService.java      # Alert generation + AI suggestions
│   ├── CategoryService.java
│   ├── CustomCategoryService.java
│   ├── IncomeService.java
│   ├── RecurringExpenseService.java
│   ├── NotificationService.java
│   ├── AnalyticsService.java
│   ├── AIChatService.java
│   ├── GeminiReceiptParserService.java  # Gemini API integration
│   ├── GeminiBudgetAdvisorService.java  # AI budget suggestions
│   ├── OcrService.java                  # Tesseract OCR
│   ├── ImageStorageService.java         # Cloudinary upload
│   └── ScheduledTaskService.java        # Cron jobs
├── entity/
│   ├── User.java (implements UserDetails)
│   ├── Transaction.java
│   ├── Budget.java
│   ├── BudgetAlert.java
│   ├── CustomCategory.java
│   ├── PredefinedCategory.java
│   ├── IncomeSource.java
│   ├── RecurringExpense.java
│   ├── ChatMessage.java
│   └── AppNotification.java
├── repository/           # Spring Data JPA interfaces
├── dto/                  # Request/Response objects
├── security/
│   ├── JwtUtil.java
│   ├── JwtAuthenticationFilter.java
│   └── UserDetailsServiceImpl.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── NotFoundException.java
│   └── BadRequestException.java
└── scheduler/
    └── RecurringExpenseScheduler.java
```

### 3.2 Key Configuration

```properties
# application.properties
server.port=8080
spring.datasource.url=jdbc:postgresql://localhost:5432/receipt_scanner
spring.jpa.hibernate.ddl-auto=validate    # Schema via Flyway only
spring.jpa.open-in-view=false             # Prevent lazy loading outside transactions
spring.servlet.multipart.max-file-size=10MB
gemini.api.model=gemini-2.0-flash
jwt.expiration=86400000                   # 24 hours
```

### 3.3 Scheduled Tasks

| Schedule | Task | Description |
|----------|------|-------------|
| Daily 1:00 AM | `processDueExpenses()` | Auto-creates transactions for due recurring expenses |
| Daily 9:00 AM | `sendBillReminders()` | Sends notifications for upcoming bills |
| Daily 6:00 PM | `dailyBudgetCheck()` | Creates CRITICAL alerts for exceeded budgets |
| Sunday 8:00 PM | `generateWeeklySummaries()` | Weekly budget summary alerts |
| 1st of month 9:00 AM | `generateMonthlySummaries()` | Monthly budget summary alerts |

---

## 4. Frontend Architecture

### 4.1 Project Structure

```
frontend/src/app/
├── components/
│   ├── landing/                  # Public landing page
│   ├── login/                    # Login form
│   ├── register/                 # Registration with password validation
│   ├── auth-dialog/              # Modal auth (used from landing)
│   ├── analytics-dashboard/      # D3.js charts + financial overview
│   ├── receipt-list/             # Transaction list grouped by month
│   ├── receipt-detail/           # View/edit single transaction
│   ├── manual-receipt-form/      # Manual entry + receipt upload
│   ├── income-settings/          # Income CRUD grouped by month
│   └── ai-chat/                  # Floating AI chat widget
├── services/
│   ├── auth.service.ts           # Login/register/token management
│   ├── transaction.service.ts    # CRUD + receipt processing
│   ├── analytics.service.ts      # Chart data endpoints
│   ├── income.service.ts         # Income CRUD
│   ├── category.service.ts       # Category lookups
│   └── chat.service.ts           # AI chat messaging
├── models/
│   ├── auth.model.ts
│   ├── transaction.model.ts
│   ├── income.model.ts
│   └── category.model.ts
├── guards/
│   └── auth.guard.ts             # CanActivateFn, redirects to /login
├── interceptors/
│   └── auth.interceptor.ts       # Adds Bearer token to all requests
├── app.ts                        # Root component with nav toolbar
├── app.routes.ts                 # Route definitions
└── app.config.ts                 # Providers (router, http, interceptors)
```

### 4.2 Routing

| Path | Component | Auth Required |
|------|-----------|:---:|
| `/` | LandingComponent | No |
| `/login` | LoginComponent | No |
| `/register` | RegisterComponent | No |
| `/dashboard` | AnalyticsDashboardComponent | Yes |
| `/transactions` | ReceiptListComponent | Yes |
| `/transactions/create` | ManualReceiptForm | Yes |
| `/transactions/:id` | ReceiptDetail | Yes |
| `/income` | IncomeSettingsComponent | Yes |

### 4.3 D3.js Chart Implementations

| Chart | Type | Data Source | Location |
|-------|------|-------------|----------|
| Weekly Spending | Line chart (curveMonotoneX) | `/api/analytics/weekly-spending` | Dashboard |
| Monthly Spending | Bar chart | `/api/analytics/monthly-spending` | Dashboard |
| Category Breakdown | Pie chart | `/api/analytics/category-breakdown` | Dashboard |
| Monthly Income | Bar chart | Income service aggregation | Dashboard |
| Monthly Savings | Diverging bar chart | Income - Expenses per month | Dashboard |

**D3 Scales Used:** `scaleBand` (categorical X), `scaleLinear` (numeric Y), `scaleOrdinal` (colors)

### 4.4 Key Angular Patterns

- **Standalone Components** — no NgModules, each component declares its own imports
- **Reactive Forms** — FormGroup/FormArray with validators for all forms
- **Functional Guards** — `CanActivateFn` (modern Angular guard pattern)
- **Functional Interceptors** — `HttpInterceptorFn` for JWT token injection
- **BehaviorSubject** — AuthService exposes `currentUser$` observable
- **provideHttpClient(withInterceptors([...]))** — modern HTTP setup in app.config

---

## 5. Database Design

### 5.1 Entity Relationship Diagram

```
users (1) ──→ (M) transactions (receipts table)
users (1) ──→ (M) budgets
users (1) ──→ (M) budget_alerts ←── (M) budgets
users (1) ──→ (M) custom_categories
users (1) ──→ (M) income_sources
users (1) ──→ (M) recurring_expenses ──→ (M) transactions
users (1) ──→ (M) chat_messages
users (1) ──→ (M) app_notifications
predefined_categories (standalone lookup table)
```

### 5.2 Key Tables

**users**
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGSERIAL | PK |
| email | VARCHAR | UNIQUE, NOT NULL |
| password_hash | VARCHAR | NOT NULL |
| full_name | VARCHAR | |
| role | VARCHAR | DEFAULT 'ROLE_USER' |
| created_at | TIMESTAMP | |

**receipts (transactions)**
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGSERIAL | PK |
| user_id | BIGINT | FK → users, NOT NULL |
| image_url | TEXT | Nullable (manual entries) |
| raw_text | TEXT | OCR output |
| merchant_name | VARCHAR | |
| amount | DECIMAL(10,2) | NOT NULL |
| transaction_date | DATE | NOT NULL |
| category | VARCHAR(50) | NOT NULL |
| items | JSONB | Line items array |
| confidence_score | DECIMAL(3,2) | 0.0-1.0 |
| payment_method | VARCHAR(50) | |
| is_recurring | BOOLEAN | DEFAULT false |
| recurring_expense_id | BIGINT | FK → recurring_expenses |

**budgets**
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGSERIAL | PK |
| user_id | BIGINT | FK → users |
| category | VARCHAR(100) | |
| limit_amount | DECIMAL(12,2) | |
| limit_type | VARCHAR(20) | PERCENTAGE or DOLLAR |
| period_type | VARCHAR(20) | WEEKLY, MONTHLY, YEARLY |
| is_active | BOOLEAN | DEFAULT true |
| UNIQUE | | (user_id, category, period_type) |

**budget_alerts**
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGSERIAL | PK |
| user_id | BIGINT | FK → users |
| budget_id | BIGINT | FK → budgets |
| alert_type | VARCHAR(50) | REAL_TIME, WEEKLY_SUMMARY, etc. |
| severity | VARCHAR(20) | INFO, WARNING, CRITICAL |
| category | VARCHAR(100) | |
| current_spending | DECIMAL(12,2) | |
| budget_limit | DECIMAL(12,2) | |
| percentage_used | DECIMAL(5,2) | |
| message | TEXT | |
| ai_suggestions | JSONB | AI-generated recommendations |
| is_read | BOOLEAN | DEFAULT false |

### 5.3 Migration History (Flyway)

| Version | Purpose |
|---------|---------|
| V1 | Initial schema — users, receipts |
| V3 | Make image_url nullable, add quantity to items |
| V4 | Add role column to users |
| V5 | Budget management — custom_categories, budgets, budget_alerts |
| V6 | Chat messages table |
| V7 | Income sources, recurring expenses, predefined categories, notifications |
| V8 | Simplify income tracking |
| V9 | Add "Other" predefined category |

### 5.4 Indexing Strategy

```sql
-- Transaction performance indexes
idx_receipts_user_id           (user_id)
idx_receipts_transaction_date  (transaction_date)
idx_receipts_category          (category)
idx_receipts_user_date         (user_id, transaction_date)
idx_receipts_user_category_date (user_id, category, transaction_date)

-- Alert performance indexes
idx_budget_alerts_user_id      (user_id)
idx_budget_alerts_unread       (is_read)
idx_budget_alerts_created_at   (created_at)
```

---

## 6. Authentication & Security

### 6.1 Authentication Flow

```
Registration:
  Client → POST /api/auth/register {email, password, fullName}
  Server → BCrypt hash password → Save User → Generate JWT → Return {token, user}
  Client → Store token in localStorage → Redirect to /dashboard

Login:
  Client → POST /api/auth/login {email, password}
  Server → AuthenticationManager.authenticate() → Generate JWT → Return {token, user}
  Client → Store token in localStorage → Redirect to returnUrl

Authenticated Request:
  Client → authInterceptor clones request, adds "Authorization: Bearer {token}"
  Server → JwtAuthenticationFilter extracts token → JwtUtil validates → Load UserDetails
         → Set SecurityContext → Controller receives @AuthenticationPrincipal User
```

### 6.2 JWT Implementation

- **Algorithm:** HMAC SHA-256 (HS256)
- **Secret:** 256-bit key from environment variable
- **Expiration:** 24 hours
- **Claims:** `sub` (email), `userId` (custom), `iat`, `exp`
- **Library:** io.jsonwebtoken (jjwt) 0.12.3

### 6.3 Security Configuration

```java
// SecurityConfig.java - Key decisions
http.csrf(csrf -> csrf.disable())                    // Stateless API, no CSRF needed
    .sessionManagement(s -> s.stateless())           // No server-side sessions
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/**").permitAll()  // Public auth endpoints
        .anyRequest().authenticated())                // Everything else requires JWT
    .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

// CORS: Only http://localhost:4200 allowed
// Methods: GET, POST, PUT, DELETE, OPTIONS
// Headers: Authorization, Content-Type
```

### 6.4 Password Requirements

```
Minimum 8 characters
At least 1 uppercase letter
At least 1 lowercase letter
At least 1 digit
At least 1 special character (@$!%*?&)
Regex: ^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$
```

---

## 7. Receipt Processing Pipeline

### 7.1 Three-Tier Parsing Strategy

```
Receipt Image Upload
        │
        ▼
┌─── TIER 1: Gemini Vision (Multimodal) ───┐
│ Send raw image bytes to Gemini API        │
│ Model analyzes image directly             │
│ Highest accuracy for structured receipts  │
│ Returns: ParsedReceipt with confidence    │
└──────────────┬──────────────┬────────────┘
          Success          Failure
               │               │
               ▼               ▼
        Save to DB    ┌─── TIER 2: OCR + AI Text ──────┐
                      │ 1. Download from Cloudinary      │
                      │ 2. Preprocess (scale/gray/binary)│
                      │ 3. Tesseract OCR text extraction │
                      │ 4. Send text to Gemini API       │
                      │ Returns: ParsedReceipt           │
                      └──────┬──────────────┬────────────┘
                        Success          Failure
                             │               │
                             ▼               ▼
                      Save to DB    ┌─── TIER 3: Regex Fallback ──┐
                                    │ Pattern matching:             │
                                    │ - Merchant: first cap line    │
                                    │ - Total: "Total: $X.XX"      │
                                    │ - Date: MM/DD/YYYY            │
                                    │ - Items: "Name    $X.XX"     │
                                    └──────────────┬───────────────┘
                                                   ▼
                                            Save to DB
                                         (flagged for review)
```

### 7.2 OCR Image Preprocessing

```java
// OcrService.preprocessImage()
1. Scale: If width < 1500px → upscale 2x (bicubic interpolation)
2. Grayscale: Convert to 8-bit grayscale (TYPE_BYTE_GRAY)
3. Binarize: Convert to binary black/white (TYPE_BYTE_BINARY)
// Result: High-contrast image optimized for text extraction
```

### 7.3 Gemini Vision Prompt

```
Extract structured JSON from this receipt image:
- merchantName, subtotal, tax, total
- transactionDate (YYYY-MM-DD)
- category (Food, Transport, Bills, Shopping, Entertainment, Health, Groceries, Utilities, Other)
- items: [{name, quantity, unitPrice, price}]
- confidenceScore: 0.0-1.0
- paymentMethod, address, phoneNumber
```

**Gemini Generation Config:** temperature=0.1, topP=0.95, topK=40, maxOutputTokens=2048

### 7.4 Quality Validation

After parsing, the system flags transactions for manual review when:
- `amount == 0` AND `merchant == "Unknown"` → EXTRACTION_FAILURE
- `amount == 0` OR `merchant == "Unknown"` → WARNING
- `confidenceScore < threshold` → needs review

---

## 8. AI Integration (Google Gemini)

### 8.1 Services Using Gemini

| Service | Use Case | Model |
|---------|----------|-------|
| GeminiReceiptParserService | Receipt image/text parsing | gemini-2.0-flash |
| GeminiBudgetAdvisorService | Budget savings suggestions | gemini-2.0-flash |
| AIChatService | Conversational financial assistant | gemini-2.0-flash |

### 8.2 Budget Alert AI Suggestions

When spending exceeds budget thresholds, the system:
1. Calculates current spending vs budget limit
2. Determines severity (INFO <80%, WARNING 80-100%, CRITICAL >100%)
3. Calls Gemini for personalized savings suggestions
4. Stores suggestions as JSONB: `[{title, description, potentialSavings, category}]`

### 8.3 AI Chat Context

The chat service:
- Sends user messages to Gemini with financial context
- Stores conversation history in `chat_messages` table
- Supports suggested quick questions (e.g., "How much did I spend this month?")
- Context data stored as JSONB for future enhancement

---

## 9. API Reference

### 9.1 Authentication

| Method | Endpoint | Body | Response |
|--------|----------|------|----------|
| POST | `/api/auth/register` | `{email, password, fullName}` | `{token, email, userId, fullName}` |
| POST | `/api/auth/login` | `{email, password}` | `{token, email, userId, fullName}` |
| GET | `/api/auth/me` | — | `{id, email, fullName, role}` |

### 9.2 Transactions

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/transactions` | Create manual transaction |
| POST | `/api/transactions/process` | Upload + OCR + AI parse receipt |
| POST | `/api/transactions/upload` | Upload image only |
| POST | `/api/transactions/upload-and-extract` | Upload + OCR text |
| GET | `/api/transactions` | List user transactions (?category, ?startDate, ?endDate) |
| GET | `/api/transactions/{id}` | Get single transaction |
| GET | `/api/transactions/stats` | Transaction statistics |
| PUT | `/api/transactions/{id}` | Update transaction |
| DELETE | `/api/transactions/{id}` | Delete transaction |
| POST | `/api/transactions/{id}/items` | Add line item |
| PUT | `/api/transactions/{id}/items/{idx}` | Update line item |
| DELETE | `/api/transactions/{id}/items/{idx}` | Delete line item |

### 9.3 Budgets & Alerts

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/budgets` | Create budget |
| GET | `/api/budgets` | List budgets (?activeOnly) |
| PUT | `/api/budgets/{id}` | Update budget |
| DELETE | `/api/budgets/{id}` | Delete budget |
| GET | `/api/alerts` | List alerts (?unreadOnly, ?alertType) |
| GET | `/api/alerts/unread-count` | Unread alert count |
| PUT | `/api/alerts/{id}/mark-read` | Mark alert read |
| POST | `/api/alerts/check/{category}` | Trigger budget check |

### 9.4 Analytics

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/analytics/weekly-spending` | Weekly totals (?weeks=4) |
| GET | `/api/analytics/monthly-spending` | Monthly totals (?months=6) |
| GET | `/api/analytics/category-breakdown` | By category (?period=MONTHLY) |
| GET | `/api/analytics/top-categories` | Top N categories (?limit=5) |
| GET | `/api/analytics/spending-trends` | Trends (?startDate, ?endDate) |

### 9.5 Other Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| CRUD | `/api/income` | Income source management |
| GET | `/api/income/total-monthly` | Total monthly income |
| CRUD | `/api/recurring` | Recurring expense management |
| GET | `/api/recurring/upcoming` | Upcoming expenses (?days) |
| CRUD | `/api/categories/custom` | Custom category management |
| GET | `/api/categories/all` | All categories |
| POST | `/api/chat/message` | Send AI chat message |
| GET | `/api/chat/history` | Chat history (?limit=50) |
| CRUD | `/api/notifications` | Notification management |

---

## 10. Data Flow & State Management

### 10.1 Frontend State

```
localStorage
├── token          → JWT string
└── currentUser    → Serialized User JSON

AuthService
└── currentUserSubject: BehaviorSubject<User | null>
    └── All components subscribe for auth state

Component State
├── Reactive Forms (FormGroup/FormArray)
├── Local properties (arrays, loading flags, error messages)
└── Sets for UI state (collapsedMonths accordion)
```

### 10.2 Request Flow

```
Component → Service.method()
         → HttpClient.get/post/put/delete()
         → authInterceptor adds Bearer token
         → Backend Controller
         → @AuthenticationPrincipal extracts user
         → Service layer business logic
         → Repository → PostgreSQL
         → Response DTO → JSON → Component
```

### 10.3 Budget Alert Flow

```
Transaction Created/Updated
  → TransactionService checks budget
  → BudgetAlertService.checkAndCreateAlert()
  → Calculate spending vs limit
  → If exceeded: determine severity
  → If WARNING/CRITICAL: call GeminiBudgetAdvisorService
  → Save BudgetAlert with AI suggestions
  → Create AppNotification
```

---

## 11. Design Patterns & Decisions

### 11.1 Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Repository Pattern** | Spring Data JPA interfaces | Abstracts database access, auto-generates queries |
| **DTO Pattern** | Request/Response objects | Decouples API contract from entity internals |
| **Strategy Pattern** | Three-tier receipt parsing | Fallback chain: Vision → OCR+AI → Regex |
| **Observer Pattern** | BehaviorSubject in AuthService | Components react to auth state changes |
| **Interceptor Pattern** | JWT auth interceptor | Cross-cutting concern (auth) applied to all requests |
| **Guard Pattern** | Angular route guards | Protects routes from unauthenticated access |
| **Builder Pattern** | Lombok @Builder on entities | Clean object construction |
| **Singleton Pattern** | Angular services (providedIn: 'root') | Shared state across components |

### 11.2 Key Technical Decisions

**Why PostgreSQL over MongoDB?**
- Relational data (users → transactions → items) fits naturally
- JSONB columns give document flexibility where needed (line items, AI suggestions)
- Strong ACID transactions for financial data
- Complex analytics queries (GROUP BY, date ranges, aggregations)

**Why Flyway over JPA auto-ddl?**
- Version-controlled schema changes
- Repeatable, auditable migrations
- `ddl-auto=validate` ensures entity/schema sync without auto-modification

**Why D3.js over Chart.js?**
- Full control over chart rendering and interactivity
- Custom diverging bar charts not easily available in Chart.js
- SVG-based output for crisp rendering at any resolution

**Why Standalone Components over NgModules?**
- Angular 21 best practice — no module boilerplate
- Each component is self-contained with explicit imports
- Tree-shakeable by default

**Why Gemini Vision + OCR Fallback?**
- Vision API handles complex receipt layouts directly
- OCR fallback ensures processing even if vision fails
- Regex as last resort guarantees some data extraction
- Confidence scoring flags uncertain results for review

**Why JSONB for line items?**
- Variable number of items per receipt
- No need to join a separate table for common queries
- Items always accessed with their parent transaction
- PostgreSQL JSONB supports indexing if needed

---

## 12. Interview Q&A Guide

### Architecture & Design

**Q: How does the three-tier receipt parsing work?**
A: Tier 1 sends the raw image to Gemini Vision API for multimodal analysis — it "sees" the receipt directly. If that fails, Tier 2 extracts text via Tesseract OCR (with preprocessing: upscaling, grayscale, binarization), then sends the text to Gemini for structured parsing. Tier 3 falls back to regex patterns for basic extraction. Each tier assigns a confidence score, and low-confidence results are flagged for manual review.

**Q: Why did you choose this architecture over a simpler monolith?**
A: It is a monolith — a Spring Boot backend with an Angular frontend. This is appropriate for a single-team project. The separation of concerns (controllers/services/repositories) and the REST API boundary between frontend and backend gives us the option to scale independently later without the operational complexity of microservices.

**Q: How do you handle data consistency for financial transactions?**
A: PostgreSQL provides ACID transactions. JPA's `@Transactional` annotation ensures that operations like "create transaction + check budget + create alert" are atomic. We use `ddl-auto=validate` with Flyway migrations for schema consistency, and BigDecimal for all monetary values to avoid floating-point errors.

### Security

**Q: How is authentication implemented?**
A: Stateless JWT authentication. On login, the server generates a signed JWT (HS256, 24h expiry) containing the user's email and ID. The Angular interceptor attaches this as a Bearer token on every request. Spring Security's filter chain validates the token before each request reaches the controller, and `@AuthenticationPrincipal` injects the authenticated user.

**Q: What prevents a user from accessing another user's data?**
A: Every service method receives the authenticated user's ID from `@AuthenticationPrincipal`. All repository queries filter by `userId`. For example, `getTransaction(id, userId)` throws `NotFoundException` if the transaction doesn't belong to the user — there's no way to omit the userId filter.

**Q: How do you handle CORS?**
A: `SecurityConfig` whitelists `http://localhost:4200` (Angular dev server) with specific allowed methods (GET, POST, PUT, DELETE) and headers (Authorization, Content-Type). In production, this would be updated to the deployed frontend URL.

### AI & ML

**Q: How does Gemini Vision differ from OCR + text parsing?**
A: Gemini Vision receives the raw image bytes and analyzes the receipt visually — it understands layout, fonts, and spatial relationships between text elements. OCR + text parsing first converts the image to a flat text string, losing layout information. Vision is more accurate for complex receipts (multi-column, faded text) but costs more API credits.

**Q: How do you handle Gemini API failures?**
A: The service uses `@Autowired(required = false)` so the app starts even without API keys. Each tier catches exceptions, logs them, and falls back to the next tier. The regex tier has no external dependencies. Transactions parsed by fallback tiers get lower confidence scores and `needsReview=true`.

**Q: What's the prompt engineering approach?**
A: Low temperature (0.1) for deterministic output. The prompt requests structured JSON with specific field names and date formats. The response parser strips markdown code blocks, handles null values gracefully, and uses safe type conversion (string → BigDecimal, string → LocalDate) with fallback defaults.

### Database & Performance

**Q: Why JSONB for line items instead of a separate table?**
A: Line items are always accessed with their parent transaction — we never query items independently. JSONB avoids a JOIN for the most common access pattern (listing transactions with their items). The tradeoff is that you can't easily query "find all transactions containing milk" — but that's not a use case we have.

**Q: How do analytics queries perform?**
A: Composite indexes like `idx_receipts_user_category_date` cover the most common analytics patterns (spending by category within a date range). The `AnalyticsService` uses JPQL queries with GROUP BY and date functions. For the dashboard, we make parallel API calls via `Promise.all` to load all chart data concurrently.

**Q: How does the budget calculation work?**
A: When building a `BudgetResponse`, the service calculates `currentSpending` by summing all transactions in that category within the budget's period (weekly: last 7 days, monthly: 1st of month to today, yearly: Jan 1 to today). It then computes `percentageUsed` and assigns a status: SAFE (<80%), WARNING (80-100%), EXCEEDED (>100%).

### Frontend

**Q: How are D3 charts implemented in Angular?**
A: Each chart method uses `@ViewChild` to get an `ElementRef` to a container div, then uses D3's `select()` to create an SVG. Charts are rendered in `ngAfterViewInit` and re-rendered on data changes. Window resize triggers `clearCharts()` + `renderAllCharts()`. The chart dimensions are derived from the container's `offsetWidth` for responsiveness.

**Q: How does the auth guard work?**
A: It's a functional `CanActivateFn` that checks `authService.isAuthenticated` (which checks for a token in localStorage). If unauthenticated, it creates a `UrlTree` redirecting to `/login` with the original URL as a `returnUrl` query parameter, so after login the user is redirected back.

**Q: How does the receipt upload component handle multiple files?**
A: It uses drag-and-drop (DragEvent) and file input. Files are validated (image type, <10MB), preview URLs are generated. On upload, each file is sent sequentially to `POST /api/transactions/process`. Results appear as editable cards where the user can review/correct AI-parsed data before confirming.

### DevOps & Operations

**Q: How are database migrations managed?**
A: Flyway runs on application startup. Migration files in `src/main/resources/db/migration/` follow the naming convention `V{N}__{description}.sql`. `spring.flyway.baseline-on-migrate=true` handles existing databases. `spring.jpa.hibernate.ddl-auto=validate` ensures entities match the migrated schema.

**Q: How would you deploy this to production?**
A: Backend as a JAR on a cloud VM or container (with environment variables for secrets). Frontend built with `ng build` and served via Nginx or a CDN. PostgreSQL on a managed service (e.g., Neon, RDS). Cloudinary and Gemini APIs are already cloud services. The `.env` file pattern keeps secrets out of code.
