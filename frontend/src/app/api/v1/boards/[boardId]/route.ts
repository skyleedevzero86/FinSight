import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ boardId: string }> }

function invalidId() {
  return new Response(JSON.stringify({ message: "잘못된 게시글 번호입니다." }), {
    status: 400,
    headers: { "Content-Type": "application/json" },
  })
}

export async function GET(req: Request, ctx: Ctx) {
  const { boardId } = await ctx.params
  if (!/^\d+$/.test(boardId)) return invalidId()
  return mirrorRequestToFinSight(req, `/api/v1/boards/${boardId}`)
}

export async function PUT(req: Request, ctx: Ctx) {
  const { boardId } = await ctx.params
  if (!/^\d+$/.test(boardId)) return invalidId()
  return mirrorRequestToFinSight(req, `/api/v1/boards/${boardId}`)
}

export async function DELETE(req: Request, ctx: Ctx) {
  const { boardId } = await ctx.params
  if (!/^\d+$/.test(boardId)) return invalidId()
  return mirrorRequestToFinSight(req, `/api/v1/boards/${boardId}`)
}
