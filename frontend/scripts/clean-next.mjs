import { existsSync, lstatSync, rmSync } from "node:fs"
import { execSync } from "node:child_process"
import os from "node:os"
import path from "node:path"
import { fileURLToPath } from "node:url"

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const root = path.resolve(__dirname, "..")
const isWin = process.platform === "win32"
const localNext = path.join(root, ".next")
const legacyExternal = isWin
  ? path.join(process.env.LOCALAPPDATA || os.tmpdir(), "FinSight", "next-dev")
  : null

function removePath(dir, label) {
  if (!dir || !existsSync(dir)) {
    console.log(`없음: ${label}`)
    return true
  }
  try {
    const st = lstatSync(dir)
    if (isWin && st.isSymbolicLink()) {
      try {
        execSync(`cmd /c rmdir "${dir}"`, { stdio: "ignore", windowsHide: true })
        console.log(`정션 해제: ${label}`)
        return true
      } catch {
        void 0
      }
    }
    rmSync(dir, { recursive: true, force: true, maxRetries: 10, retryDelay: 300 })
    console.log(`삭제: ${label}${st.isSymbolicLink() ? " (정션/링크)" : ""}`)
    return true
  } catch (err) {
    console.error(`삭제 실패: ${label}`, err && err.message ? err.message : err)
    return false
  }
}

let ok = true
ok = removePath(localNext, localNext) && ok
if (legacyExternal) ok = removePath(legacyExternal, legacyExternal) && ok

if (!ok) {
  console.error("Next 프로세스를 종료한 뒤 다시 pnpm run clean 하세요.")
  process.exit(1)
}

console.log("Next 캐시 정리를 마쳤습니다. 프로젝트 안 .next만 새로 만듭니다.")
