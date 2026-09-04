import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ userId: string }> }

export async function POST(req: Request, ctx: Ctx) {
  const { userId } = await ctx.params
  const url = new URL(req.url)
  const qs = url.searchParams.toString()
  return mirrorRequestToFinSight(
    req,
    `/api/v1/admin/users/${encodeURIComponent(userId)}/role${qs ? `?${qs}` : ""}`,
  )
}
