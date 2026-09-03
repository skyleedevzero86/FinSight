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

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function toKoreanMessage(value: unknown, fallback: string): string {
  if (typeof value !== "string" || !value.trim()) return fallback
  const text = value.trim()
  if (/[가-힣]/.test(text)) return text
  if (/unauthorized|forbidden|login|auth/i.test(text)) return "로그인이 필요합니다."
  if (/not found/i.test(text)) return "요청한 파일을 찾을 수 없습니다."
  if (/timeout|abort/i.test(text)) return "요청 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요."
  if (/network|fetch|econn|refused|reset/i.test(text)) {
    return "서버 연결이 끊겼습니다. 잠시 후 다시 시도해 주세요."
  }
  if (/too large|size|payload/i.test(text)) return "파일 크기가 너무 큽니다."
  if (/content.?type|unsupported|invalid/i.test(text)) return "지원하지 않는 파일 형식입니다."
  return fallback
}

async function postOnce(
  file: File,
  qs: string,
): Promise<{ res: Response; payload: unknown }> {
  const form = new FormData()
  form.append("file", file, file.name || "붙여넣은-이미지.png")
  const res = await fetch(`/api/editor/images${qs}`, {
    method: "POST",
    headers: { ...authHeaders() },
    body: form,
    cache: "no-store",
  })
  const payload: unknown = await res.json().catch(() => null)
  return { res, payload }
}

export async function uploadEditorAsset(
  file: File,
  options?: { allowFile?: boolean },
): Promise<EditorUploadResult> {
  if (!readUsableAccessToken()) {
    throw new Error("로그인이 필요합니다.")
  }

  const qs = options?.allowFile ? "?allowFile=true" : ""
  let lastError = "파일 업로드에 실패했습니다."

  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      const { res, payload } = await postOnce(file, qs)
      if (res.ok) {
        const root = asRecord(payload)
        const data = asRecord(root?.data) ?? root
        if (!data) throw new Error("업로드 응답을 해석하지 못했습니다.")
        const url =
          (typeof data.imageUrl === "string" && data.imageUrl) ||
          (typeof data.url === "string" && data.url) ||
          ""
        if (!url) throw new Error("업로드 주소가 없습니다.")
        return {
          url,
          originalFileName:
            (typeof data.originalFileName === "string" && data.originalFileName) ||
            file.name,
          size: typeof data.size === "number" ? data.size : file.size,
        }
      }

      const root = asRecord(payload)
      lastError = toKoreanMessage(root?.message, "파일 업로드에 실패했습니다.")
      if (res.status === 401 || res.status === 403) {
        throw new Error("로그인이 필요합니다.")
      }
      if (res.status < 500 && res.status !== 429) {
        throw new Error(lastError)
      }
    } catch (err) {
      if (err instanceof Error) {
        const message = toKoreanMessage(err.message, lastError)
        if (/로그인|형식|용량|선택|비어/.test(message)) {
          throw new Error(message)
        }
        lastError = message
      } else {
        lastError = "업로드에 실패했습니다."
      }
    }
    await sleep(400 * (attempt + 1))
  }

  throw new Error(lastError)
}
