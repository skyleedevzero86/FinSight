import { redirect } from "next/navigation"

type Props = { params: Promise<{ id: string }> }

export default async function CommunityFreeEditRedirect({ params }: Props) {
  await params
  redirect("/community/free")
}
