import FinsightNewsNav from "@/components/news/FinsightNewsNav"
import StoredNewsDetail from "@/components/news/StoredNewsDetail"
import { VODBannersBar } from "@/components/VODBannersBar"
import "@/styles/finsight-news-nav.css"

type PageProps = { params: Promise<{ id: string }> }

export default async function NewsDetailPage({ params }: PageProps) {
  const { id } = await params
  return (
    <>
      <div className="finsight-news-root bg-white text-[#1e1e1e]">
        <div id="wrap" className="main">
          <FinsightNewsNav />
          <div id="container">
            <div id="content">
              <StoredNewsDetail newsId={id} />
            </div>
          </div>
        </div>
      </div>
      <div className="border-t border-gray-200 bg-finsight-light py-4">
        <VODBannersBar />
      </div>
    </>
  )
}
