"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import toast from "react-hot-toast";
import {
  Eye,
  EyeOff,
  Loader2,
  UserPlus,
  User,
  Lock,
  Phone,
  BadgeInfo,
  Bike,
} from "lucide-react";

import { authService } from "@/services/auth.service";
import { setCookie } from "@/utils/storage";
import { useAuthStore } from "@/stores/auth.store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export default function RegisterPage() {
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [fullName, setFullName] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isPending, setIsPending] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    const trimmedUser = username.trim();
    const trimmedName = fullName.trim();
    const trimmedPhone = phoneNumber.trim();

    if (trimmedUser.length < 3 || trimmedUser.length > 50) {
      setErrorMsg("Tên đăng nhập phải từ 3 đến 50 ký tự.");
      return;
    }
    if (!trimmedName) {
      setErrorMsg("Họ và tên không được để trống.");
      return;
    }
    if (!trimmedPhone) {
      setErrorMsg("Số điện thoại không được để trống.");
      return;
    }
    if (password.length < 6) {
      setErrorMsg("Mật khẩu phải có ít nhất 6 ký tự.");
      return;
    }
    if (password !== confirmPassword) {
      setErrorMsg("Mật khẩu xác nhận không khớp.");
      return;
    }

    setIsPending(true);
    try {
      const res = await authService.register({
        username: trimmedUser,
        password,
        fullName: trimmedName,
        phoneNumber: trimmedPhone,
      });

      setCookie("access_token", res.token);
      try {
        const me = await authService.getMe();
        useAuthStore.getState().setUser(me);
      } catch {
        useAuthStore.getState().setUser({
          username: trimmedUser,
          fullName: trimmedName,
          phoneNumber: trimmedPhone,
          role: res.role,
        });
      }

      toast.success(res.message || "Đăng ký tài khoản thành công!");
      router.push("/profile");
    } catch (err: any) {
      const msg =
        err?.response?.data?.message ||
        "Đăng ký thất bại (tên đăng nhập hoặc số điện thoại có thể đã tồn tại).";
      setErrorMsg(msg);
      toast.error(msg);
    } finally {
      setIsPending(false);
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center p-4">
      <div className="fixed inset-0 gradient-bg" />
      <div className="fixed inset-0 bg-background/60" />

      <div className="relative z-10 w-full max-w-[460px] rounded-[28px] border border-border/40 bg-card/95 p-8 md:p-10 shadow-xl backdrop-blur-xl">
        <div className="mb-6 flex flex-col items-center text-center">
          <div className="mb-3 flex h-14 w-14 items-center justify-center rounded-2xl bg-indigo-600 text-white shadow-lg shadow-indigo-600/20">
            <Bike className="h-7 w-7 stroke-[2]" />
          </div>
          <h1 className="text-2xl font-bold tracking-tight">Đăng ký tài khoản</h1>
          <p className="mt-1.5 text-sm text-muted-foreground">
            Tạo tài khoản khách thuê phương tiện trên hệ thống Project_ITS.
          </p>
        </div>

        {errorMsg && (
          <div className="mb-4 rounded-xl border border-destructive/40 bg-destructive/10 px-3.5 py-2.5 text-sm text-destructive">
            {errorMsg}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="reg-username" className="flex items-center gap-2">
              <User className="h-4 w-4 text-muted-foreground" />
              <span>Tên đăng nhập</span>
            </Label>
            <Input
              id="reg-username"
              placeholder="VD: nguyenvana (tối thiểu 3 ký tự)"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={isPending}
              required
              className="h-10 rounded-xl"
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="reg-fullname" className="flex items-center gap-2">
              <BadgeInfo className="h-4 w-4 text-muted-foreground" />
              <span>Họ và tên</span>
            </Label>
            <Input
              id="reg-fullname"
              placeholder="VD: Nguyễn Văn A"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              disabled={isPending}
              required
              className="h-10 rounded-xl"
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="reg-phone" className="flex items-center gap-2">
              <Phone className="h-4 w-4 text-muted-foreground" />
              <span>Số điện thoại</span>
            </Label>
            <Input
              id="reg-phone"
              placeholder="VD: 0911222333"
              value={phoneNumber}
              onChange={(e) => setPhoneNumber(e.target.value)}
              disabled={isPending}
              required
              className="h-10 rounded-xl"
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="reg-password" className="flex items-center gap-2">
              <Lock className="h-4 w-4 text-muted-foreground" />
              <span>Mật khẩu</span>
            </Label>
            <div className="relative">
              <Input
                id="reg-password"
                type={showPassword ? "text" : "password"}
                placeholder="Ít nhất 6 ký tự"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={isPending}
                required
                className="h-10 rounded-xl pr-10"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                tabIndex={-1}
                aria-label="Hiện hoặc ẩn mật khẩu"
              >
                {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="reg-confirm" className="flex items-center gap-2">
              <Lock className="h-4 w-4 text-muted-foreground" />
              <span>Xác nhận mật khẩu</span>
            </Label>
            <Input
              id="reg-confirm"
              type={showPassword ? "text" : "password"}
              placeholder="Nhập lại mật khẩu"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              disabled={isPending}
              required
              className="h-10 rounded-xl"
            />
          </div>

          <Button
            type="submit"
            disabled={isPending}
            className="mt-2 h-11 w-full rounded-xl bg-indigo-600 font-semibold text-white hover:bg-indigo-700"
          >
            {isPending ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Đang tạo tài khoản...
              </>
            ) : (
              <>
                <UserPlus className="mr-2 h-4 w-4" />
                Đăng ký tài khoản
              </>
            )}
          </Button>
        </form>

        <div className="mt-6 text-center text-sm text-muted-foreground">
          <span>Đã có tài khoản? </span>
          <Link href="/login" className="font-semibold text-indigo-600 hover:underline dark:text-indigo-400">
            Đăng nhập ngay
          </Link>
        </div>
      </div>
    </div>
  );
}

