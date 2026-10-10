import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ shareId: string }> }

export async function GET(req: Request, ctx: Ctx) {
  const { shareId } = await ctx.params
  return mirrorRequestToFinSight(req, `/api/v1/portfolio/shares/${shareId}/comments`)
}

export async function POST(req: Request, ctx: Ctx) {
  const { shareId } = await ctx.params
  return mirrorRequestToFinSight(req, `/api/v1/portfolio/shares/${shareId}/comments`)
}
