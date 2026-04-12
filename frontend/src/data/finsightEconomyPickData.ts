export type WatchlistRow =
  | { kind: "splitter"; title: string }
  | {
      kind: "stock"
      name: string
      symbol: string
      price: string
      changePct: string
      direction: "up" | "down" | "zero"
      logo: string
    }

export const WATCHLIST_ROWS: WatchlistRow[] = [
  { kind: "splitter", title: "국내종목" },
  {
    kind: "stock",
    name: "알테오젠",
    symbol: "196170",
    price: "363,500",
    changePct: "-1.76%",
    direction: "down",
    logo: "https://file.alphasquare.co.kr/media/images/stock_logo/kr/196170.png",
  },
  {
    kind: "stock",
    name: "에코프로",
    symbol: "086520",
    price: "148,100",
    changePct: "-0.54%",
    direction: "down",
    logo: "https://file.alphasquare.co.kr/media/images/stock_logo/kr/086520.png",
  },
  {
    kind: "stock",
    name: "두산에너빌리티",
    symbol: "034020",
    price: "100,000",
    changePct: "0.00%",
    direction: "zero",
    logo: "https://file.alphasquare.co.kr/media/images/stock_logo/4b441c1796704403bcd5b81e2c674b25.png",
  },
  {
    kind: "stock",
    name: "삼성전자",
    symbol: "005930",
    price: "207,000",
    changePct: "+1.47%",
    direction: "up",
    logo: "https://file.alphasquare.co.kr/media/images/stock_logo/kr/005930.png",
  },
  { kind: "splitter", title: "해외종목" },
  {
    kind: "stock",
    name: "애플",
    symbol: "AAPL",
    price: "$261.51",
    changePct: "+0.39%",
    direction: "up",
    logo: "https://file.alphasquare.co.kr/media/images/stock_logo/us/AAPL.png",
  },
  {
    kind: "stock",
    name: "엔비디아",
    symbol: "NVDA",
    price: "$187.54",
    changePct: "+1.97%",
    direction: "up",
    logo: "https://file.alphasquare.co.kr/media/images/stock_logo/us/NVDA.png",
  },
  { kind: "splitter", title: "가상화폐" },
  {
    kind: "stock",
    name: "비트코인",
    symbol: "KRW-BTC",
    price: "108,381,000",
    changePct: "+1.64%",
    direction: "up",
    logo: "https://file.alphasquare.co.kr/media/images/stock_logo/crypto/BTC.png",
  },
]

export const TICKER_ITEMS: {
  name: string
  price: string
  chg: string
  direction: "up" | "down"
  logo: string
}[] = [
  {
    name: "코스피",
    price: "5,858.87",
    chg: "+1.40%",
    direction: "up",
    logo: "https://file.alphasquare.co.kr/media/images/stock_logo/b04467a3cbf849518915c4df6fd4f480.png",
  },
  {
    name: "코스닥",
    price: "1,093.63",
    chg: "+1.64%",
    direction: "up",
    logo: "https://file.alphasquare.co.kr/media/images/stock_logo/420ac18415094bc481def17a0cff03e0.png",
  },
  {
    name: "다우",
    price: "48,077.05",
    chg: "-0.23%",
    direction: "down",
    logo: "https://file.alphasquare.co.kr/media/images/stock_logo/flag/US.png",
  },
  {
    name: "S&P500",
    price: "6,836.84",
    chg: "+0.18%",
    direction: "up",
    logo: "https://file.alphasquare.co.kr/media/images/stock_logo/flag/US.png",
  },
  {
    name: "나스닥",
    price: "22,958.91",
    chg: "+0.60%",
    direction: "up",
    logo: "https://file.alphasquare.co.kr/media/images/stock_logo/flag/US.png",
  },
  {
    name: "미국 USD",
    price: "1,480.68",
    chg: "+0.41%",
    direction: "up",
    logo: "https://file.alphasquare.co.kr/media/images/stock_logo/flag/USD_KRW.png",
  },
  {
    name: "금",
    price: "$4,787.44",
    chg: "+0.44%",
    direction: "up",
    logo: "https://file.alphasquare.co.kr/media/images/stock_logo/flag/XAU_USD.png",
  },
]

