import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ notificationId: string }> }

export async function DELETE(req: Request, ctx: Ctx) {
  const { notificationId } = await ctx.params
  return mirrorRequestToFinSight(req, `/api/v1/inbox/${notificationId}`)
}
