export type CommunityNavKey = "home" | "notice" | "free" | "qna"

export const COMMUNITY_NAV = [
  { key: "home" as const, label: "커뮤니티", href: "/community" },
  { key: "notice" as const, label: "공지사항", href: "/community/notice" },
  { key: "free" as const, label: "포트폴리오 공유", href: "/community/free" },
  { key: "qna" as const, label: "Q&A", href: "/community/qna" },
]

export type BoardRow = {
  num: number | "pin"
  title: string
  href: string
  author: string
  date: string
  hits: number
  hasFile?: boolean
}

export const NOTICE_CATEGORY_TABS = [
  { label: "전체", href: "/community/notice" },
  { label: "공지", href: "/community/notice?cate=notice" },
  { label: "채용", href: "/community/notice?cate=hire" },
  { label: "기타", href: "/community/notice?cate=etc" },
]

export const MOCK_NOTICE_ROWS: BoardRow[] = [
  {
    num: "pin",
    title: "[안내] finsight 커뮤니티 이용 규칙 및 개인정보 보호 안내",
    href: "#",
    author: "운영자",
    date: "26.04.01",
    hits: 1204,
  },
  {
    num: 48,
    title: "[공지] 2026년 상반기 서비스 점검 일정 안내",
    href: "#",
    author: "운영자",
    date: "26.04.08",
    hits: 892,
    hasFile: true,
  },
  {
    num: 47,
    title: "[채용] 콘텐츠 에디터 채용 공고",
    href: "#",
    author: "운영자",
    date: "26.04.05",
    hits: 641,
  },
  {
    num: 46,
    title: "뉴스 클립 재생 오류 관련 패치 안내",
    href: "#",
    author: "운영자",
    date: "26.04.02",
    hits: 533,
  },
  {
    num: 45,
    title: "모바일 앱 푸시 알림 설정 방법",
    href: "#",
    author: "운영자",
    date: "26.03.28",
    hits: 1201,
  },
]

export const MOCK_COMMUNITY_HOME_ROWS: BoardRow[] = [
  {
    num: "pin",
    title: "[안내] 커뮤니티 운영 원칙 및 글쓰기 가이드",
    href: "#",
    author: "운영자",
    date: "26.04.10",
    hits: 2103,
  },
  {
    num: 24,
    title: "이번 주 인기 토픽: 금리 동향과 시장 이야기",
    href: "#",
    author: "모더레이터",
    date: "26.04.09",
    hits: 412,
  },
  {
    num: 23,
    title: "신규 회원 환영합니다 — 자기소개는 여기로 모여요",
    href: "#",
    author: "운영자",
    date: "26.04.08",
    hits: 891,
    hasFile: true,
  },
  {
    num: 22,
    title: "건전한 토론을 위한 신고·문의 안내",
    href: "#",
    author: "운영자",
    date: "26.04.05",
    hits: 356,
  },
]

export const MOCK_FREE_ROWS: BoardRow[] = [
  {
    num: 128,
    title: "경제 뉴스만 봐도 투자 공부가 될까요?",
    href: "#",
    author: "청년투자러",
    date: "26.04.09",
    hits: 88,
  },
  {
    num: 127,
    title: "첫 직장 연봉 협상 팁 나눠요",
    href: "#",
    author: "취준생A",
    date: "26.04.08",
    hits: 156,
    hasFile: true,
  },
  {
    num: 126,
    title: "오늘 본 VOD 중 추천작 있으신가요",
    href: "#",
    author: "vod러버",
    date: "26.04.07",
    hits: 64,
  },
]

export const MOCK_QNA_ROWS: BoardRow[] = [
  {
    num: 56,
    title: "회원가입 인증 메일이 오지 않아요",
    href: "#",
    author: "guest12",
    date: "26.04.09",
    hits: 34,
  },
  {
    num: 55,
    title: "경제 Pick 관심목록은 몇 개까지 저장되나요?",
    href: "#",
    author: "pick유저",
    date: "26.04.08",
    hits: 72,
  },
  {
    num: 54,
    title: "실시간 VOD 품질(화질) 변경 방법",
    href: "#",
    author: "streamer",
    date: "26.04.06",
    hits: 91,
  },
]
