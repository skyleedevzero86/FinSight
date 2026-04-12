import Link from "next/link"
import { COMMUNITY_NAV, type CommunityNavKey } from "@/data/communityBoardData"

type CommunityBoardLayoutProps = {
  active: CommunityNavKey
  heading: string
  description: string
  children: React.ReactNode
}

function heroTitleForActive(active: CommunityNavKey): string {
  const found = COMMUNITY_NAV.find((n) => n.key === active)
  return found?.label ?? "커뮤니티"
}

export default function CommunityBoardLayout({
  active,
  heading,
  description,
  children,
}: CommunityBoardLayoutProps) {
  const heroTitle = heroTitleForActive(active)

  return (
    <div className="fcb-page">
      <div className="fcb-vis">
        <div className="fcb-titler">
          <h1 className="fcb-leader" key={active}>
            {heroTitle}
          </h1>
          <div className="fcb-menutab">
            <div className="fcb-dep-wrap">
              <div className="fcb-dep">
                <ul>
                  {COMMUNITY_NAV.map((item) => (
                    <li
                      key={item.key}
                      className={active === item.key ? "fcb-on" : undefined}
                    >
                      <Link href={item.href}>{item.label}</Link>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="fcb-main">
        <div className="fcb-container">
          <header className="fcb-heading">
            <h2>{heading}</h2>
            <p className="fcb-description">{description}</p>
          </header>
          {children}
        </div>
      </div>
    </div>
  )
}
