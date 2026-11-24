import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { jwtDecode } from "jwt-decode";

interface JwtPayload {
  role?: string;
  exp?: number;
}

/** Các route không cần xác thực */
const PUBLIC_ROUTES = ["/", "/login", "/register", "/forbidden"];

/** Mapping role -> prefix route được phép truy cập (chỉ 2 vai trò ADMIN & USER) */
const ROLE_ROUTE_MAP: Record<string, string[]> = {
  ADMIN: ["/admin", "/manager", "/profile"],
  USER: ["/profile"],
};

function isPublicRoute(pathname: string): boolean {
  return PUBLIC_ROUTES.some(
    (route) => pathname === route || pathname.startsWith(`${route}?`)
  );
}

function decodeJwtPayload(token: string): JwtPayload | null {
  try {
    return jwtDecode<JwtPayload>(token);
  } catch {
    return null;
  }
}

function hasRouteAccess(role: string, pathname: string): boolean {
  const allowedPrefixes = ROLE_ROUTE_MAP[role];
  if (!allowedPrefixes) return false;
  return allowedPrefixes.some((prefix) => pathname.startsWith(prefix));
}

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;

  if (
    pathname.startsWith("/_next") ||
    pathname.startsWith("/api") ||
    pathname.includes(".")
  ) {
    return NextResponse.next();
  }

  if (isPublicRoute(pathname)) {
    return NextResponse.next();
  }

  const token = request.cookies.get("access_token")?.value;

  if (!token) {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("redirect", pathname);
    return NextResponse.redirect(loginUrl);
  }

  const payload = decodeJwtPayload(token);

  if (!payload || !payload.role) {
    const response = NextResponse.redirect(new URL("/login", request.url));
    response.cookies.delete("access_token");
    return response;
  }

  if (payload.exp && Date.now() >= payload.exp * 1000) {
    const response = NextResponse.redirect(new URL("/login", request.url));
    response.cookies.delete("access_token");
    return response;
  }

  if (!hasRouteAccess(payload.role, pathname)) {
    return NextResponse.redirect(new URL("/forbidden", request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    "/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)",
  ],
};

