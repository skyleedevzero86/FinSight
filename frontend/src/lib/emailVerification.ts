type VerifyResponse = {
  ok: boolean
  message?: string
  verified?: boolean
}

function readMessage(data: unknown): string | undefined {
  if (data && typeof data === "object" && "message" in data) {
    const m = (data as { message: unknown }).message
    if (typeof m === "string") return m
  }
  return undefined
}

export async function requestEmailVerification(
  email: string,
): Promise<VerifyResponse> {
  const res = await fetch("/api/v1/auth/email/verify-request", {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify({ email: email.trim() }),
  })

  let data: Record<string, unknown> | null = null
  try {
    data = (await res.json()) as Record<string, unknown>
  } catch {
    data = null
  }

  if (!res.ok) {
    return {
      ok: false,
      message:
        readMessage(data) ??
        (res.status === 503
          ? "백엔드 연결(FINSIGHT_API_BASE_URL)을 확인해 주세요."
          : "이메일 인증 요청에 실패했습니다."),
    }
  }

  if (data && data.verified === false) {
    return {
      ok: false,
      message:
        readMessage(data) ?? "이메일 인증에 실패했습니다.",
      verified: false,
    }
  }

  return {
    ok: true,
    verified: true,
    message: readMessage(data),
  }
}
