"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { Facebook, Instagram, Youtube, Twitter } from "lucide-react"
import {
  fetchPublicUlinkItems,
  isUlinkImageType,
  isUlinkPolicyItem,
  type UlinkItem,
} from "@/lib/ulink"

const DEFAULT_SERVICE: UlinkItem[] = [
  { id: "d1", domainId: null, sectionCode: "FOOTER_TEXT", linkGroup: null, linkName: "뉴스", linkUrl: "/news", linkTarget: "_self", description: null, imgPath: null, sortOrder: 1, openYn: "Y", createdAt: null, updatedAt: null },
  { id: "d2", domainId: null, sectionCode: "FOOTER_TEXT", linkGroup: null, linkName: "경제PICK", linkUrl: "/economy-pick", linkTarget: "_self", description: null, imgPath: null, sortOrder: 2, openYn: "Y", createdAt: null, updatedAt: null },
  { id: "d3", domainId: null, sectionCode: "FOOTER_TEXT", linkGroup: null, linkName: "실시간VOD", linkUrl: "/live-vod", linkTarget: "_self", description: null, imgPath: null, sortOrder: 3, openYn: "Y", createdAt: null, updatedAt: null },
  { id: "d4", domainId: null, sectionCode: "FOOTER_TEXT", linkGroup: null, linkName: "커뮤니티", linkUrl: "/community", linkTarget: "_self", description: null, imgPath: null, sortOrder: 4, openYn: "Y", createdAt: null, updatedAt: null },
]

const DEFAULT_POLICY: UlinkItem[] = [
  { id: "p1", domainId: null, sectionCode: "FOOTER_TEXT", linkGroup: "POLICY", linkName: "이용약관", linkUrl: "/terms", linkTarget: "_self", description: null, imgPath: null, sortOrder: 1, openYn: "Y", createdAt: null, updatedAt: null },
  { id: "p2", domainId: null, sectionCode: "FOOTER_TEXT", linkGroup: "POLICY", linkName: "개인정보처리방침", linkUrl: "/privacy", linkTarget: "_self", description: null, imgPath: null, sortOrder: 2, openYn: "Y", createdAt: null, updatedAt: null },
  { id: "p3", domainId: null, sectionCode: "FOOTER_TEXT", linkGroup: "POLICY", linkName: "청소년보호정책", linkUrl: "/youth-policy", linkTarget: "_self", description: null, imgPath: null, sortOrder: 3, openYn: "Y", createdAt: null, updatedAt: null },
  { id: "p4", domainId: null, sectionCode: "FOOTER_TEXT", linkGroup: "POLICY", linkName: "시청자권익보호", linkUrl: "/viewer-rights", linkTarget: "_self", description: null, imgPath: null, sortOrder: 4, openYn: "Y", createdAt: null, updatedAt: null },
]

const DEFAULT_SOCIAL: UlinkItem[] = [
  { id: "s1", domainId: null, sectionCode: "FOOTER_IMAGE", linkGroup: null, linkName: "Facebook", linkUrl: "#", linkTarget: "_blank", description: "FACEBOOK", imgPath: null, sortOrder: 1, openYn: "Y", createdAt: null, updatedAt: null },
  { id: "s2", domainId: null, sectionCode: "FOOTER_IMAGE", linkGroup: null, linkName: "Instagram", linkUrl: "#", linkTarget: "_blank", description: "INSTAGRAM", imgPath: null, sortOrder: 2, openYn: "Y", createdAt: null, updatedAt: null },
  { id: "s3", domainId: null, sectionCode: "FOOTER_IMAGE", linkGroup: null, linkName: "YouTube", linkUrl: "#", linkTarget: "_blank", description: "YOUTUBE", imgPath: null, sortOrder: 3, openYn: "Y", createdAt: null, updatedAt: null },
  { id: "s4", domainId: null, sectionCode: "FOOTER_IMAGE", linkGroup: null, linkName: "Twitter", linkUrl: "#", linkTarget: "_blank", description: "TWITTER", imgPath: null, sortOrder: 4, openYn: "Y", createdAt: null, updatedAt: null },
]

function SocialIcon({ kind }: { kind: string }) {
  const key = kind.trim().toUpperCase()
  if (key === "INSTAGRAM") return <Instagram className="w-5 h-5" />
  if (key === "YOUTUBE") return <Youtube className="w-5 h-5" />
  if (key === "TWITTER" || key === "X") return <Twitter className="w-5 h-5" />
  return <Facebook className="w-5 h-5" />
}

function FooterLink({ item, className }: { item: UlinkItem; className?: string }) {
  const href = item.linkUrl || "#"
  const external = href.startsWith("http") || item.linkTarget === "_blank"
  const cls = className ?? "hover:text-white transition"
  if (external) {
    return (
      <a
        href={href}
        target={item.linkTarget === "_self" ? "_self" : "_blank"}
        rel={item.linkTarget === "_self" ? undefined : "noopener noreferrer"}
        className={cls}
      >
        {item.linkName}
      </a>
    )
  }
  return (
    <Link href={href} className={cls}>
      {item.linkName}
    </Link>
  )
}

