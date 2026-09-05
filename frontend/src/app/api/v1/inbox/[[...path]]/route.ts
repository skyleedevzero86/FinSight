import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ path?: string[] }> }

function targetPath(path: string[] | undefined, search: string): string {
  const suffix = path?.length ? `/${path.join("/")}` : ""
  return `/api/v1/inbox${suffix}${search}`
}

async function mirror(req: Request, ctx: Ctx) {
  const { path } = await ctx.params
  const search = new URL(req.url).search
  return mirrorRequestToFinSight(req, targetPath(path, search))
}

export async function GET(req: Request, ctx: Ctx) {
  return mirror(req, ctx)
}

export async function POST(req: Request, ctx: Ctx) {
  return mirror(req, ctx)
}

export async function PUT(req: Request, ctx: Ctx) {
  return mirror(req, ctx)
}

export async function DELETE(req: Request, ctx: Ctx) {
  return mirror(req, ctx)
}

export async function PATCH(req: Request, ctx: Ctx) {
  return mirror(req, ctx)
}
