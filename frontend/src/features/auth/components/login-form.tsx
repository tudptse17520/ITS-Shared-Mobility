"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import toast from "react-hot-toast";
import { Eye, EyeOff, Loader2, Shield, User, Lock, LogIn, Bike } from "lucide-react";

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
    toast.success(`Đã điền tài khoản mẫu quyền ${role}!`);
  };

  return (
    <div className="w-full rounded-[28px] border border-border/40 bg-card/90 p-8 md:p-10 shadow-xl backdrop-blur-xl animate-scale-in max-w-[440px] mx-auto">
      {/* Logo & Header */}
      <div className="mb-8 flex flex-col items-center text-center">
        <div className="mb-4 flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-indigo-600 text-white shadow-lg shadow-indigo-600/20 dark:bg-indigo-500">
          <Bike className="h-8 w-8 stroke-[2]" />
        </div>
        <h1 className="text-3xl font-bold tracking-tight text-slate-900 dark:text-white">
          Đăng nhập
        </h1>
        <p className="mt-2.5 text-sm text-muted-foreground/90 leading-relaxed">
          Chào mừng trở lại! Vui lòng đăng nhập để tiếp tục.
        </p>
      </div>

      {/* Form */}
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-5">
        {/* Username */}
        <div className="space-y-2">
          <Label htmlFor="username" className="flex items-center gap-2 text-slate-700 dark:text-slate-300">
            <User className="h-4 w-4 text-slate-400" />
            <span>Tên đăng nhập</span>
          </Label>
          <Input
            id="username"
            placeholder="Nhập tên đăng nhập"
            autoComplete="username"
            {...form.register("username")}
            disabled={isPending}
            className="h-11 rounded-xl"
          />
          {form.formState.errors.username && (
            <p className="text-xs text-destructive mt-1.5">
              {form.formState.errors.username.message}
            </p>
          )}
        </div>

        {/* Password */}
        <div className="space-y-2">
          <Label htmlFor="password" className="flex items-center gap-2 text-slate-700 dark:text-slate-300">
            <Lock className="h-4 w-4 text-slate-400" />
            <span>Mật khẩu</span>
          </Label>
          <div className="relative">
            <Input
              id="password"
              type={showPassword ? "text" : "password"}
              placeholder="Nhập mật khẩu"
              autoComplete="current-password"
              className="pr-11 h-11 rounded-xl"
              {...form.register("password")}
              disabled={isPending}
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground/50 hover:text-foreground transition-colors duration-200"
              tabIndex={-1}
              aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
            >
              {showPassword ? (
                <EyeOff className="h-4.5 w-4.5" />
              ) : (
                <Eye className="h-4.5 w-4.5" />
              )}
            </button>
          </div>
          {form.formState.errors.password && (
            <p className="text-xs text-destructive mt-1.5">
              {form.formState.errors.password.message}
            </p>
          )}
          
          {/* Forgot Password Link */}
          <span 
            className="text-xs font-medium text-indigo-600 dark:text-indigo-400 hover:underline cursor-pointer block text-right mt-2"
            onClick={() => toast("Tính năng khôi phục mật khẩu đang được phát triển.")}
          >
            Quên mật khẩu?
          </span>
        </div>

        {/* Submit */}
        <Button
          type="submit"
          className="w-full h-11 mt-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white shadow-md shadow-indigo-600/10 font-semibold gap-2 transition-all active:scale-[0.98]"
          disabled={isPending}
        >
          {isPending ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin" />
              Đang xử lý...
            </>
          ) : (
            <>
              <LogIn className="h-4 w-4" />
              Đăng nhập
            </>
          )}
        </Button>
      </form>

      {/* Quick Login Section */}
      <div className="mt-6">
        <div className="relative flex py-3 items-center">
          <div className="flex-grow border-t border-border/40"></div>
          <span className="flex-shrink mx-4 text-xs font-medium text-muted-foreground/60">
            Đăng nhập nhanh với vai trò
          </span>
          <div className="flex-grow border-t border-border/40"></div>
        </div>
        
        <div className="grid grid-cols-2 gap-3 mt-3">
          <Button
            type="button"
            variant="outline"
            onClick={() => handleQuickLogin("ADMIN")}
            className="rounded-xl px-2 border-rose-500 text-rose-600 hover:bg-rose-500/10 dark:border-rose-500/40 dark:text-rose-400 dark:hover:bg-rose-500/20 text-xs h-12 flex-col font-medium gap-1 transition-all"
          >
            <Shield className="h-4 w-4" />
            Quản trị viên (Admin)
          </Button>

          <Button
            type="button"
            variant="outline"
            onClick={() => handleQuickLogin("USER")}
            className="rounded-xl px-2 border-emerald-500 text-emerald-600 hover:bg-emerald-500/10 dark:border-emerald-500/40 dark:text-emerald-400 dark:hover:bg-emerald-500/20 text-xs h-12 flex-col font-medium gap-1 transition-all"
          >
            <User className="h-4 w-4" />
            Khách thuê (User)
          </Button>
        </div>
      </div>

      {/* Footer Registration Link */}
      <div className="mt-8 text-center text-sm text-muted-foreground">
        <span>Chưa có tài khoản? </span>
        <button
          type="button"
          className="text-indigo-600 dark:text-indigo-400 font-semibold hover:underline cursor-pointer"
          onClick={() => router.push("/register")}
        >
          Đăng ký ngay
        </button>
      </div>
    </div>
  );
}
