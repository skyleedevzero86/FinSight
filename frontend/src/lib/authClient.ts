function readApiErrorMessage(payload: unknown, fallback: string): string {
  if (payload && typeof payload === "object") {
    const o = payload as Record<string, unknown>
    if (typeof o.message === "string" && o.message) return o.message
    if (Array.isArray(o.messages) && o.messages[0])
      return String(o.messages[0])
  }
  return fallback
}

export async function postLogin(body: {
  email: string
  password: string
}): Promise<
  | { ok: true; data: unknown }
  | { ok: false; status: number; message: string }
> {
  const res = await fetch("/api/v1/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify(body),
  })
  let data: unknown
  try {
    data = await res.json()
  } catch {
    data = null
  }
  if (!res.ok) {
    const fallback =
      res.status >= 500
        ? "로그인 서버에 일시적인 문제가 있습니다. 잠시 후 다시 시도해 주세요."
        : "로그인에 실패했습니다. 이메일(또는 아이디)과 비밀번호를 확인해 주세요."
    return {
      ok: false,
      status: res.status,
      message: readApiErrorMessage(data, fallback),
    }
  }
  return { ok: true, data }
}

export async function postFindEmail(body: {
  name: string
  phone: string
}): Promise<
  | { ok: true; data: unknown }
  | { ok: false; status: number; message: string }
> {
  const res = await fetch("/api/v1/auth/find-email", {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify(body),
  })
  let data: unknown
  try {
    data = await res.json()
  } catch {
    data = null
  }
  if (!res.ok) {
    return {
      ok: false,
      status: res.status,
      message: readApiErrorMessage(
        data,
        "이메일을 찾을 수 없습니다. 입력 정보를 확인해 주세요.",
      ),
    }
  }
  return { ok: true, data }
}

export async function postForgotPassword(body: { email: string }): Promise<
  | { ok: true; data: unknown }
  | { ok: false; status: number; message: string }
> {
  const res = await fetch("/api/v1/auth/forgot-password", {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify(body),
  })
  let data: unknown
  try {
    data = await res.json()
  } catch {
    data = null
  }
  if (!res.ok) {
    return {
      ok: false,
      status: res.status,
      message: readApiErrorMessage(
        data,
        "요청을 처리하지 못했습니다. 이메일을 확인해 주세요.",
      ),
    }
  }
  return { ok: true, data }
}
