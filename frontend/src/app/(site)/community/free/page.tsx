import type { Metadata } from "next"
import CommunityBoardLayout from "@/components/community/CommunityBoardLayout"

export const metadata: Metadata = {
  title: "포트폴리오 공유 | 커뮤니티 | finsight",
  description: "finsight 포트폴리오 공유 (준비 중)",
}

export default function CommunityPortfolioPage() {
  return (
    <CommunityBoardLayout
      heading="포트폴리오 공유"
      description="투자·포트폴리오를 공유하는 공간입니다. 곧 별도 기능으로 제공됩니다."
    >
      <div className="rounded-xl border border-slate-200 bg-slate-50 px-5 py-10 text-center">
        <p className="text-base font-semibold text-slate-800">준비 중인 메뉴입니다</p>
        <p className="mt-2 text-sm text-slate-600">
          포트폴리오 공유는 게시판이 아닌 전용 화면으로 구성할 예정입니다.
        </p>
      </div>
    </CommunityBoardLayout>
  )
}
