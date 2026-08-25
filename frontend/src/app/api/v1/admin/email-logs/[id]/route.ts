import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

export async function GET(
  req: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  const { id } = await params
  return mirrorRequestToFinSight(req, `/api/v1/admin/email-logs/${id}`)
}
