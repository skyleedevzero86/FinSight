import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ boardId: string }> }

export async function GET(req: Request, ctx: Ctx) {
  const { boardId } = await ctx.params
  return mirrorRequestToFinSight(req, `/api/v1/admin/media/videos/${boardId}`)
}
