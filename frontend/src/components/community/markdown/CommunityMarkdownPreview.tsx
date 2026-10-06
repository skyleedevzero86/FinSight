"use client"

import type { ComponentPropsWithoutRef } from "react"
import { useMemo, useRef } from "react"
import ReactMarkdown from "react-markdown"
import remarkGfm from "remark-gfm"
import {
  createHeadingDefinitions,
  escapeCssIdentifier,
} from "@/lib/communityMarkdown"

type Props = {
  title: string
  author?: string
  markdown: string
  tags?: string[]
  metaLine?: string
  eyebrow?: string
  showTableOfContents?: boolean
  className?: string
}

type HeadingTag = "h1" | "h2" | "h3" | "h4" | "h5" | "h6"
type MarkdownHeadingProps = ComponentPropsWithoutRef<"h1"> & {
  node?: {
    position?: {
      start?: {
        line?: number | null
      } | null
    } | null
  }
}

const MAX_TOC_LEVEL = 4
const SCROLL_OFFSET = 24

export default function CommunityMarkdownPreview({
  title,
  author = "",
  markdown,
  tags = [],
  metaLine,
  eyebrow = "미리보기",
  showTableOfContents = true,
  className = "",
}: Props) {
  const panelRef = useRef<HTMLElement | null>(null)
  const headingDefinitions = useMemo(() => createHeadingDefinitions(markdown), [markdown])
  const headingDefinitionByLine = useMemo(
    () => new Map(headingDefinitions.map((heading) => [heading.line, heading])),
    [headingDefinitions]
  )
  const tableOfContents = useMemo(
    () => headingDefinitions.filter((heading) => heading.level <= MAX_TOC_LEVEL),
    [headingDefinitions]
  )
  const showSideTableOfContents = showTableOfContents && tableOfContents.length > 0

  const renderHeading = (tagName: HeadingTag) => {
    const Heading = ({ children, node, ...props }: MarkdownHeadingProps) => {
      const line = node?.position?.start?.line ?? undefined
      const heading = line ? headingDefinitionByLine.get(line) : undefined
      const TagName = tagName

      return (
        <TagName {...props} id={heading?.id}>
          {children}
        </TagName>
      )
    }
    Heading.displayName = `MarkdownHeading(${tagName})`
    return Heading
  }

  const handleTableOfContentsSelect = (headingId: string) => {
    const panel = panelRef.current
    const target =
      panel?.querySelector<HTMLElement>(
        `.fcb-md-preview-markdown #${escapeCssIdentifier(headingId)}`
      ) ?? document.getElementById(headingId)

    if (!target) return

    if (panel && isPanelScrollable(panel)) {
      const panelRect = panel.getBoundingClientRect()
      const targetRect = target.getBoundingClientRect()
      const nextScrollTop = panel.scrollTop + (targetRect.top - panelRect.top) - SCROLL_OFFSET
      panel.scrollTo({ top: Math.max(0, nextScrollTop), behavior: "smooth" })
      return
    }

    const nextWindowScrollTop = window.scrollY + target.getBoundingClientRect().top - SCROLL_OFFSET
    window.scrollTo({ top: Math.max(0, nextWindowScrollTop), behavior: "smooth" })
  }

  return (
    <section
      ref={panelRef}
      className={`fcb-md-preview${showSideTableOfContents ? " fcb-md-preview--with-toc" : ""}${className ? ` ${className}` : ""}`}
    >
      <div className="fcb-md-preview__content">
        <div className="fcb-md-preview__header">
          {eyebrow ? <span className="fcb-md-preview__eyebrow">{eyebrow}</span> : null}
          <h2 className="fcb-md-preview__title">{title || "제목 없는 초안"}</h2>
          {(metaLine || author) && (
            <p className="fcb-md-preview__meta">{metaLine || author}</p>
          )}
          {tags.length > 0 ? (
            <div className="fcb-md-preview__tags">
              {tags.map((tag) => (
                <span key={tag} className="fcb-md-preview-tag">
                  #{tag}
                </span>
              ))}
            </div>
          ) : null}
        </div>

        <article className="fcb-md-preview-markdown">
          <ReactMarkdown
            remarkPlugins={[remarkGfm]}
            components={{
              h1: renderHeading("h1"),
              h2: renderHeading("h2"),
              h3: renderHeading("h3"),
              h4: renderHeading("h4"),
              h5: renderHeading("h5"),
              h6: renderHeading("h6"),
              img: ({ alt, src, ...props }) => {
                if (!src) return null
                return <img {...props} src={src} alt={alt ?? "markdown"} loading="lazy" />
              },
              a: ({ children, ...props }) => (
                <a {...props} target="_blank" rel="noreferrer">
                  {children}
                </a>
              ),
            }}
          >
            {markdown || "_내용을 입력하면 미리보기가 표시됩니다._"}
          </ReactMarkdown>
        </article>
      </div>

      {showSideTableOfContents ? (
        <aside className="fcb-md-toc" aria-label="목차">
          <div className="fcb-md-toc__card">
            <strong className="fcb-md-toc__title">목차</strong>
            <div className="fcb-md-toc__items">
              {tableOfContents.map((heading) => (
                <button
                  key={heading.id}
                  type="button"
                  className={`fcb-md-toc__item fcb-md-toc__item--level-${Math.min(heading.level, MAX_TOC_LEVEL)}`}
                  onClick={() => handleTableOfContentsSelect(heading.id)}
                >
                  {heading.text}
                </button>
              ))}
            </div>
          </div>
        </aside>
      ) : null}
    </section>
  )
}

function isPanelScrollable(panel: HTMLElement) {
  return (
    panel.scrollHeight > panel.clientHeight + 8 &&
    getComputedStyle(panel).overflowY !== "visible"
  )
}
