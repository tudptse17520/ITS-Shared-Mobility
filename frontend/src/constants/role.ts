// ---------------------------------------------
// Role Constants
// Vai trò tài khoản trong hệ thống ITS-Shared-Mobility
// ---------------------------------------------

export enum Role {
  ADMIN = "ADMIN",
  USER = "USER",
}

export const ROLE_LABELS: Record<Role, string> = {
  [Role.ADMIN]: "Quản trị viên",
  [Role.USER]: "Khách thuê",
};
