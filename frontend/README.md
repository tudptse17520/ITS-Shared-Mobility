# Frontend Structure

```text
frontend/
├── public/              # Public static files (logos, global images, icons)
├── src/
│   ├── app/             # App Router (pages, layouts, route groups)
│   ├── components/      # Reusable UI components (ui/, common/, layouts/)
│   ├── config/          # Environment, permissions, navigation menus
│   ├── constants/       # Roles, statuses, and domain enums
│   ├── features/        # Business logic modules (UI local state & components)
│   ├── hooks/           # Global custom hooks
│   ├── lib/             # Third-party clients (Axios client, Query client)
│   ├── providers/       # Global state providers (Query, Theme, Toast, Auth)
│   ├── services/        # Centralized API layer (Axios endpoints)
│   ├── stores/          # Global client state (Zustand stores)
│   ├── types/           # TypeScript types/interfaces matching Backend Entities
│   └── utils/           # Global helper functions (formatters)
└── src/proxy.ts         # Role-based route guard (JWT cookie)
```

## Web routes

- `/` — project landing page.
- `/login` and `/register` — account access.
- `/admin/dashboard` — fleet and ride summary.
- `/admin/fleet-map` — fleet map and telemetry.
- `/manager/stations` — stations and dock availability.
- `/manager/bookings` — vehicle reservations.
- `/manager/feedbacks` — ride feedback and incidents.
- `/manager/reports` — ride flow, revenue and current dock utilization.
- `/profile` — rider profile and ride history.

## Prerequisites
- Node.js 20.9 or newer
- npm

## Setup & Run
1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm ci
   ```
3. Set up environment variables:
   Copy `.env.example` to `.env.local`. Set `NEXT_PUBLIC_API_URL` if the backend uses a different URL from `http://localhost:8080/api/v1`. Set `NEXT_PUBLIC_VIETMAP_API_KEY` to use the VietMap basemap.
4. Start the development server:
   ```bash
   npm run dev
   ```
The frontend application will start on `http://localhost:3000`.