export const THEME_PILLS = [
  "스마트홈",
  "차세대이동통신",
  "자율주행",
  "OLED",
  "HBM",
  "데이터센터",
  "엔비디아",
  "메모리반도체",
  "시스템반도체",
  "온디바이스AI",
]

export const MAIN_TABS = [
  { label: "시장정보", href: "#", active: false },
  { label: "종목분석", href: "#", active: true },
  { label: "종목발굴", href: "#", active: false },
  { label: "커뮤니티", href: "#", active: false },
  { label: "트레이딩", href: "#", active: false },
]

export const SUB_TABS_STOCK = [
  { label: "종목정보", active: true },
  { label: "지표분석", active: false },
  { label: "AI예측", active: false },
]

export const COMPANY_INFO_ROWS: [string, string][][] = [
  [
    ["시가총액", "1,219조 4,454억"],
    ["기업순위", "코스피 1위"],
  ],
  [
    ["주식수", "5,919,637,922주"],
    ["외국인비중", "48.64%"],
  ],
  [
    ["산업군", "하드웨어/IT장비"],
    ["세부산업군", "반도체/반도체장비"],
  ],
  [
    ["52주 최저", "53,700"],
    ["52주 최고", "223,000"],
  ],
]

export const INVESTOR_ROWS: [string, string, string, string][] = [
  ["2026.04.10.", "-1,938,572", "-475,614", "+465,171"],
  ["2026.04.09.", "+501,344", "-13,418,579", "+11,077,050"],
  ["2026.04.08.", "-7,765,652", "+3,775,431", "+2,338,874"],
]

export const NEWS_ITEMS: { title: string; source: string; time: string; href: string; thumb?: string }[] = [
  {
    title: '쿠팡, 미국 쿠팡Inc에 1.4조 중간배당..."대만 로켓배송 투자 활용"',
    source: "머니투데이",
    time: "5시간 전",
    href: "https://www.mt.co.kr/living/2026/04/10/2026041017383714142",
    thumb: "https://thumb.mt.co.kr/cdn-cgi/image/f=avif/21/2026/04/2026041017383714142_1.jpg",
  },
  {
    title: "삼성 보험주 급등에 오너가 지분 7조 돌파…상속세 부담도 ‘상쇄’",
    source: "CEO스코어데일리",
    time: "6시간 전",
    href: "https://www.ceoscoredaily.com/page/view/2026040910353373366",
    thumb: "https://www.ceoscoredaily.com/photos/2026/04/10/2026041010521563958_l.jpg",
  },
  {
    title: "[카드뉴스] 삼성전자, 베트남에 6조 원 규모 ‘반도체 패키징’ 거점 구축...",
    source: "인포스탁데일리",
    time: "6시간 전",
    href: "https://www.infostockdaily.co.kr/news/articleView.html?idxno=215275",
  },
  {
    title: "[0410마감체크] 한국은행, 기준금리 연 2.50%로 7회 연속 동결",
    source: "인포스탁데일리",
    time: "6시간 전",
    href: "https://www.infostockdaily.co.kr/news/articleView.html?idxno=215273",
  },
]

export const SALES_COMPOSITION: { key: string; value: string; sign: "plus" | "minus" }[] = [
  { key: "TV, 모니터… [DX부문]", value: "65.70%", sign: "plus" },
  { key: "DRAM, NAND… [DS부문]", value: "25.70%", sign: "plus" },
  { key: "스마트폰용OLED… [SDC]", value: "12.00%", sign: "plus" },
  { key: "부문간내부거래제거 [기타]", value: "-9.00%", sign: "minus" },
]
