import type { Metadata } from "next"
import PortfolioReportAdmin from "@/components/portfolio/PortfolioReportAdmin"

export const metadata: Metadata = {
  title: "포트폴리오 신고 관리 | finsight",
  description: "공개 Snapshot 신고만 다룹니다. PRIVATE 자산 원본은 보이지 않습니다.",
}

export default function AdminModerationPage() {
  return <PortfolioReportAdmin />
}
