import { existsSync, rmSync } from "node:fs"
import { join } from "node:path"

const root = join(process.cwd())
const targets = [
  "core/out",
  "core/bin",
  "web/out",
  "web/bin",
  "batch/out",
  "batch/bin",
]

let failed = false
for (const rel of targets) {
  const full = join(root, rel)
  if (!existsSync(full)) {
    console.log(`없음: ${rel}`)
    continue
  }
  try {
    rmSync(full, { recursive: true, force: true, maxRetries: 5, retryDelay: 200 })
    console.log(`삭제: ${rel}`)
  } catch (err) {
    failed = true
    console.error(`삭제 실패: ${rel}`, err && err.message ? err.message : err)
  }
}

if (failed) {
  console.error("일부 산출물이 잠겨 있습니다. IntelliJ 실행을 중지한 뒤 다시 실행하세요.")
  process.exit(1)
}

console.log("IntelliJ/Gradle 중복 산출물 정리를 마쳤습니다. IDE에서 Rebuild 후 다시 실행하세요.")
