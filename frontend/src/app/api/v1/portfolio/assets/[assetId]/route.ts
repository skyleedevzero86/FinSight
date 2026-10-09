import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ assetId: string }> }

export async function DELETE(req: Request, ctx: Ctx) {
  const { assetId } = await ctx.params
  return mirrorRequestToFinSight(req, `/api/v1/portfolio/assets/${assetId}`)
}
