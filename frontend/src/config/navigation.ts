// ---------------------------------------------
// Navigation Configuration
// Định nghĩa mảng Menu tĩnh cho Sidebar
// ---------------------------------------------

import {
  LayoutDashboard,
  Users,
  Settings,
  MapPin,
  Bike,
  DollarSign,
  MessageSquareWarning,
  BarChart3,
  CalendarCheck,
  Car,
  UserCircle,
  type LucideIcon,
} from "lucide-react";
import { Role } from "@/constants/role";

export interface NavItem {
  title: string;
  href: string;
  icon: LucideIcon;
  roles: Role[];
}

export interface NavGroup {
  label: string;
  items: NavItem[];
}

export const ADMIN_NAV: NavGroup[] = [
  {
    label: "Tổng quan",
    items: [
      {
        title: "Dashboard",
        href: "/admin/dashboard",
        icon: LayoutDashboard,
        roles: [Role.ADMIN],
      },
      {
        title: "Bản đồ Đội xe & IoT",
        href: "/admin/fleet-map",
        icon: Bike,
        roles: [Role.ADMIN],
      },
    ],
  },
  {
    label: "Quản lý Shared Mobility",
    items: [
      {
        title: "Trạm xe công cộng",
        href: "/manager/stations",
        icon: MapPin,
        roles: [Role.ADMIN],
      },
      {
        title: "Loại xe cho thuê",
        href: "/manager/vehicle-types",
        icon: Car,
        roles: [Role.ADMIN],
      },
      {
        title: "Bảng giá thuê xe",
        href: "/manager/pricing",
        icon: DollarSign,
        roles: [Role.ADMIN],
      },
      {
        title: "Đặt giữ & Chuyến đi",
        href: "/manager/bookings",
        icon: CalendarCheck,
        roles: [Role.ADMIN],
      },
      {
        title: "Tài khoản",
        href: "/admin/users",
        icon: Users,
        roles: [Role.ADMIN],
      },
      {
        title: "Sự cố & Phản hồi",
        href: "/manager/feedbacks",
        icon: MessageSquareWarning,
        roles: [Role.ADMIN],
      },
      {
        title: "Thống kê Doanh thu",
        href: "/manager/reports",
        icon: BarChart3,
        roles: [Role.ADMIN],
      },
      {
        title: "Cấu hình",
        href: "/admin/settings",
        icon: Settings,
        roles: [Role.ADMIN],
      },
    ],
  },
];

export const DRIVER_NAV: NavItem[] = [
  {
    title: "Hồ sơ & Lịch sử thuê xe",
    href: "/profile",
    icon: UserCircle,
    roles: [Role.USER],
  },
];

export const adminMenus = ADMIN_NAV;
export const driverMenus = DRIVER_NAV;

