import { proxyJsonToFinSight, mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

export async function GET(
  req: Request,
  ctx: { params: Promise<{ provider: string }> },
) {
  const { provider } = await ctx.params
  const url = new URL(req.url)
  return mirrorRequestToFinSight(
    req,
    `/api/v1/auth/oauth/${encodeURIComponent(provider)}/url${url.search}`,
  )
}
