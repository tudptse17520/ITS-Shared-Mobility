import Link from "next/link";
import { ArrowUpRightIcon as ArrowUpRight } from "@phosphor-icons/react/ssr";
import { AuthFrame } from "@/components/layout/auth-frame";
import { buttonVariants } from "@/components/ui/button";

export default function HomePage() {
  return (
    <AuthFrame>
      <div className="animate-fade-in w-full max-w-sm">
        <h1 className="text-3xl leading-tight font-semibold tracking-tight sm:text-4xl">Thuê xe trong thành phố</h1>
        <div className="mt-7 flex flex-wrap gap-2">
          <Link href="/login" className={buttonVariants({ size: "lg", className: "h-11 px-4" })}>
            Đăng nhập <ArrowUpRight />
          </Link>
          <Link href="/register" className={buttonVariants({ variant: "outline", size: "lg", className: "h-11 px-4" })}>
            Tạo tài khoản
          </Link>
        </div>
      </div>
    </AuthFrame>
  );
}
