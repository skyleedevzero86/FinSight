import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ boardId: string }> }

export async function POST(req: Request, ctx: Ctx) {
  const { boardId } = await ctx.params
  if (!/^\d+$/.test(boardId)) {
    return new Response(JSON.stringify({ message: "잘못된 게시글 번호입니다." }), {
      status: 400,
      headers: { "Content-Type": "application/json" },
    })
  }
  return mirrorRequestToFinSight(req, `/api/v1/boards/${boardId}/dislike`)
}
