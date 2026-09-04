import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ id: string }> }

export async function GET(req: Request, ctx: Ctx) {
  const { id } = await ctx.params
  return mirrorRequestToFinSight(req, `/api/v1/ulink/items/${encodeURIComponent(id)}`)
}

export async function PUT(req: Request, ctx: Ctx) {
  const { id } = await ctx.params
  return mirrorRequestToFinSight(req, `/api/v1/ulink/items/${encodeURIComponent(id)}`)
}

export async function DELETE(req: Request, ctx: Ctx) {
  const { id } = await ctx.params
  return mirrorRequestToFinSight(req, `/api/v1/ulink/items/${encodeURIComponent(id)}`)
}
