const IMG = (path: string) => `https://image.imnews.imbc.com${path}`

export const MAIN_HERO = {
  href: "#",
  title: '[단독] 경찰, 삼립 시화공장 직원 진술 확보‥"전원 내리지 않고 작업"',
  summary:
    "삼립 시화공장에서 노동자 2명의 손가락이 절단되는 사고가 또 발생한 가운데, 경찰이 이번 사고 당시 생산 설비를 멈추지 않은 채 수리 작업이 진행된 정황을 포착해 수사 중인 것으로 확인됐습니다.",
  image: IMG("/news/2026/society/article/__icsFiles/afieldfile/2026/04/10/yh_20260410-10.jpg"),
  alt: "삼립 시화공장 관련 보도",
}

export const MAIN_RECOMMENDED: { href: string; title: string }[] = [
  { href: "#", title: '위성락 "호르무즈 통항 원활치 않다‥공급망 불확실성 지속"' },
  { href: "#", title: "여야, '고유가 피해지원금' 포함 26조 2천억 원 추경 합의" },
  { href: "#", title: '트럼프 "민주당에 권력 주면‥", 잔혹 영상 올리며 \'벌컥\'' },
  { href: "#", title: "신임 독립기념관장에 김희곤‥김형석 '광복절 기념사 논란' 8개월만" },
]

export type PanItem = { href: string; title: string; image: string; alt: string; vod?: boolean }

export const AI_PICK_PANS: PanItem[][] = [
  [
    {
      href: "#",
      title: "[날씨] 제주·남해안 강한 비바람 계속‥내일 낮 전국 대부분 그쳐",
      image: IMG("/replay/2026/nwdesk/article/__icsFiles/afieldfile/2026/04/09/desk_20260409_205049_1_31_Large.jpg"),
      alt: "",
      vod: true,
    },
    {
      href: "#",
      title: "K9 자주포, 핀란드에 112문 추가 수출‥9천4백억 원 규모",
      image: IMG("/replay/2026/nwdesk/article/__icsFiles/afieldfile/2026/04/09/desk_20260409_203933_1_26_Large.jpg"),
      alt: "",
      vod: true,
    },
    {
      href: "#",
      title: "고교 최대어 엄준상 앞세운 덕수고 결승행‥야탑고와 우승 다툰다",
      image: IMG("/news/2026/sports/article/__icsFiles/afieldfile/2026/04/10/yh_20260410-14.jpg"),
      alt: "",
    },
  ],
  [
    {
      href: "#",
      title: '한국배구연맹, 현대캐피탈 블랑 감독에 "부적절 언행"',
      image: IMG("/news/2026/sports/article/__icsFiles/afieldfile/2026/04/09/hye_20260409_18_1.jpg"),
      alt: "",
    },
    {
      href: "#",
      title: "'비디오 판독' 이후 분노가 자극제로‥확률 '100% vs 0%'",
      image: IMG("/replay/2026/nwdesk/article/__icsFiles/afieldfile/2026/04/09/desk_20260409_204559_1_28_Large.jpg"),
      alt: "",
      vod: true,
    },
    {
      href: "#",
      title: '민주노총 "피지컬AI 우려"‥이 대통령 "너무 공포 가질 필요없어"',
      image: IMG("/news/2026/politics/article/__icsFiles/afieldfile/2026/04/10/lyj_260410_26.jpg"),
      alt: "",
    },
  ],
  [
    {
      href: "#",
      title: "뉴스데스크 클로징",
      image: IMG("/replay/2026/nwdesk/article/__icsFiles/afieldfile/2026/04/09/desk_20260409_205119_1_32_Large.jpg"),
      alt: "",
      vod: true,
    },
    {
      href: "#",
      title: "[와글와글] 갑자기 방향 틀더니 '붕' 날아오른 보트‥무슨 일?",
      image: IMG("/replay/2026/nwtoday/article/__icsFiles/afieldfile/2026/04/10/today_20260410_064448_2_10_Large.jpg"),
      alt: "",
      vod: true,
    },
    {
      href: "#",
      title: "호르무즈에 '트럼프 요금소'?‥트럼프 \"'합작 사업'은 아름다운 일\"",
      image: IMG("/replay/2026/nwdesk/article/__icsFiles/afieldfile/2026/04/09/desk_20260409_195911_1_6_Large.jpg"),
      alt: "",
      vod: true,
    },
  ],
  [
    {
      href: "#",
      title: "아시아개발은행, 올해 한국 성장률 1.9%로 0.2%p 상향 전망",
      image: IMG("/news/2026/econo/article/__icsFiles/afieldfile/2026/04/10/ggm_20260410_6.jpg"),
      alt: "",
    },
    {
      href: "#",
      title: '\'전재수 불기소\'에 국힘 긴급 최고위 "부산 시민들이 심판해줄 것"',
      image: IMG("/news/2026/politics/article/__icsFiles/afieldfile/2026/04/10/lyj_260410_17.jpg"),
      alt: "",
    },
    {
      href: "#",
      title: "네이버 AI 검색 '클로바X' 2년 8개월 만에 종료‥\"AI탭 중심 재편\"",
      image: IMG("/news/2026/econo/article/__icsFiles/afieldfile/2026/04/09/hye_20260409_14.jpg"),
      alt: "",
    },
  ],
]

