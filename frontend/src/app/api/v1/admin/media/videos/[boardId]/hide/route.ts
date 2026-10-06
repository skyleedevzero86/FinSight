import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ boardId: string }> }

export async function POST(req: Request, ctx: Ctx) {
  const { boardId } = await ctx.params
  return mirrorRequestToFinSight(req, `/api/v1/admin/media/videos/${boardId}/hide`)
}
