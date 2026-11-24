# Frontend Structure

```text
frontend/
├── public/              # Public static files (logos, global images, icons)
├── src/
│   ├── app/             # App Router (pages, layouts, route groups, middleware)
│   ├── components/      # Reusable UI components (ui/, common/, layouts/)
│   ├── config/          # Environment, permissions, navigation menus
│   ├── constants/       # Roles, statuses, and domain enums
│   ├── features/        # Business logic modules (UI local state & components)
│   ├── hooks/           # Global custom hooks
│   ├── lib/             # Third-party clients (Axios client, Query client)
│   ├── mocks/           # Mock data for frontend independent development
│   ├── providers/       # Global state providers (Query, Theme, Toast, Auth)
│   ├── services/        # Centralized API layer (Axios endpoints)
│   ├── stores/          # Global client state (Zustand stores)
│   ├── types/           # TypeScript types/interfaces matching Backend Entities
│   └── utils/           # Global helper functions (formatters)
└── middleware.ts        # Route middleware (demo mode)
```

## Web routes

- `/` — project landing page.
- `/admin/fleet-map` — fleet map and telemetry.
- `/manager/bookings` — vehicle reservations.
- `/manager/feedbacks` — ride feedback and incidents.
- `/profile` — rider profile and ride history.

The `feature/web-auth-layout` branch adds `/login`, `/register`, and the shared dashboard components required to build these pages together.

## Prerequisites
- Node.js 20.9 or newer
- npm, yarn, or pnpm

## Setup & Run
1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm install
   # or yarn install / pnpm install
   ```
3. Set up environment variables:
   Set `NEXT_PUBLIC_API_URL` in `.env.local` if the backend uses a different URL from `http://localhost:8080/api/v1`. Set `NEXT_PUBLIC_VIETMAP_API_KEY` to display the VietMap basemap.
4. Start the development server:
   ```bash
   npm run dev
   # or yarn dev / pnpm dev
   ```
The frontend application will start on `http://localhost:3000`.
