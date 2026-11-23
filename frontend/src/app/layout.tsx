import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";

import { QueryProvider } from "@/providers/query-provider";
import { ToastProvider } from "@/providers/toast-provider";
import { AuthProvider } from "@/providers/auth-provider";
import { ThemeProvider } from "@/providers/theme-provider";
import { TooltipProvider } from "@/components/ui/tooltip";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
  display: "swap",
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "ITS-Shared-Mobility - Hệ thống Cho thuê Phương tiện Giao thông Chia sẻ",
  description:
    "Nền tảng quản trị đội xe thông minh & phương tiện chia sẻ (ITS Shared Mobility)",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="vi"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
      suppressHydrationWarning
    >
      <body className="min-h-full flex flex-col" suppressHydrationWarning>
        <QueryProvider>
          <ThemeProvider>
            <AuthProvider>
              <TooltipProvider delay={200}>
                {children}
                <ToastProvider />
              </TooltipProvider>
            </AuthProvider>
          </ThemeProvider>
        </QueryProvider>
      </body>
    </html>
  );
}