function FooterMediaLink({ item }: { item: UlinkItem }) {
  const href = item.linkUrl || "#"
  const img = (item.imgPath || "").trim()
  const iconKey = (item.description || item.linkName || "FACEBOOK").toUpperCase()
  return (
    <a
      href={href}
      target={item.linkTarget === "_self" ? "_self" : "_blank"}
      rel={item.linkTarget === "_self" ? undefined : "noopener noreferrer"}
      aria-label={item.linkName}
      className="bg-gray-800 p-2 rounded-full hover:bg-gray-700 transition inline-flex items-center justify-center"
    >
      {img ? (
        <img src={img} alt={item.linkName} className="h-5 w-5 object-contain" />
      ) : (
        <SocialIcon kind={iconKey} />
      )}
    </a>
  )
}

export default function Footer() {
  const [service, setService] = useState<UlinkItem[]>(DEFAULT_SERVICE)
  const [policy, setPolicy] = useState<UlinkItem[]>(DEFAULT_POLICY)
  const [social, setSocial] = useState<UlinkItem[]>(DEFAULT_SOCIAL)

  useEffect(() => {
    let cancelled = false
    void (async () => {
      const result = await fetchPublicUlinkItems({ size: 100 })
      if (cancelled || !result.ok || result.data.length === 0) return

      const textItems = result.data.filter(
        (item) =>
          item.sectionCode === "FOOTER_TEXT" ||
          item.sectionCode === "FOOTER_SERVICE" ||
          item.sectionCode === "FOOTER_POLICY",
      )
      const imageItems = result.data.filter(
        (item) =>
          isUlinkImageType(item.sectionCode) ||
          item.sectionCode === "FOOTER_SOCIAL",
      )

      const nextService = textItems.filter((item) => !isUlinkPolicyItem(item))
      const nextPolicy = textItems.filter((item) => isUlinkPolicyItem(item))
      const legacyPolicy = result.data.filter((item) => item.sectionCode === "FOOTER_POLICY")
      const policyMerged =
        nextPolicy.length > 0
          ? nextPolicy
          : legacyPolicy.length > 0
            ? legacyPolicy
            : []

      if (nextService.length > 0) setService(nextService)
      if (policyMerged.length > 0) setPolicy(policyMerged)
      if (imageItems.length > 0) setSocial(imageItems)
    })()
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <footer className="bg-gray-900 text-gray-400 py-12">
      <div className="max-w-7xl mx-auto px-4 md:px-8">
        <div className="grid md:grid-cols-3 gap-8 mb-8">
          <div>
            <h3 className="text-white font-bold mb-4">finsight</h3>
            <p className="text-sm mb-2">핀사이트</p>
            <p className="text-sm leading-relaxed">
              서울특별시 마포구 상암산로 48-6
              <br />
              (상암동, finsight 타워)
            </p>
          </div>

          <div>
            <h4 className="text-white font-semibold mb-4">서비스</h4>
            <ul className="space-y-2 text-sm">
              {service.map((item) => (
                <li key={item.id}>
                  <FooterLink item={item} />
                </li>
              ))}
            </ul>
          </div>

          <div className="flex w-fit max-w-full flex-col items-start md:justify-self-end">
            <h4 className="text-white font-semibold mb-3">소셜미디어</h4>
            <div className="flex gap-3">
              {social.map((item) => (
                <FooterMediaLink key={item.id} item={item} />
              ))}
            </div>
          </div>
        </div>

        <div className="border-t border-gray-800 pt-8">
          <div className="flex flex-col md:flex-row justify-between items-center gap-4">
            <div className="flex flex-wrap items-center gap-4 text-sm">
              {policy.map((item, i) => (
                <span key={item.id} className="inline-flex items-center gap-4">
                  {i > 0 ? <span className="text-gray-600">|</span> : null}
                  <FooterLink
                    item={item}
                    className={
                      item.linkUrl.includes("privacy")
                        ? "hover:text-white transition font-semibold"
                        : "hover:text-white transition"
                    }
                  />
                </span>
              ))}
            </div>
            <p className="text-sm">© finsight 무단 이용 및 재배포 금지 · AI 학습 등 금지</p>
          </div>

          <div className="mt-6 text-xs text-gray-500">
            <p>핀사이트(주) | 사업자등록번호 : 000-00-00000 | 통신판매업신고번호 : 0000-서울마포-0000</p>
            <p className="mt-1">대표이사: 궁금하면 500원 | 개인정보보호책임자: 궁금하면 500원</p>
          </div>
        </div>
      </div>
    </footer>
  )
}
