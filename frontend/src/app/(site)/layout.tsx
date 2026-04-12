import Header from "@/components/Header"
import Footer from "@/components/Footer"

export default function SiteLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <div className="flex min-h-screen flex-col bg-white">
      <Header />
      <main className="min-h-0 flex-1 w-full">{children}</main>
      <Footer />
    </div>
  )
}
