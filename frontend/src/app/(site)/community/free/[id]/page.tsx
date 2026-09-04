import { redirect } from "next/navigation"

type Props = { params: Promise<{ id: string }> }

export default async function CommunityFreeDetailRedirect({ params }: Props) {
  await params
  redirect("/community/free")
}
