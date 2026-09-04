import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ boardId: string }> }

export async function POST(req: Request, ctx: Ctx) {
  const { boardId } = await ctx.params
  return mirrorRequestToFinSight(
    req,
    `/api/v1/admin/boards/maintenance/boards/${encodeURIComponent(boardId)}/block`,
  )
}