export const AI_PICK_BOTTOM_PANS: PanItem[][] = [
  [
    {
      href: "#",
      title: "'종전 협상' D-1‥\"이란 대표단, 이슬라마바드 도착\"",
      image: IMG("/replay/2026/nw1200/article/__icsFiles/afieldfile/2026/04/10/noon_20260410_120448_1_3_Large.jpg"),
      alt: "",
      vod: true,
    },
    {
      href: "#",
      title: '"종전 협상 매우 낙관‥레바논 공격 자제할 것"',
      image: IMG("/replay/2026/nw1200/article/__icsFiles/afieldfile/2026/04/10/noon_20260410_120206_1_1_Large.jpg"),
      alt: "",
      vod: true,
    },
    {
      href: "#",
      title: '"이란 대표단, 美 대면 협상 앞두고 이슬라마바드 도착"',
      image: IMG("/news/2026/world/article/__icsFiles/afieldfile/2026/04/10/chhh_20260410_13.jpg"),
      alt: "",
    },
  ],
  [
    {
      href: "#",
      title: '트럼프, 이번엔 "이란 통행료 부과, 지금 중단하는 게 좋을 것"',
      image: IMG("/news/2026/world/article/__icsFiles/afieldfile/2026/04/10/kds_20260410_18.jpg"),
      alt: "",
    },
    {
      href: "#",
      title: "이 대통령 \"전시 살해, 유대인 학살과 다를 바 없어‥인권은 최우선 가치\"",
      image: IMG("/news/2026/politics/article/__icsFiles/afieldfile/2026/04/10/kds_20260410_41.jpg"),
      alt: "",
    },
    {
      href: "#",
      title: "신임 독립기념관장에 김희곤‥김형석 '광복절 기념사 논란' 8개월만",
      image: IMG("/news/2026/politics/article/__icsFiles/afieldfile/2026/04/10/lyj_260410_17.jpg"),
      alt: "",
    },
  ],
  [
    {
      href: "#",
      title: "[날씨] 제주·남해안 강한 비바람 계속‥내일 낮 전국 대부분 그쳐",
      image: IMG("/replay/2026/nwdesk/article/__icsFiles/afieldfile/2026/04/09/desk_20260409_205049_1_31_Large.jpg"),
      alt: "",
      vod: true,
    },
    {
      href: "#",
      title: "K9 자주포, 핀란드에 112문 추가 수출‥9천4백억 원 규모",
      image: IMG("/replay/2026/nwdesk/article/__icsFiles/afieldfile/2026/04/09/desk_20260409_203933_1_26_Large.jpg"),
      alt: "",
      vod: true,
    },
    {
      href: "#",
      title: "네이버 AI 검색 '클로바X' 2년 8개월 만에 종료‥\"AI탭 중심 재편\"",
      image: IMG("/news/2026/econo/article/__icsFiles/afieldfile/2026/04/09/hye_20260409_14.jpg"),
      alt: "",
    },
  ],
]

export const MBIG_PANS: PanItem[][] = [
  [
    {
      href: "#",
      title: "[엠빅뉴스] '유영찬 세이브=LG 승리' KBO 역대 4번째 기록!!!",
      image: IMG("/original/mbig/__icsFiles/afieldfile/2026/04/10/260410_yyc_mbic_thumb_640.jpg"),
      alt: "",
    },
    {
      href: "#",
      title: "[14F] 장시간 비행은 누워서... 2027년에 도입된다는 미 항공사의 '눕코노미'",
      image: IMG("/original/14f/__icsFiles/afieldfile/2026/04/10/g.png"),
      alt: "",
    },
    {
      href: "#",
      title: "[엠빅뉴스] ‘더블 베이글’ 치욕패 메드베데프..라켓은 대체 무슨 죄야...",
      image: IMG("/original/mbig/__icsFiles/afieldfile/2026/04/10/260410_smashracket_mbic_thumb_640.jpg"),
      alt: "",
    },
  ],
  [
    {
      href: "#",
      title: "[엠빅뉴스] 눈물(?) 없인 볼 수 없는 늑구 수색 작전",
      image: IMG("/original/mbig/__icsFiles/afieldfile/2026/04/10/260410_wolfescape_mbic_thumb_640.jpg"),
      alt: "",
    },
    {
      href: "#",
      title: "[14F] 일본인들도 줄 서게 한 한국 저가 커피 브랜드 근황",
      image: IMG("/original/14f/__icsFiles/afieldfile/2026/04/09/g.png"),
      alt: "",
    },
    {
      href: "#",
      title: "[엠빅뉴스] KKKKKKKKKK 공 모두 모아봄! 역시 ABS 조련사! 류현진",
      image: IMG("/original/mbig/__icsFiles/afieldfile/2026/04/08/260408_rhj10K_mbic_thumb_640.jpg"),
      alt: "",
    },
  ],
]

