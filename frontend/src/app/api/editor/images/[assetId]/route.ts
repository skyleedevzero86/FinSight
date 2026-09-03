import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

export async function GET(
  req: Request,
  ctx: { params: Promise<{ assetId: string }> },
) {
  const { assetId } = await ctx.params
  return mirrorRequestToFinSight(req, `/api/editor/images/${encodeURIComponent(assetId)}`)
}
