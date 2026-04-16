import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

function joinPath(path: string[] | undefined): string {
  if (!path || path.length === 0) return "/"
  return `/${path.map(encodeURIComponent).join("/")}`
}

function buildBackendPath(req: Request, path: string[] | undefined): string {
  const url = new URL(req.url)
  const pathname = joinPath(path)
  const query = url.search ? url.search : ""
  return `${pathname}${query}`
}

type Context = { params: Promise<{ path?: string[] }> }

export async function GET(req: Request, ctx: Context) {
  const { path } = await ctx.params
  return mirrorRequestToFinSight(req, buildBackendPath(req, path))
}

export async function POST(req: Request, ctx: Context) {
  const { path } = await ctx.params
  return mirrorRequestToFinSight(req, buildBackendPath(req, path))
}

export async function PUT(req: Request, ctx: Context) {
  const { path } = await ctx.params
  return mirrorRequestToFinSight(req, buildBackendPath(req, path))
}

export async function PATCH(req: Request, ctx: Context) {
  const { path } = await ctx.params
  return mirrorRequestToFinSight(req, buildBackendPath(req, path))
}

export async function DELETE(req: Request, ctx: Context) {
  const { path } = await ctx.params
  return mirrorRequestToFinSight(req, buildBackendPath(req, path))
}

