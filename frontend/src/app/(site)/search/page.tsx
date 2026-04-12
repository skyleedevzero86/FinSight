import Link from "next/link"

type Props = {
  searchParams: Promise<{ q?: string }>
}

export default async function SearchPage({ searchParams }: Props) {
  const { q } = await searchParams
  const query = (q ?? "").trim()

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-12">
      <h1 className="text-xl font-bold text-gray-900">검색</h1>
      {query ? (
        <p className="mt-4 text-gray-600">
          <span className="font-medium text-gray-900">「{query}」</span>에 대한 결과는 준비 중입니다.
        </p>
      ) : (
        <p className="mt-4 text-gray-600">검색어를 입력해 주세요.</p>
      )}
      <Link href="/" className="mt-8 inline-block text-finsight-secondary hover:underline">
        홈으로
      </Link>
    </div>
  )
}
