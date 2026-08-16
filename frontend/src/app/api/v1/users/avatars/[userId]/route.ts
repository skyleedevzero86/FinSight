import { mirrorBinaryRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ userId: string }> }

export async function GET(req: Request, ctx: Ctx) {
  const { userId } = await ctx.params
  if (!/^\d+$/.test(userId)) {
    return new Response(JSON.stringify({ message: "잘못된 사용자입니다." }), {
      status: 400,
      headers: { "Content-Type": "application/json" },
    })
  }
  return mirrorBinaryRequestToFinSight(req, `/api/v1/users/avatars/${userId}`)
}
