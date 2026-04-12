export type NewsCategoryArticle = {
  id: string
  href: string
  title: string
  summary: string
  reporter: string
  thumb: string
  alt: string
  isVod?: boolean
}

export const NEWS_CATEGORY_TOP: NewsCategoryArticle[] = [
  {
    id: "t1",
    href: "#",
    title: '이창용 "현 시점에서 스태그플레이션 발생 가능성 적어"',
    summary:
      '이창용 한국은행 총재가 현 시점에서는 스태그플레이션이 발생할 가능성이 적다고 밝혔습니다. 이 총재는 오늘 한은 금융통화위원회 이후 기자간담회에서 "지금 이란 사태가 종결되면 스태그플레이션이 올 것이냐, 그러면 그럴 가능성이 적다고 말씀드릴 것 같다"고 말했습니다. 그렇...',
    reporter: "남효정",
    thumb: "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?w=640&h=360&fit=crop",
    alt: "이창용 총재 관련 기사",
  },
  {
    id: "t2",
    href: "#",
    title: '"중동발 물가·성장 불안"‥기준금리 7회 연속 동결',
    summary:
      "한국은행이 기준금리를 2.50%로 동결하기로 했습니다. 고유가로 물가 상승이 우려되는 한편 성장률 전망도 낮아지는 진퇴양난의 상황에서 현재 금리 수준을 유지하면서 추이를 지켜보는 게 맞다고 판단했습니다.",
    reporter: "남효정",
    thumb: "https://images.unsplash.com/photo-1590283603385-17ffb3a7f29f?w=640&h=360&fit=crop",
    alt: "기준금리",
    isVod: true,
  },
  {
    id: "t3",
    href: "#",
    title: '"AI 잘 쓰는 나라로 도약할 때" 한경협 AI혁신위 3차 회의 개최',
    summary:
      "한국경제인협회가 국내 기업들의 AI전환시 고충을 해결하고 경쟁력 강화 방안을 모색하기 위해 오늘 오후 'AI혁신위원회' 3차 회의를 개최했습니다.",
    reporter: "오해정",
    thumb: "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=640&h=360&fit=crop",
    alt: "AI 경제",
  },
  {
    id: "t4",
    href: "#",
    title: "오늘의 증시",
    summary: "오늘의 증시",
    reporter: "정다인/삼성증권",
    thumb: "https://images.unsplash.com/photo-1642790106117-e829e14a795f?w=640&h=360&fit=crop",
    alt: "증시",
    isVod: true,
  },
  {
    id: "t5",
    href: "#",
    title: "석유 최고가 '동결'에도‥기름값 상승세 '여전'",
    summary:
      "정부가 오늘부터 적용되는 3차 석유 최고가격을 2차 때와 동일하게 유지하기로 했습니다. 하지만 주유소의 휘발유와 경유 가격의 오름세는 오늘도 이어지고 있습니다.",
    reporter: "이지수",
    thumb: "https://images.unsplash.com/photo-1581094794329-c8112a89af12?w=640&h=360&fit=crop",
    alt: "기름값",
    isVod: true,
  },
]

export const NEWS_CATEGORY_BOTTOM: NewsCategoryArticle[] = [
  {
    id: "b1",
    href: "#",
    title: '"중동발 물가·성장 불안"‥기준금리 7연속 동결',
    summary: "한국은행이 기준금리를 2.50%로 동결하기로 했습니다.",
    reporter: "남효정",
    thumb: "https://images.unsplash.com/photo-1590283603385-17ffb3a7f29f?w=640&h=360&fit=crop",
    alt: "",
    isVod: true,
  },
  {
    id: "b2",
    href: "#",
    title: '이창용 "공급 충격 일시적일 때 금리로 대응하지 않는 게 바람직"',
    summary:
      '이창용 한국은행 총재가 "공급 충격이 일시적일 경우 정책 시차 등을 고려할 때 금리 조정으로 대응하지 않는 것이 바람직하다"고 밝혔습니다.',
    reporter: "남효정",
    thumb: "https://images.unsplash.com/photo-1563986768609-322da13575f3?w=640&h=360&fit=crop",
    alt: "",
  },
  {
    id: "b3",
    href: "#",
    title: "3차 최고가격제 첫날 상승세 이어져‥오름폭 둔화",
    summary:
      "정부가 3차 석유 최고가격제를 동결했지만, 전국 주유소 기름값은 상승세를 이어갔습니다.",
    reporter: "송재원",
    thumb: "https://images.unsplash.com/photo-1581094794329-c8112a89af12?w=640&h=360&fit=crop",
    alt: "",
  },
  {
    id: "b4",
    href: "#",
    title: "아시아개발은행, 올해 한국 성장률 1.9%로 0.2%p 상향 전망",
    summary: "아시아개발은행은 올해 한국 경제가 1.9% 성장할 것으로 전망했습니다.",
    reporter: "김건휘",
    thumb: "https://images.unsplash.com/photo-1526304640581-d334cdbbf45e?w=640&h=360&fit=crop",
    alt: "",
  },
]
