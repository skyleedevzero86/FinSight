import CommunityBoardShell from "@/components/community/CommunityBoardShell"
import "@/styles/community-board.css"

export default function CommunityLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return <CommunityBoardShell>{children}</CommunityBoardShell>
}
