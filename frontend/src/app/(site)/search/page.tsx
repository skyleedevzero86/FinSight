type Props = {
  searchParams: Promise<{ q?: string }>
}

export default async function SearchPage({ searchParams }: Props) {
  const { q } = await searchParams
  const query = (q ?? "").trim()

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-12">
      <h1 className="text-2xl font-bold text-[#231f20]">검색</h1>
      <form action="/search" className="mt-6 flex border border-finsight-secondary bg-white">
        <input
          type="search"
          name="q"
          defaultValue={query}
          placeholder="검색어를 입력해주세요"
          className="min-w-0 flex-1 border-0 bg-transparent px-4 py-3 text-[17px] text-gray-900 outline-none placeholder:text-gray-400"
          autoComplete="off"
        />
        <button
          type="submit"
          className="px-4 text-gray-900 hover:text-finsight-secondary"
          aria-label="검색"
        >
          검색
        </button>
      </form>
      {query ? (
        <p className="mt-6 text-gray-600">
          <span className="font-medium text-[#231f20]">「{query}」</span> 검색 결과는 이어서 연결합니다.
        </p>
      ) : (
        <p className="mt-6 text-gray-600">검색어를 입력해 주세요.</p>
      )}
    </div>
  )
}
