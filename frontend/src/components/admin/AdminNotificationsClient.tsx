"use client"

import { useCallback, useEffect, useState, type FormEvent } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import AdminNoticeBoardSection from "@/components/admin/AdminNoticeBoardSection"
import { canManageUsers } from "@/lib/adminUsers"
import { pageWindow } from "@/data/communityBoardConfig"
import "@/styles/community-board.css"
import {
  INBOX_CATEGORIES,
  broadcastInbox,
  fetchAdminInboxPage,
  formatInboxTime,
  type InboxCategory,
  type InboxItem,
} from "@/lib/inbox"

const inputClass =
  "w-full rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 outline-none focus:border-finsight-secondary focus:ring-1 focus:ring-finsight-secondary/40"

const primaryButtonClass =
  "rounded bg-finsight-primary px-4 py-2 text-sm text-white hover:bg-finsight-primary/90 disabled:opacity-50"

type TabKey = "notice" | "inbox"

export default function AdminNotificationsClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const allowed = Boolean(user && canManageUsers(user.role))
  const [tab, setTab] = useState<TabKey>("notice")

  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  const [category, setCategory] = useState<InboxCategory>("ADMIN")
  const [title, setTitle] = useState("")
  const [body, setBody] = useState("")
  const [linkUrl, setLinkUrl] = useState("")
  const [actorName, setActorName] = useState("FinSight")
  const [target, setTarget] = useState<"all" | "admins">("all")

  const [page, setPage] = useState(0)
  const [rows, setRows] = useState<InboxItem[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [loading, setLoading] = useState(false)

  const loadHistory = useCallback(async () => {
    setLoading(true)
    setError(null)
    const result = await fetchAdminInboxPage({ page, size: 20 })
    setLoading(false)
    if (!result.ok) {
      setRows([])
      setTotalElements(0)
      setError(result.message)
      return
    }
    setRows(result.page.content)
    setTotalElements(result.page.totalElements)
    setTotalPages(Math.max(1, result.page.totalPages))
  }, [page])

  useEffect(() => {
    if (!ready) return
    if (!allowed) {
      router.replace("/")
    }
  }, [ready, allowed, router])

  useEffect(() => {
    if (!allowed || tab !== "inbox") return
    void loadHistory()
  }, [allowed, tab, loadHistory])

  async function onBroadcast(e: FormEvent) {
    e.preventDefault()
    if (!title.trim()) {
      setError("알림 문구를 입력해 주세요.")
      return
    }
    setSaving(true)
    setError(null)
    setMessage(null)
    const result = await broadcastInbox({
      category,
      title: title.trim(),
      body: body.trim() || undefined,
      linkUrl: linkUrl.trim() || undefined,
      actorName: actorName.trim() || "FinSight",
      allUsers: target === "all",
      adminsOnly: target === "admins",
    })
    setSaving(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    setMessage(`${result.createdCount}건의 알림을 등록했습니다.`)
    setTitle("")
    setBody("")
    setPage(0)
    void loadHistory()
  }

  if (!ready || !allowed) {
    return (
      <div className="w-full px-6 py-16 text-sm text-gray-500">권한을 확인하는 중…</div>
    )
  }

  const safeTotalPages = Math.max(1, totalPages)
  const pages = pageWindow(page + 1, safeTotalPages, 7)

  return (
    <div className="fcb-page fcb-embed">
      <div className="fcb-main">
        <div className="fcb-container">
          <header className="fcb-heading">
            <h2>공지·알림</h2>
            <p className="fcb-description">
              공지 게시판은 관리자가 작성하고, 알림은 등록한 뒤 발송 이력을 확인합니다.
            </p>
          </header>

          <div className="bbs_cate tablist fcb-tablist">
            <ul className="tablist_3d fcb-tablist-3d">
              {(
                [
                  ["notice", "공지 게시판"],
                  ["inbox", "알림 등록"],
                ] as const
              ).map(([key, label]) => (
                <li key={key} className={tab === key ? "on fcb-on" : undefined}>
                  <button type="button" onClick={() => setTab(key)}>
                    {label}
                  </button>
                </li>
              ))}
            </ul>
          </div>

          <div hidden={tab !== "notice"}>
            <AdminNoticeBoardSection />
          </div>

          <div hidden={tab !== "inbox"}>
            <section>
              <form className="mb-8 grid gap-3" onSubmit={(e) => void onBroadcast(e)}>
                <h3 className="text-lg font-semibold text-[#00216a]">알림 등록</h3>
                <label className="grid gap-1 text-sm">
                  <span className="text-gray-600">카테고리</span>
                  <select
                    className={inputClass}
                    value={category}
                    onChange={(e) => setCategory(e.target.value as InboxCategory)}
                  >
                    {INBOX_CATEGORIES.map((c) => (
                      <option key={c.value} value={c.value}>
                        {c.label}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="grid gap-1 text-sm">
                  <span className="text-gray-600">대상</span>
                  <select
                    className={inputClass}
                    value={target}
                    onChange={(e) => setTarget(e.target.value as "all" | "admins")}
                  >
                    <option value="all">전체 사용자</option>
                    <option value="admins">관리자만</option>
                  </select>
                </label>
                <label className="grid gap-1 text-sm">
                  <span className="text-gray-600">표시 이름</span>
                  <input
                    className={inputClass}
                    value={actorName}
                    onChange={(e) => setActorName(e.target.value)}
                    placeholder="FinSight"
                  />
                </label>
                <label className="grid gap-1 text-sm">
                  <span className="text-gray-600">알림 문구</span>
                  <input
                    className={inputClass}
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    placeholder="새 공지가 등록되었습니다."
                    required
                  />
                </label>
                <label className="grid gap-1 text-sm">
                  <span className="text-gray-600">부가 설명 (선택)</span>
                  <textarea
                    className={inputClass}
                    rows={2}
                    value={body}
                    onChange={(e) => setBody(e.target.value)}
                  />
                </label>
                <label className="grid gap-1 text-sm">
                  <span className="text-gray-600">이동 URL (선택)</span>
                  <input
                    className={inputClass}
                    value={linkUrl}
                    onChange={(e) => setLinkUrl(e.target.value)}
                    placeholder="/community/notice"
                  />
                </label>
                <div className="pt-1">
                  <button type="submit" className={primaryButtonClass} disabled={saving}>
                    {saving ? "등록 중…" : "알림 등록"}
                  </button>
                </div>
              </form>

              {error ? <p className="mb-3 text-sm text-red-600">{error}</p> : null}
              {message ? <p className="mb-3 text-sm text-teal-700">{message}</p> : null}

              <div className="bbs bbs_list bbs_basic" id="bbs_admin_inbox">
                <div className="bbs_leadin">
                  <div className="bbs_count">
                    <span className="list-count">
                      발송 이력 <strong>{totalElements}</strong>건
                    </span>
                    <span className="page-count">
                      <b>{page + 1}</b> / {safeTotalPages}page
                    </span>
                  </div>
                </div>
                <div className="bbs_listing">
                  <table className="table">
                    <caption>알림 발송 이력</caption>
                    <thead>
                      <tr>
                        <th className="td_num">번호</th>
                        <th className="td_name">분류</th>
                        <th className="td_subject">알림 문구</th>
                        <th className="td_date">발송일</th>
                      </tr>
                    </thead>
                    <tbody>
                      {loading ? (
                        <tr>
                          <td className="td_subject" colSpan={4}>
                            발송 이력을 불러오는 중…
                          </td>
                        </tr>
                      ) : rows.length === 0 ? (
                        <tr>
                          <td className="td_subject" colSpan={4}>
                            등록된 알림이 없습니다.
                          </td>
                        </tr>
                      ) : (
                        rows.map((row, index) => (
                          <tr key={row.id}>
                            <td className="td_num">{page * 20 + index + 1}</td>
                            <td className="td_name">
                              {INBOX_CATEGORIES.find((c) => c.value === row.category)?.label ??
                                row.category}
                            </td>
                            <td className="td_subject">
                              {row.title}
                              {row.body ? ` · ${row.body}` : ""}
                            </td>
                            <td className="td_date">{formatInboxTime(row.createdAt)}</td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
                <div className="bbs_paging">
                  <nav className="pg_wrap" aria-label="알림 이력 페이지">
                    <span className="pg">
                      <button
                        type="button"
                        className="pg_page pg_start"
                        disabled={page <= 0}
                        onClick={() => setPage(0)}
                      >
                        처음
                      </button>
                      <button
                        type="button"
                        className="pg_page pg_prev"
                        disabled={page <= 0}
                        onClick={() => setPage((current) => Math.max(0, current - 1))}
                      >
                        이전
                      </button>
                      {pages.map((pageNo) =>
                        pageNo === page + 1 ? (
                          <span key={pageNo} className="pg_current pg_must" aria-current="page">
                            {pageNo}
                          </span>
                        ) : (
                          <button
                            key={pageNo}
                            type="button"
                            className="pg_page pg_must"
                            onClick={() => setPage(pageNo - 1)}
                          >
                            {pageNo}
                          </button>
                        ),
                      )}
                      <button
                        type="button"
                        className="pg_page pg_next"
                        disabled={page + 1 >= safeTotalPages}
                        onClick={() => setPage((current) => current + 1)}
                      >
                        다음
                      </button>
                      <button
                        type="button"
                        className="pg_page pg_end"
                        disabled={page + 1 >= safeTotalPages}
                        onClick={() => setPage(safeTotalPages - 1)}
                      >
                        맨끝
                      </button>
                    </span>
                  </nav>
                </div>
              </div>
            </section>
          </div>
        </div>
      </div>
    </div>
  )
}
