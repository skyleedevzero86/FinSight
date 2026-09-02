export default function CommunityLoading() {
  return (
    <div className="fcb-loading" aria-busy="true" aria-label="게시판 불러오는 중">
      <div className="fcb-heading">
        <div className="fcb-skel fcb-skel-title" />
        <div className="fcb-skel fcb-skel-desc" />
      </div>
      <div className="fcb-skel-table">
        {Array.from({ length: 8 }).map((_, i) => (
          <div key={i} className="fcb-skel fcb-skel-row" />
        ))}
      </div>
    </div>
  )
}
