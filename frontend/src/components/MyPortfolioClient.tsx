"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { PortfolioAssetDialog } from "@/components/portfolio/PortfolioAssetDialog"
import { PortfolioDashboard } from "@/components/portfolio/PortfolioDashboard"
import { PortfolioRecordDialog } from "@/components/portfolio/PortfolioRecordDialog"
import PortfolioReportAdmin from "@/components/portfolio/PortfolioReportAdmin"
import { PortfolioShareDialog } from "@/components/portfolio/PortfolioShareDialog"
import { canManageUsers } from "@/lib/adminUsers"
import { fetchWatchlist } from "@/lib/myAccount"
import {
  deletePortfolioAsset,
  fetchPortfolio,
  publishPortfolioShare,
  recordPortfolioMonth,
  registerPortfolioAsset,
  type PortfolioAsset,
  type PortfolioAssetInput,
  type PortfolioShareRequest,
  type PortfolioSummary,
} from "@/lib/portfolioApi"
import { fetchPersonalizedNews, fetchPopularNews, type StoredNewsCard } from "@/lib/publicNews"

export default function MyPortfolioClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const [summary, setSummary] = useState<PortfolioSummary | null>(null)
  const [news, setNews] = useState<StoredNewsCard[]>([])
  const [newsCaption, setNewsCaption] = useState("뉴스를 불러오는 중입니다.")
  const [error, setError] = useState("")
  const [shareNote, setShareNote] = useState("")
  const [assetOpen, setAssetOpen] = useState(false)
  const [recordOpen, setRecordOpen] = useState(false)
  const [shareOpen, setShareOpen] = useState(false)
  const [pending, setPending] = useState(false)
  const admin = canManageUsers(user?.role)

  useEffect(() => {
    if (!ready) return
    if (!user) router.replace(`/login?next=${encodeURIComponent("/myinfo/portfolio")}`)
  }, [ready, user, router])

  useEffect(() => {
    if (!ready || !user || canManageUsers(user.role)) return
    let alive = true
    fetchPortfolio()
      .then((data) => {
        if (alive) setSummary(data)
      })
      .catch((reason: unknown) => {
        if (alive) setError(reason instanceof Error ? reason.message : "포트폴리오를 불러오지 못했습니다.")
      })
    loadAssetNews()
      .then((loaded) => {
        if (!alive) return
        setNews(loaded.news)
        setNewsCaption(loaded.caption)
      })
      .catch(() => {
        if (!alive) return
        setNews([])
        setNewsCaption("뉴스를 불러오지 못했습니다.")
      })
    return () => {
      alive = false
    }
  }, [ready, user])

  if (!ready || !user) {
    return (
      <div className="mx-auto max-w-[960px] px-4 py-10 md:px-6">
        <p className="text-sm text-gray-500">로그인 확인 중…</p>
      </div>
    )
  }

  async function saveAsset(input: PortfolioAssetInput) {
    setPending(true)
    setError("")
    try {
      setSummary(await registerPortfolioAsset(input))
      setAssetOpen(false)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "자산을 등록하지 못했습니다.")
    } finally {
      setPending(false)
    }
  }

  async function removeAsset(asset: PortfolioAsset) {
    if (!window.confirm(`${asset.name}을 삭제할까요?`)) return
    setPending(true)
    setError("")
    try {
      setSummary(await deletePortfolioAsset(asset.id))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "자산을 삭제하지 못했습니다.")
    } finally {
      setPending(false)
    }
  }

  async function saveMonth(memo: string) {
    setPending(true)
    setError("")
    try {
      setSummary(await recordPortfolioMonth(memo))
      setRecordOpen(false)
      setShareOpen(false)
      setShareNote("이번 달 Snapshot을 저장했습니다.")
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "이번 달 기록을 저장하지 못했습니다.")
    } finally {
      setPending(false)
    }
  }

  async function publishShare(request: PortfolioShareRequest) {
    setPending(true)
    setError("")
    try {
      setSummary(await publishPortfolioShare(request))
      setShareOpen(false)
      setShareNote(
        request.visibility === "PUBLIC"
          ? "커뮤니티 포트폴리오 공유에 올렸습니다."
          : "공유를 저장했습니다. 커뮤니티 목록에는 전체 공개만 올라갑니다.",
      )
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "포트폴리오를 공유하지 못했습니다.")
    } finally {
      setPending(false)
    }
  }

  if (admin) {
    return (
      <div className="min-h-full bg-[#f4f7fb]">
        <PortfolioReportAdmin />
      </div>
    )
  }

  return (
    <div className="min-h-full bg-[#f4f7fb]">
      {error ? <p className="mx-auto max-w-[1100px] px-4 pt-4 text-sm text-red-600 md:px-6">{error}</p> : null}
      {!summary ? (
        <p className="px-6 py-10 text-sm text-slate-500">포트폴리오를 불러오는 중입니다.</p>
      ) : (
        <PortfolioDashboard
          summary={summary}
          news={news}
          newsCaption={newsCaption}
          shareNote={shareNote}
          onRegister={() => setAssetOpen(true)}
          onRecord={() => setRecordOpen(true)}
          onShare={() => setShareOpen(true)}
          onDelete={(asset) => void removeAsset(asset)}
        />
      )}
      {assetOpen ? (
        <PortfolioAssetDialog pending={pending} onClose={() => setAssetOpen(false)} onSubmit={(input) => void saveAsset(input)} />
      ) : null}
      {recordOpen && summary ? (
        <PortfolioRecordDialog summary={summary} pending={pending} onClose={() => setRecordOpen(false)} onSubmit={(memo) => void saveMonth(memo)} />
      ) : null}
      {shareOpen && summary ? (
        <PortfolioShareDialog summary={summary} pending={pending} onClose={() => setShareOpen(false)} onSubmit={(request) => void publishShare(request)} />
      ) : null}
    </div>
  )
}

async function loadAssetNews(): Promise<{ news: StoredNewsCard[]; caption: string }> {
  const watchlist = await fetchWatchlist()
  const selected = watchlist.filter((code) => code && code !== "NONE")
  if (selected.length === 0) {
    return {
      news: (await fetchPopularNews(3)) ?? [],
      caption: "선택한 관심 카테고리가 없어 가장 인기 많은 뉴스만 보여 줍니다.",
    }
  }
  return {
    news: (await fetchPersonalizedNews(3)) ?? [],
    caption: "관심 카테고리에서 고른 뉴스만 보여 줍니다.",
  }
}
