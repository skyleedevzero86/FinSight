import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

export async function GET(
  req: Request,
  context: { params: Promise<{ chartKey: string }> },
) {
  const { chartKey } = await context.params
  const q = new URL(req.url).searchParams.toString()
  return mirrorRequestToFinSight(
    req,
    `/api/v1/admin/stats/charts/${encodeURIComponent(chartKey)}${q ? `?${q}` : ""}`,
  )
}
