import Image from "next/image";
import { BrandMark } from "./brand";

export function AuthFrame({ children }: { children: React.ReactNode }) {
  return (
    <main className="auth-layout grid min-h-dvh grid-cols-1 bg-background lg:grid-cols-[minmax(0,0.95fr)_minmax(0,1.05fr)]">
      <section className="flex min-w-0 flex-col px-6 py-6 sm:px-10 lg:px-[clamp(2rem,5vw,6rem)] lg:py-9">
        <div className="auth-opening-brand flex items-center gap-3">
          <BrandMark />
          <span className="text-base font-semibold tracking-tight">ITS Mobility</span>
        </div>
        <div className="auth-opening-content flex flex-1 items-center justify-center py-9 sm:py-12">
          {children}
        </div>
      </section>
      <section className="auth-cover auth-opening-cover relative m-3 ml-0 hidden min-h-[640px] overflow-hidden rounded-[18px] lg:flex lg:flex-col lg:justify-end" aria-label="Xe đạp công cộng tại TP. Hồ Chí Minh">
        <Image src="/its-xe-dap-tp-hcm.webp" alt="" fill priority sizes="(min-width: 1024px) 52vw, 1px" className="auth-opening-image object-cover object-[center_62%]" />
        <div className="auth-cover-shade absolute inset-0" aria-hidden="true" />
        <div className="auth-opening-caption relative m-5 mb-6 max-w-[34rem] rounded-[15px] border border-white/75 bg-[#fffaf0]/94 px-5 py-4 text-[#312a22] shadow-sm backdrop-blur-md xl:m-8 xl:px-7 xl:py-5">
          <p className="text-3xl leading-tight font-semibold tracking-[-0.035em] xl:text-[2.8rem]">Xe công cộng tại TP. Hồ Chí Minh</p>
          <p className="mt-2 text-sm text-[#62554a] xl:text-base">Xe đạp và xe máy điện dùng chung.</p>
        </div>
      </section>
    </main>
  );
}
