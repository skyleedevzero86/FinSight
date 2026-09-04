import { rmSync, existsSync } from "node:fs"
import { join } from "node:path"

const nextDir = join(process.cwd(), ".next")

if (!existsSync(nextDir)) {
  console.log(".next 캐시가 없습니다. 건너뜁니다.")
  process.exit(0)
}

try {
  rmSync(nextDir, { recursive: true, force: true })
  console.log(".next 캐시를 삭제했습니다.")
} catch (err) {
  console.error(".next 캐시 삭제에 실패했습니다.", err)
  process.exit(1)
}
