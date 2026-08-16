import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ userId: string }> }

export async function POST(req: Request, ctx: Ctx) {
  const { userId } = await ctx.params
  return mirrorRequestToFinSight(req, `/api/v1/admin/users/${userId}/restore`)
}
