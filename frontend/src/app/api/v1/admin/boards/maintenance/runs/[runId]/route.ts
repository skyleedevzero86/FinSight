import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ runId: string }> }

export async function GET(req: Request, ctx: Ctx) {
  const { runId } = await ctx.params
  return mirrorRequestToFinSight(
    req,
    `/api/v1/admin/boards/maintenance/runs/${encodeURIComponent(runId)}`,
  )
}
