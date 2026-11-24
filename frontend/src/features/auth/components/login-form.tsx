"use client";

import { useState } from "react";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import toast from "react-hot-toast";
import { ArrowRightIcon as ArrowRight, EyeIcon as Eye, EyeSlashIcon as EyeOff, CircleNotchIcon as Loader2, ShieldIcon as Shield, UserIcon as User } from "@phosphor-icons/react/ssr";

import { loginSchema, type LoginFormData } from "../schemas/login.schema";
import { useLogin } from "../hooks/use-login";
import { setCookie } from "@/utils/storage";
import { Role } from "@/constants/role";
import { authService } from "@/services/auth.service";
import { useAuthStore } from "@/stores/auth.store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

/**
 * Trả về đường dẫn redirect dựa trên role từ API response (chỉ 2 vai trò ADMIN & USER).
 */
function getRedirectPathByRole(role: 'ADMIN' | 'USER'): string {
  switch (role) {
    case Role.ADMIN:
      return "/admin/dashboard";
    case Role.USER:
      return "/profile";
    default:
      return "/";
  }
}

export function LoginForm() {
  const router = useRouter();
  const { mutate: login, isPending } = useLogin();
  const [showPassword, setShowPassword] = useState(false);

  const form = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      username: "",
      password: "",
    },
  });

  const onSubmit = (data: LoginFormData) => {
    login(data, {
      onSuccess: (res) => {
        setCookie("access_token", res.token);
        authService
          .getMe()
          .then((u) => useAuthStore.getState().setUser(u))
          .catch(() => {});

        toast.success(res.message || "Đăng nhập thành công");

        // Điều hướng dựa trên role từ API response
        router.push(getRedirectPathByRole(res.role));
      },
      onError: (error) => {
        const message = error.response?.data?.message || "Đăng nhập thất bại. Vui lòng kiểm tra lại.";
        toast.error(message);
      },
    });
  };

  // Điền thông tin đăng nhập nhanh vào form
  const handleQuickLogin = (role: 'ADMIN' | 'USER') => {
    const credentials = {
      ADMIN: { username: "admin", password: "password" },
      USER: { username: "user", password: "password" },
    };

    const { username, password } = credentials[role];

    form.setValue("username", username);
    form.setValue("password", password);

  };


  return (
    <div className="animate-fade-in w-full max-w-sm">
      <h1 className="text-3xl font-semibold tracking-[-0.04em] sm:text-4xl">Đăng nhập</h1>

      <form onSubmit={form.handleSubmit(onSubmit)} className="mt-7 space-y-4">
        <div className="space-y-2">
          <Label htmlFor="username">Tên đăng nhập</Label>
          <Input id="username" placeholder="Nhập tên đăng nhập" autoComplete="username"
            {...form.register("username")} disabled={isPending} aria-invalid={!!form.formState.errors.username}
            className="h-11 rounded-xl" />
          {form.formState.errors.username && <p className="text-xs text-destructive">{form.formState.errors.username.message}</p>}
        </div>
        <div className="space-y-2">
          <Label htmlFor="password">Mật khẩu</Label>
          <div className="relative">
            <Input id="password" type={showPassword ? "text" : "password"} placeholder="Nhập mật khẩu"
              autoComplete="current-password" className="h-11 rounded-xl pr-12"
              {...form.register("password")} disabled={isPending} aria-invalid={!!form.formState.errors.password} />
            <button type="button" onClick={() => setShowPassword(!showPassword)}
              className="its-interactive absolute inset-y-0 right-0 grid w-11 place-items-center rounded-xl text-muted-foreground hover:text-foreground"
              aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}>
              {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
            </button>
          </div>
          {form.formState.errors.password && <p className="text-xs text-destructive">{form.formState.errors.password.message}</p>}
          <div className="text-right">
            <button type="button" className="text-xs font-medium text-primary hover:underline"
              onClick={() => toast("Chưa hỗ trợ đặt lại mật khẩu.")}>Quên mật khẩu?</button>
          </div>
        </div>
        <Button type="submit" className="h-11 w-full rounded-xl font-semibold" disabled={isPending}>
          {isPending ? <><Loader2 className="animate-spin" /> Đang đăng nhập</> : <>Đăng nhập <ArrowRight /></>}
        </Button>
      </form>

      <div className="mt-5 border-t pt-4">
        <p className="mb-2.5 text-xs font-medium text-muted-foreground">Tài khoản dùng thử</p>
        <div className="grid grid-cols-2 gap-2">
          <Button type="button" variant="outline" onClick={() => handleQuickLogin("ADMIN")} className="h-10">
            <Shield /> Quản trị viên
          </Button>
          <Button type="button" variant="outline" onClick={() => handleQuickLogin("USER")} className="h-10">
            <User /> Khách thuê
          </Button>
        </div>
      </div>
      <p className="mt-5 text-sm text-muted-foreground">
        Chưa có tài khoản? <Link href="/register" className="font-semibold text-primary hover:underline">Đăng ký</Link>
      </p>
    </div>
  );
}
