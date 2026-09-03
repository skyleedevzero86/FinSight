import { authHeaders, readUsableAccessToken } from "@/lib/finsightToken"

export type EditorUploadResult = {
  url: string
  originalFileName: string
  size: number
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object") return null
  return value as Record<string, unknown>
}

export async function uploadEditorAsset(
  file: File,
  options?: { allowFile?: boolean },
): Promise<EditorUploadResult> {
  if (!readUsableAccessToken()) {
    throw new Error("로그인이 필요합니다.")
  }
  const form = new FormData()
  form.append("file", file)
  const qs = options?.allowFile ? "?allowFile=true" : ""
  const res = await fetch(`/api/editor/images${qs}`, {
    method: "POST",
    headers: { ...authHeaders() },
    body: form,
    cache: "no-store",
  })
  const payload: unknown = await res.json().catch(() => null)
  if (!res.ok) {
    const root = asRecord(payload)
    const msg =
      (root && typeof root.message === "string" && root.message) ||
      "파일 업로드에 실패했습니다."
    throw new Error(msg)
  }
  const root = asRecord(payload)
  const data = asRecord(root?.data) ?? root
  if (!data) throw new Error("업로드 응답을 해석하지 못했습니다.")
  const url =
    (typeof data.imageUrl === "string" && data.imageUrl) ||
    (typeof data.url === "string" && data.url) ||
    ""
  if (!url) throw new Error("업로드 URL이 없습니다.")
  return {
    url,
    originalFileName:
      (typeof data.originalFileName === "string" && data.originalFileName) || file.name,
    size: typeof data.size === "number" ? data.size : file.size,
  }
}
