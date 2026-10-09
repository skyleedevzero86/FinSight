import Header from "@/components/Header"
import Footer from "@/components/Footer"

export default function SiteLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <div className="flex min-h-screen w-full flex-1 flex-col bg-white">
      <Header />
      <main className="relative flex min-h-0 w-full flex-1 flex-col">{children}</main>
      <Footer />
    </div>
  )
}