export const MBIG_EXTRA_PANS: PanItem[][] = [
  [
    {
      href: "#",
      title: "여야, '고유가 피해지원금' 포함 26조 2천억 원 추경 합의",
      image: IMG("/news/2026/politics/article/__icsFiles/afieldfile/2026/04/10/kds_20260410_41.jpg"),
      alt: "",
    },
    {
      href: "#",
      title: '트럼프 "민주당에 권력 주면‥", 잔혹 영상 올리며 \'벌컥\'',
      image: IMG("/news/2026/world/article/__icsFiles/afieldfile/2026/04/10/kds_20260410_18.jpg"),
      alt: "",
    },
    {
      href: "#",
      title: "네이버 AI 검색 '클로바X' 2년 8개월 만에 종료‥\"AI탭 중심 재편\"",
      image: IMG("/news/2026/econo/article/__icsFiles/afieldfile/2026/04/09/hye_20260409_14.jpg"),
      alt: "",
    },
  ],
  [
    {
      href: "#",
      title: "심보틱(SYM) — 월마트 물류 자동화 파트너",
      image: IMG("/news/2026/econo/article/__icsFiles/afieldfile/2026/04/10/yh_20260410-14.jpg"),
      alt: "",
    },
    {
      href: "#",
      title: "엔비디아 Q2 호실적 | 미국 Q2 GDP 수정치 3.3%",
      image: IMG("/news/2026/econo/article/__icsFiles/afieldfile/2026/04/10/ggm_20260410_6.jpg"),
      alt: "",
    },
    {
      href: "#",
      title: "이란 2주 휴전 극적 합의 | 호르무즈 통행 중단",
      image: IMG("/replay/2026/nwdesk/article/__icsFiles/afieldfile/2026/04/09/desk_20260409_203933_1_26_Large.jpg"),
      alt: "",
    },
  ],
]

export const TIMELINE_ITEMS: { href: string; title: string; image: string; alt: string; time: string; vod?: boolean }[] =
  [
    {
      href: "#",
      title: "'종전 협상' D-1‥\"이란 대표단, 이슬라마바드 도착\"",
      image: IMG("/replay/2026/nw1200/article/__icsFiles/afieldfile/2026/04/10/noon_20260410_120448_1_3_Large.jpg"),
      alt: "",
      time: "6시간전",
      vod: true,
    },
    {
      href: "#",
      title: '"종전 협상 매우 낙관‥레바논 공격 자제할 것"',
      image: IMG("/replay/2026/nw1200/article/__icsFiles/afieldfile/2026/04/10/noon_20260410_120206_1_1_Large.jpg"),
      alt: "",
      time: "6시간전",
      vod: true,
    },
    {
      href: "#",
      title: '"이란 대표단, 美 대면 협상 앞두고 이슬라마바드 도착"',
      image: IMG("/news/2026/world/article/__icsFiles/afieldfile/2026/04/10/chhh_20260410_13.jpg"),
      alt: "",
      time: "7시간전",
    },
    {
      href: "#",
      title: '트럼프, 이번엔 "이란 통행료 부과, 지금 중단하는 게 좋을 것"',
      image: IMG("/news/2026/world/article/__icsFiles/afieldfile/2026/04/10/kds_20260410_18.jpg"),
      alt: "",
      time: "11시간전",
    },
    {
      href: "#",
      title: '트럼프, \'이란전 반대\' 보수 논객들 겨냥 "멍청한 패배자들"',
      image: IMG("/news/2026/world/article/__icsFiles/afieldfile/2026/04/10/kds_20260410_14.jpg"),
      alt: "",
      time: "12시간전",
    },
  ]

export const HIT_NEWS_TOP = {
  href: "#",
  title: '이 대통령 "전시 살해, 유대인 학살과 다를 바 없어‥인권은 최우선 가치"',
  image: IMG("/news/2026/politics/article/__icsFiles/afieldfile/2026/04/10/kds_20260410_41.jpg"),
  alt: "",
}

