// ---------------------------------------------
// Navigation Configuration
// Định nghĩa mảng Menu tĩnh cho Sidebar
// ---------------------------------------------

import { SquaresFourIcon as LayoutDashboard, UsersThreeIcon as Users, GearSixIcon as Settings, MapPinIcon as MapPin, BicycleIcon as Bike, CurrencyDollarIcon as DollarSign, ChatCircleDotsIcon as MessageSquareWarning, ChartBarIcon as BarChart3, CalendarCheckIcon as CalendarCheck, CarIcon as Car, UserCircleIcon as UserCircle } from "@phosphor-icons/react/ssr";
import type { Icon } from "@phosphor-icons/react";
import { Role } from "@/constants/role";

export interface NavItem {
  title: string;
  href: string;
  icon: Icon;
  roles: Role[];
}

export interface NavGroup {
  label: string;
  items: NavItem[];
}

export const ADMIN_NAV: NavGroup[] = [
  {
    label: "Điều hướng",
    items: [
      {
        title: "Tổng quan",
        href: "/admin/dashboard",
        icon: LayoutDashboard,
        roles: [Role.ADMIN],
      },
      {
        title: "Bản đồ đội xe",
        href: "/admin/fleet-map",
        icon: Bike,
        roles: [Role.ADMIN],
      },
    ],
  },
  {
    label: "Vận hành",
    items: [
      {
        title: "Trạm xe",
        href: "/manager/stations",
        icon: MapPin,
        roles: [Role.ADMIN],
      },
      {
        title: "Loại xe",
        href: "/manager/vehicle-types",
        icon: Car,
        roles: [Role.ADMIN],
      },
      {
        title: "Bảng giá",
        href: "/manager/pricing",
        icon: DollarSign,
        roles: [Role.ADMIN],
      },
      {
        title: "Đặt xe và chuyến đi",
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
        title: "Sự cố và phản hồi",
        href: "/manager/feedbacks",
        icon: MessageSquareWarning,
        roles: [Role.ADMIN],
      },
      {
        title: "Doanh thu",
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

