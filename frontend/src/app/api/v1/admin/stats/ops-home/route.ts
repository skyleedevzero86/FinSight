import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object") return null
  return value as Record<string, unknown>
}

async function readUpstream(req: Request, path: string): Promise<{ status: number; body: unknown }> {
  const upstream = await mirrorRequestToFinSight(req, path)
  try {
    return { status: upstream.status, body: await upstream.json() }
  } catch {
    return { status: upstream.status, body: null }
  }
}

function payloadData(body: unknown): Record<string, unknown> | null {
  const root = asRecord(body)
  return asRecord(root?.data) ?? root
}

async function legacyOpsHome(req: Request): Promise<Response> {
  const [overview, signups, logins, content] = await Promise.all([
    readUpstream(req, "/api/v1/admin/stats/overview"),
    readUpstream(req, "/api/v1/admin/stats/charts/signups?days=7"),
    readUpstream(req, "/api/v1/admin/stats/charts/logins?days=7"),
    readUpstream(req, "/api/v1/admin/stats/charts/content?days=7"),
  ])
  if (overview.status === 401 || overview.status === 403) {
    return Response.json(overview.body ?? { success: false, message: "관리자 권한이 필요합니다.", statusCode: overview.status }, {
      status: overview.status,
    })
  }
  const overviewData = payloadData(overview.body)
  if (overview.status !== 200 || !overviewData) {
    return Response.json(
      { success: false, message: "운영 현황을 불러오지 못했습니다.", statusCode: overview.status || 503 },
      { status: overview.status || 503 },
    )
  }
  return Response.json({
    success: true,
    message: "운영 현황을 조회했습니다",
    statusCode: 200,
    data: {
      ...overviewData,
      signups: payloadData(signups.body),
      logins: payloadData(logins.body),
      content: payloadData(content.body),
    },
  })
}

export async function GET(req: Request) {
  const primary = await mirrorRequestToFinSight(req, "/api/v1/admin/stats/ops-home")
  if (primary.status !== 404) return primary
  return legacyOpsHome(req)
}
