# Budget Planner - Receipt Scanner & AI Budget Predictor

A full-stack personal finance application that uses OCR and AI to scan receipts, categorize expenses, track budgets, and provide AI-powered financial insights.

## Features

**Receipt Processing**
- Upload receipt images with drag-and-drop or file picker
- Three-tier parsing: Gemini Vision (multimodal) → Tesseract OCR + AI text parsing → Regex fallback
- Automatic merchant, amount, date, and line item extraction
- Confidence scoring with manual review flagging
- Manual expense entry for non-receipt transactions

**Budget & Expense Tracking**
- Category-based budgets with flexible periods (weekly, monthly, yearly)
- Real-time budget alerts with severity levels (Safe, Warning, Exceeded)
- AI-generated savings suggestions when budgets are exceeded
- Recurring expense management with automated transaction creation
- Income tracking and savings analysis

**Analytics Dashboard**
- Weekly and monthly spending charts (D3.js line and bar charts)
- Category breakdown pie chart
- Income vs. expense comparison
- Savings trend visualization (diverging bar chart)
- Top spending categories with merchant breakdown

**AI Assistant**
- Chat-based financial advisor powered by Google Gemini
- Context-aware spending insights
- Suggested quick questions for common queries

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Frontend | Angular (Standalone Components) | 21 |
| UI Components | Angular Material | 21 |
| Charts | D3.js | 7.9 |
| Backend | Spring Boot | 3.5 |
| Language | Java | 21 |
| Database | PostgreSQL | 16+ |
| ORM | Spring Data JPA + Flyway migrations | |
| Authentication | JWT (jjwt) | 0.12 |
| OCR | Tesseract (tess4j) | 5.9 |
| AI | Google Gemini 2.0 Flash | |
| Image Storage | Cloudinary | |
| Build | Gradle (backend), Angular CLI + Vite (frontend) | |

## Architecture

```
Frontend (Angular 21)                    Backend (Spring Boot 3.5)
┌─────────────────────┐                 ┌──────────────────────────┐
│ Components          │  REST + JWT     │ Controllers              │
│ Services            │ ──────────────► │ Services                 │
│ Auth Guard/Intercept│                 │ Repositories             │
│ D3.js Charts        │                 │ Security (JWT Filter)    │
└─────────────────────┘                 │ Scheduled Tasks          │
                                        └────────┬─────────────────┘
                                                 │
                              ┌──────────────────┼──────────────────┐
                              ▼                  ▼                  ▼
                        PostgreSQL         Cloudinary          Gemini AI
                        (Data)             (Images)            (OCR/Chat)
```

## Prerequisites

- Java 21+
- Node.js 18+
- PostgreSQL 16+
- Tesseract OCR installed locally
  - macOS: `brew install tesseract`
  - Ubuntu: `sudo apt install tesseract-ocr`
- Google Gemini API key ([Get one here](https://aistudio.google.com/apikey))
- Cloudinary account ([Sign up free](https://cloudinary.com))

## Setup

### 1. Database

```bash
createdb receipt_scanner
```

### 2. Backend

Create `backend/.env` with your credentials:

```env
DB_USERNAME=your_postgres_user
DB_PASSWORD=your_postgres_password
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
GEMINI_API_KEY=your_gemini_key
JWT_SECRET=your_256_bit_secret
```

Run the backend:

```bash
cd backend
./gradlew bootRun
```

The API starts at `http://localhost:8080`. Flyway automatically creates all database tables on first run.

### 3. Frontend

```bash
cd frontend
npm install
npx ng serve
```

The app starts at `http://localhost:4200`.

## API Overview

| Area | Base Path | Key Endpoints |
|------|-----------|---------------|
| Auth | `/api/auth` | `POST /register`, `POST /login`, `GET /me` |
| Transactions | `/api/transactions` | CRUD + `POST /process` (receipt scan) |
| Budgets | `/api/budgets` | CRUD with spending calculations |
| Alerts | `/api/alerts` | Budget alerts with AI suggestions |
| Analytics | `/api/analytics` | Weekly/monthly/category spending |
| Income | `/api/income` | Income source CRUD |
| Recurring | `/api/recurring` | Recurring expense management |
| Categories | `/api/categories` | Predefined + custom categories |
| Chat | `/api/chat` | AI assistant messaging |

All endpoints except `/api/auth/**` require a JWT Bearer token.

## Receipt Processing Pipeline

```
Receipt Image
    │
    ├── Tier 1: Gemini Vision (sends raw image to AI)
    │
    ├── Tier 2: Tesseract OCR → Gemini text parsing
    │           (preprocess: upscale → grayscale → binarize)
    │
    └── Tier 3: Regex fallback (basic pattern matching)

    → Validate & flag low-confidence results
    → Save transaction + check budget alerts
```

## Project Structure

```
receipt-scanner/
├── backend/
│   └── src/main/java/com/receiptscan/
│       ├── config/          # Security, Cloudinary beans
│       ├── controller/      # 12 REST controllers
│       ├── service/         # Business logic + AI/OCR integration
│       ├── entity/          # 10 JPA entities
│       ├── repository/      # Spring Data interfaces
│       ├── dto/             # Request/Response objects
│       ├── security/        # JWT util, filter, UserDetailsService
│       ├── exception/       # Global error handling
│       └── scheduler/       # Cron jobs (recurring expenses, alerts)
├── frontend/
│   └── src/app/
│       ├── components/      # 10 standalone Angular components
│       ├── services/        # 6 HTTP services
│       ├── models/          # TypeScript interfaces
│       ├── guards/          # Auth route guard
│       └── interceptors/    # JWT token interceptor
└── README.md
```

## Author

**Pranav Sai Musham**

## License

MIT License