export const HIT_NEWS_LIST: { href: string; num: number; title: string }[] = [
  { href: "#", num: 2, title: "신임 독립기념관장에 김희곤‥김형석 '광복절 기념사 논란' 8개월만" },
  { href: "#", num: 3, title: '트럼프 "민주당에 권력 주면‥", 잔혹 영상 올리며 \'벌컥\'' },
  { href: "#", num: 4, title: "\"엡스타인 엮지 마\" 급발진‥영부인 돌발 백악관 '멘붕'" },
  { href: "#", num: 5, title: "여야, '고유가 피해지원금' 포함 26조 2천억 원 추경 합의" },
  { href: "#", num: 6, title: "이상민 재판 나온 尹, '언론사 단전단수' 질문에 코웃음" },
]

export const PLUS_BANNER_PANS: { href: string; image: string; alt: string; text: string }[][] = [
  [
    {
      href: "#",
      image: IMG("/replay/2026/nwdesk/article/__icsFiles/afieldfile/2026/03/22/desk_20260322_202420_1_13_Large.jpg"),
      alt: "바로간다",
      text: "[바로간다] 사육곰 산업 끝났다는데‥절에 갇힌 '곰 세마리' 왜?",
    },
    {
      href: "#",
      image: IMG("/replay/2026/nwdesk/article/__icsFiles/afieldfile/2026/03/11/desk_20260311_202400_1_17_Large_u.jpg"),
      alt: "바로간다",
      text: "[바로간다] 따릉이 밀어낸 킥보드의 '탄소 발자국'‥버스보다 더 뿜는다?",
    },
  ],
  [
    {
      href: "#",
      image: IMG("/news/2026/society/article/__icsFiles/afieldfile/2026/03/06/kds_20260306_32.jpg"),
      alt: "서초동M본부",
      text: '"관봉권 띠지 분실, 단순 과오"라면서 불기소 처분은 검찰 몫으로? [서초동M본부]',
    },
    {
      href: "#",
      image: IMG("/news/2026/society/article/__icsFiles/afieldfile/2026/03/05/joo260305_16.jpg"),
      alt: "서초동M본부",
      text: '대법원, 재판소원 현실화에 \'먼산\'‥"시행되면 준비"? [서초동M본부]',
    },
  ],
]

export const WEEKLY_ITEMS: {
  href: string
  title: string
  image: string
  alt: string
  channel: string
  channelImg: string
}[] = [
  {
    href: "#",
    title: "[스트레이트] 말로만 '절윤'‥도로 '윤어게인'",
    image: IMG("/replay/straight/__icsFiles/afieldfile/2026/03/29/straight_20260329_211212_1_2_Large.jpg"),
    alt: "",
    channel: "스트레이트",
    channelImg: IMG("/operate/common/main/weekly/__icsFiles/afieldfile/2019/11/20/straight.png"),
  },
  {
    href: "#",
    title: "[스트레이트] 유가폭등‥'기름값'의 비밀",
    image: IMG("/replay/straight/__icsFiles/afieldfile/2026/03/29/ljm_20260329_21.jpg"),
    alt: "",
    channel: "스트레이트",
    channelImg: IMG("/operate/common/main/weekly/__icsFiles/afieldfile/2019/11/20/straight.png"),
  },
  {
    href: "#",
    title: "[스트레이트 예고] 유가폭등‥'기름값'의 비밀 / 말로만 '절윤'‥도로 '윤어게인'",
    image: IMG("/replay/straight/__icsFiles/afieldfile/2026/03/29/jhp_20260329_9.jpg"),
    alt: "",
    channel: "스트레이트",
    channelImg: IMG("/operate/common/main/weekly/__icsFiles/afieldfile/2019/11/20/straight.png"),
  },
  {
    href: "#",
    title: "[스트레이트] '촉법소년' 진짜 해법은?",
    image: IMG("/replay/straight/__icsFiles/afieldfile/2026/03/22/straight_20260322_211123_1_2_Large.jpg"),
    alt: "",
    channel: "스트레이트",
    channelImg: IMG("/operate/common/main/weekly/__icsFiles/afieldfile/2019/11/20/straight.png"),
  },
  {
    href: "#",
    title: "[스트레이트] '4·3'과 빼앗긴 '이름'",
    image: IMG("/replay/straight/__icsFiles/afieldfile/2026/03/22/straight_20260322_205405_1_1_Large.jpg"),
    alt: "",
    channel: "스트레이트",
    channelImg: IMG("/operate/common/main/weekly/__icsFiles/afieldfile/2019/11/20/straight.png"),
  },
]

export const ONAIR_BLOCK = {
  program: "SPY의 소식은..",
  liveLabel: "핫 이슈",
  image: IMG("/operate/common/category/vod/nwdesk/__icsFiles/afieldfile/2026/02/26/400x226.png"),
  alt: "프로그램 이미지",
}

export const ONAIR_BLOCK_META = {
  program: "메타에서....",
  liveLabel: "가장 이슈",
  image: IMG("/operate/common/category/vod/nwdesk/__icsFiles/afieldfile/2026/02/26/400x226.png"),
  alt: "프로그램 이미지",
}
