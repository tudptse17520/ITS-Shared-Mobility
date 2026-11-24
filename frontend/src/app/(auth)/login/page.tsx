import { AuthFrame } from "@/components/layout/auth-frame";
import { LoginForm } from "@/features/auth/components/login-form";

export const metadata = {
  title: "Đăng nhập | ITS Mobility",
  description: "Đăng nhập ITS Mobility",
};

export default function LoginPage() {
  return <AuthFrame><LoginForm /></AuthFrame>;
}
