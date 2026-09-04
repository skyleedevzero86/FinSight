import { mirrorBinaryRequestToFinSight } from "@/lib/finsightApiProxy"

export async function POST(req: Request) {
  return mirrorBinaryRequestToFinSight(req, "/api/v1/admin/sms/upload-image")
}
