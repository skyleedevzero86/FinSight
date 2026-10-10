import SearchResults from "@/components/search/SearchResults"

type Props = {
  searchParams: Promise<{ q?: string }>
}

export default async function SearchPage({ searchParams }: Props) {
  const { q } = await searchParams
  return <SearchResults query={(q ?? "").trim()} />
}
