"use client";

import { useState } from "react";
import Link from "next/link";
import { AuthFrame } from "@/components/layout/auth-frame";
import { useRouter } from "next/navigation";
import toast from "react-hot-toast";
import { EyeIcon as Eye, EyeSlashIcon as EyeOff, CircleNotchIcon as Loader2, UserPlusIcon as UserPlus, UserIcon as User, LockKeyIcon as Lock, PhoneIcon as Phone, IdentificationBadgeIcon as BadgeInfo } from "@phosphor-icons/react/ssr";

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
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { message?: string } } };
      const msg =
        apiErr?.response?.data?.message ||
        "Đăng ký thất bại (tên đăng nhập hoặc số điện thoại có thể đã tồn tại).";
      setErrorMsg(msg);
      toast.error(msg);
    } finally {
      setIsPending(false);
    }
  };

  return (
    <AuthFrame>
      <div className="animate-fade-in w-full max-w-sm">
        <h1 className="mb-5 text-3xl font-semibold tracking-[-0.04em]">Tạo tài khoản</h1>
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
              autoComplete="username"
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
              autoComplete="name"
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
              type="tel"
              autoComplete="tel"
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
            className="mt-2 h-11 w-full rounded-xl font-semibold"
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
          <Link href="/login" className="font-semibold text-primary hover:underline">
            Đăng nhập ngay
          </Link>
        </div>
      </div>
    </AuthFrame>
  );
}

