"use client"

import Link from "next/link"
import { FormEvent, useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import {
  createLiveVodComment,
  fetchLiveVodComments,
  type LiveVodComment,
} from "@/lib/liveVodEngagement"

export default function LiveVodComments({ videoId }: { videoId: string }) {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const [comments, setComments] = useState<LiveVodComment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [content, setContent] = useState("")
  const [replyTo, setReplyTo] = useState<LiveVodComment | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const reload = async () => {
    setLoading(true)
    setError(null)
    try {
      const list = await fetchLiveVodComments(videoId)
      setComments(list)
    } catch (e) {
      setError(e instanceof Error ? e.message : "댓글을 불러오지 못했습니다.")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void reload()
  }, [videoId])

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!ready) return
    if (!user) {
      router.push(`/login?next=${encodeURIComponent(window.location.pathname + window.location.search)}`)
      return
    }
    const text = content.trim()
    if (!text) return
    setSubmitting(true)
    setError(null)
    try {
      await createLiveVodComment(videoId, text, replyTo?.id ?? null)
      setContent("")
      setReplyTo(null)
      await reload()
    } catch (err) {
      setError(err instanceof Error ? err.message : "댓글 등록에 실패했습니다.")
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="flv-comments">
      <h2>댓글 {comments.reduce((n, c) => n + 1 + c.replies.length, 0)}</h2>

      <form className="flv-comment-form" onSubmit={(e) => void onSubmit(e)}>
        {replyTo ? (
          <p className="flv-reply-hint">
            {replyTo.authorNickname || replyTo.userEmail} 님에게 답글{" "}
            <button type="button" onClick={() => setReplyTo(null)}>
              취소
            </button>
          </p>
        ) : null}
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder={user ? "댓글을 입력하세요" : "로그인 후 댓글을 작성할 수 있습니다"}
          rows={3}
          maxLength={2000}
        />
        <div className="flv-comment-actions">
          {!user ? (
            <Link href={`/login?next=${encodeURIComponent(typeof window !== "undefined" ? window.location.pathname + window.location.search : "/live-vod")}`}>
              로그인
            </Link>
          ) : null}
          <button type="submit" disabled={submitting || !content.trim()}>
            {replyTo ? "답글 등록" : "댓글 등록"}
          </button>
        </div>
      </form>

      {error ? <p className="flv-comment-error">{error}</p> : null}
      {loading ? <p className="text-sm text-gray-500">댓글 불러오는 중…</p> : null}

      <ul className="flv-comment-list">
        {comments.map((c) => (
          <li key={c.id} className="flv-comment-item">
            <div className="flv-comment-head">
              <strong>{c.authorNickname || c.userEmail}</strong>
              {c.createdAt ? <time>{c.createdAt.replace("T", " ").slice(0, 16)}</time> : null}
            </div>
            <p>{c.content}</p>
            <button type="button" className="flv-reply-btn" onClick={() => setReplyTo(c)}>
              답글
            </button>
            {c.replies.length > 0 ? (
              <ul className="flv-reply-list">
                {c.replies.map((r) => (
                  <li key={r.id}>
                    <div className="flv-comment-head">
                      <strong>{r.authorNickname || r.userEmail}</strong>
                      {r.createdAt ? <time>{r.createdAt.replace("T", " ").slice(0, 16)}</time> : null}
                    </div>
                    <p>{r.content}</p>
                  </li>
                ))}
              </ul>
            ) : null}
          </li>
        ))}
      </ul>
    </section>
  )
}
