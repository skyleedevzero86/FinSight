import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

export async function DELETE(
  req: Request,
  ctx: { params: Promise<{ commentId: string }> },
) {
  const { commentId } = await ctx.params
  return mirrorRequestToFinSight(
    req,
    `/api/v1/comments/${encodeURIComponent(commentId)}`,
  )
}

