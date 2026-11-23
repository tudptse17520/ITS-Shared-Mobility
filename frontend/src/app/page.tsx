import Link from "next/link";

export default function HomePage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-6">
      <h1 className="text-4xl font-bold text-center">
        ITS-Shared-Mobility
      </h1>
      <p className="text-lg text-muted-foreground text-center">
        Hệ thống cho thuê phương tiện giao thông chia sẻ & quản lý đội xe thông minh (ITS)
      </p>
      <Link
        href="/login"
        className="rounded-md bg-primary px-8 py-3 text-primary-foreground transition-colors hover:bg-primary/90"
      >
        Đăng nhập
      </Link>
    </div>
  );
}