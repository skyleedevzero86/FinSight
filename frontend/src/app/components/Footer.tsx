import Link from "next/link"
import { Facebook, Instagram, Youtube, Twitter } from "lucide-react"

export default function Footer() {
  return (
    <footer className="bg-gray-900 text-gray-400 py-12">
      <div className="max-w-7xl mx-auto px-4 md:px-8">
        <div className="grid md:grid-cols-3 gap-8 mb-8">
          <div>
            <h3 className="text-white font-bold mb-4">finsight</h3>
            <p className="text-sm mb-2">핀사이트</p>
            <p className="text-sm leading-relaxed">
              서울특별시 마포구 상암산로 48-6<br />
              (상암동, finsight 타워)
            </p>
          </div>

          <div>
            <h4 className="text-white font-semibold mb-4">서비스</h4>
            <ul className="space-y-2 text-sm">
              <li><Link href="#" className="hover:text-white transition">뉴스</Link></li>
              <li><Link href="#" className="hover:text-white transition">경제PICK</Link></li>
              <li><Link href="#" className="hover:text-white transition">실시간VOD</Link></li>
              <li><Link href="#" className="hover:text-white transition">커뮤니티</Link></li>
            </ul>
          </div>

          <div className="flex w-fit max-w-full flex-col items-start md:justify-self-end">
            <h4 className="text-white font-semibold mb-3">소셜미디어</h4>
            <div className="flex gap-3">
              <Link href="#" className="bg-gray-800 p-2 rounded-full hover:bg-gray-700 transition">
                <Facebook className="w-5 h-5" />
              </Link>
              <Link href="#" className="bg-gray-800 p-2 rounded-full hover:bg-gray-700 transition">
                <Instagram className="w-5 h-5" />
              </Link>
              <Link href="#" className="bg-gray-800 p-2 rounded-full hover:bg-gray-700 transition">
                <Youtube className="w-5 h-5" />
              </Link>
              <Link href="#" className="bg-gray-800 p-2 rounded-full hover:bg-gray-700 transition">
                <Twitter className="w-5 h-5" />
              </Link>
            </div>
          </div>
        </div>

        <div className="border-t border-gray-800 pt-8">
          <div className="flex flex-col md:flex-row justify-between items-center gap-4">
            <div className="flex flex-wrap gap-4 text-sm">
              <Link href="/terms" className="hover:text-white transition">이용약관</Link>
              <span className="text-gray-600">|</span>
              <Link href="/privacy" className="hover:text-white transition font-semibold">개인정보처리방침</Link>
              <span className="text-gray-600">|</span>
              <Link href="/youth-policy" className="hover:text-white transition">청소년보호정책</Link>
              <span className="text-gray-600">|</span>
              <Link href="/viewer-rights" className="hover:text-white transition">시청자권익보호</Link>
            </div>
            <p className="text-sm">
              © finsight 무단 이용 및 재배포 금지 · AI 학습 등 금지
            </p>
          </div>

          <div className="mt-6 text-xs text-gray-500">
            <p>핀사이트(주) | 사업자등록번호 : 000-00-00000 | 통신판매업신고번호 : 0000-서울마포-0000</p>
            <p className="mt-1">
              대표이사: 궁금하면 500원 | 개인정보보호책임자: 궁금하면 500원
            </p>
          </div>
        </div>
      </div>
    </footer>
  )
}
