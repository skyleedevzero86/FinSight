"use client"

import { useEffect, useRef, useState } from "react"
import { useAuthSession } from "@/components/AuthSessionProvider"
import PortfolioShareArticle, { type ShareModeration } from "@/components/community/PortfolioShareArticle"
import { canManageUsers } from "@/lib/adminUsers"
import {
  fetchPortfolioShares,
  moderatePortfolioShare,
  type PortfolioShareCard,
  type PortfolioShareGoal,
} from "@/lib/portfolioApi"

export default function CommunityShareBoard() {
  const [cards, setCards] = useState<PortfolioShareCard[]>([])
  const [goals, setGoals] = useState<PortfolioShareGoal[]>([])
  const [error, setError] = useState("")
  const [loading, setLoading] = useState(true)
  const [scrollMode, setScrollMode] = useState(false)
  const [guideOpen, setGuideOpen] = useState(false)
  const [pendingId, setPendingId] = useState<number | null>(null)
  const { user } = useAuthSession()
  const moderator = canManageUsers(user?.role)
  const pageRef = useRef(0)
  const hasNextRef = useRef(false)
  const loadingRef = useRef(false)
  const sentinelRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    let alive = true
    fetchPortfolioShares(0)
      .then((feed) => {
        if (!alive) return
        setCards(feed.cards)
        setGoals(feed.goals)
        hasNextRef.current = feed.hasNext
      })
      .catch((reason: unknown) => {
        if (alive) setError(reason instanceof Error ? reason.message : "포트폴리오 공유를 불러오지 못했습니다.")
      })
      .finally(() => {
        if (alive) setLoading(false)
      })
    return () => {
      alive = false
    }
  }, [])

  async function loadMore() {
    if (loadingRef.current || !hasNextRef.current) return
    loadingRef.current = true
    setLoading(true)
    setError("")
    const next = pageRef.current + 1
    try {
      const feed = await fetchPortfolioShares(next)
      setCards((current) => [...current, ...feed.cards])
      pageRef.current = next
      hasNextRef.current = feed.hasNext
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "다음 공유를 불러오지 못했습니다.")
    } finally {
      loadingRef.current = false
      setLoading(false)
    }
  }

  useEffect(() => {
    if (!scrollMode) return
    const node = sentinelRef.current
    if (!node) return
    const observer = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        void loadMore()
      }
    }, { rootMargin: "240px" })
    observer.observe(node)
    return () => observer.disconnect()
  }, [scrollMode, cards.length])

  async function moderate(shareId: number, status: Exclude<ShareModeration, "OPEN">) {
    setPendingId(shareId)
    setError("")
    try {
      const updated = await moderatePortfolioShare(shareId, status)
      setCards((current) => current.map((card) => (card.id === shareId ? updated : card)))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "공유 게시물을 처리하지 못했습니다.")
    } finally {
      setPendingId(null)
    }
  }

  return (
    <div className="grid items-start gap-8 lg:grid-cols-[minmax(0,1fr)_280px]">
      <section>
        <header className="fcb-heading">
          <h2>포트폴리오 공유</h2>
        </header>
        {error ? <p className="mb-4 text-sm text-red-600">{error}</p> : null}
        {loading && cards.length === 0 ? <p className="text-sm text-slate-500">공유 카드를 불러오는 중입니다.</p> : null}
        {!loading && cards.length === 0 && !error ? (
          <p className="rounded-2xl border border-slate-200 px-5 py-10 text-center text-sm text-slate-500">
            아직 공유된 포트폴리오가 없습니다.
          </p>
        ) : null}
        <div className="grid gap-4 md:grid-cols-2">
          {cards.map((card) => (
            <PortfolioShareArticle
              key={card.id}
              card={card}
              pending={pendingId === card.id}
              onModerate={moderator ? (status) => void moderate(card.id, status) : undefined}
            />
          ))}
        </div>
        <div className="mt-8 flex justify-center">
          <button
            type="button"
            onClick={() => {
              setScrollMode(true)
              void loadMore()
            }}
            className="rounded-xl border border-slate-200 bg-white px-8 py-3 text-sm text-slate-700"
          >
            {loading && cards.length > 0 ? "불러오는 중…" : "이어서 보기"}
          </button>
        </div>
        {scrollMode ? <div ref={sentinelRef} className="h-8" /> : null}
      </section>
      <aside className="space-y-4">
        <div className="rounded-2xl border border-slate-200 bg-white p-5">
          <h3 className="text-sm font-semibold text-slate-900">이번 주 인기 목표</h3>
          <ol className="mt-4 space-y-3">
            {goals.map((goal, index) => (
              <li key={goal.label} className="flex items-center justify-between gap-3 text-sm">
                <span className="text-slate-700">
                  <span className="mr-2 text-slate-400">{index + 1}</span>
                  {goal.label}
                </span>
                <span className="shrink-0 text-slate-500">{goal.participantCount.toLocaleString("ko-KR")}명</span>
              </li>
            ))}
          </ol>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-5">
          <h3 className="text-sm font-semibold text-slate-900">공유 안전 가이드</h3>
          <p className="mt-3 text-xs leading-5 text-slate-500">
            계좌번호, 전화번호, 주소, 실명, 주민번호처럼 개인을 특정할 수 있는 내용은 공유 전에 빼 주세요.
          </p>
          <button
            type="button"
            onClick={() => setGuideOpen(true)}
            className="mt-4 w-full rounded-xl border border-slate-200 py-2.5 text-sm text-slate-700"
          >
            가이드 보기
          </button>
        </div>
      </aside>
      {guideOpen ? <ShareGuide onClose={() => setGuideOpen(false)} /> : null}
    </div>
  )
}

function ShareGuide({ onClose }: { onClose: () => void }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 px-4" onClick={onClose}>
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="share-guide-title"
        className="max-h-[80vh] w-full max-w-lg overflow-y-auto rounded-2xl bg-white p-6"
        onClick={(event) => event.stopPropagation()}
      >
        <h3 id="share-guide-title" className="text-lg font-semibold text-slate-900">포트폴리오 공유 가이드</h3>
        <div className="mt-4 space-y-3 text-sm leading-6 text-slate-600">
          <p>나의 포트폴리오에서 자산을 등록한 뒤 공유하기를 누르면, 이번 달 기록이 저장되고 이 목록용 카드가 만들어집니다.</p>
          <p>공유할 때 순자산, 전월 대비 증가율, 자산 구성, 목표 달성률, 부채처럼 카드에 올릴 항목을 고릅니다. 금액은 정확한 금액, 금액 구간, 비율만 중에서 고릅니다.</p>
          <p>공개 범위는 전체 공개, 팔로워, 나만 보기입니다. 이 커뮤니티 목록에는 전체 공개와 작성한 설명만 올라옵니다.</p>
          <p>이름은 첫 글자만 보이고 나머지는 가립니다. 계좌번호, 전화번호, 주소, 실명, 주민번호는 카드에 넣지 않습니다. 자산 원본은 공유 카드와 따로 저장됩니다.</p>
          <p>이번 주 인기 목표는 이번 주 공개 카드의 공통 주제를 벡터로 묶어, 같은 목표를 올린 사람 수로 1위부터 4위까지 센 결과입니다.</p>
        </div>
        <div className="mt-5 flex justify-end">
          <button type="button" onClick={onClose} className="rounded-xl border border-slate-200 px-4 py-2 text-sm text-slate-700">
            닫기
          </button>
        </div>
      </div>
    </div>
  )
}

