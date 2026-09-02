type CommunityBoardLayoutProps = {
  heading: string
  description: string
  children: React.ReactNode
}

export default function CommunityBoardLayout({
  heading,
  description,
  children,
}: CommunityBoardLayoutProps) {
  return (
    <>
      <header className="fcb-heading">
        <h2>{heading}</h2>
        <p className="fcb-description">{description}</p>
      </header>
      {children}
    </>
  )
}
