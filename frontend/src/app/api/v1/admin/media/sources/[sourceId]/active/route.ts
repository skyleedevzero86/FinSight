import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ sourceId: string }> }

export async function POST(req: Request, ctx: Ctx) {
  const { sourceId } = await ctx.params
  return mirrorRequestToFinSight(req, `/api/v1/admin/media/sources/${sourceId}/active`)
}
