import MyInfoShell from "@/components/MyInfoShell"

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return <MyInfoShell>{children}</MyInfoShell>
}
