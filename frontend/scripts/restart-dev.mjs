import { execSync, spawn } from "node:child_process"
import { existsSync, rmSync } from "node:fs"
import { join } from "node:path"
import { createRequire } from "node:module"

const ports = [3000, 3001]
const nextDir = join(process.cwd(), ".next")

function freePort(port) {
  if (process.platform === "win32") {
    try {
      const out = execSync(`netstat -ano`, { encoding: "utf8" })
      const pids = new Set()
      for (const line of out.split(/\r?\n/)) {
        if (!line.includes(`:${port}`) || !line.includes("LISTENING")) continue
        const parts = line.trim().split(/\s+/)
        const pid = parts[parts.length - 1]
        if (pid && /^\d+$/.test(pid) && pid !== "0") pids.add(pid)
      }
      for (const pid of pids) {
        try {
          execSync(`taskkill /PID ${pid} /F`, { stdio: "ignore" })
          console.log(`포트 ${port} 프로세스(PID ${pid})를 종료했습니다.`)
        } catch {
          console.warn(`포트 ${port} 프로세스(PID ${pid}) 종료에 실패했습니다.`)
        }
      }
    } catch {
      void 0
    }
    return
  }

  try {
    const out = execSync(`lsof -tiTCP:${port} -sTCP:LISTEN`, {
      encoding: "utf8",
      stdio: ["ignore", "pipe", "ignore"],
    })
    for (const pid of out.split(/\n/).map((s) => s.trim()).filter(Boolean)) {
      try {
        execSync(`kill -9 ${pid}`, { stdio: "ignore" })
        console.log(`포트 ${port} 프로세스(PID ${pid})를 종료했습니다.`)
      } catch {
        console.warn(`포트 ${port} 프로세스(PID ${pid}) 종료에 실패했습니다.`)
      }
    }
  } catch {
    void 0
  }
}

async function sleep(ms) {
  await new Promise((r) => setTimeout(r, ms))
}

async function cleanNext(retries = 8) {
  if (!existsSync(nextDir)) {
    console.log(".next 캐시가 없습니다.")
    return
  }
  for (let i = 1; i <= retries; i++) {
    try {
      rmSync(nextDir, { recursive: true, force: true })
      if (!existsSync(nextDir)) {
        console.log(".next 캐시를 삭제했습니다.")
        return
      }
    } catch (err) {
      if (i === retries) {
        console.error(
          ".next 캐시 삭제에 실패했습니다. 다른 Next/node 프로세스를 모두 종료한 뒤 다시 시도하세요.",
          err,
        )
        process.exit(1)
      }
    }
    console.warn(`.next 삭제 재시도 ${i}/${retries}… (파일 잠금 해제 대기)`)
    await sleep(500 * i)
  }
  console.error(".next 폴더가 남아 있습니다. 수동으로 삭제한 뒤 pnpm run dev 하세요.")
  process.exit(1)
}

for (const port of ports) freePort(port)
await sleep(1200)
await cleanNext()

const require = createRequire(import.meta.url)
const nextBin = require.resolve("next/dist/bin/next")
console.log("Next.js 개발 서버를 포트 3000에서 시작합니다…")
console.log("주의: Next는 한 개만 실행하세요. 3000/3001이 동시에 뜨면 .next가 깨집니다.")
const child = spawn(process.execPath, [nextBin, "dev", "-p", "3000"], {
  stdio: "inherit",
  env: process.env,
  cwd: process.cwd(),
})

child.on("exit", (code, signal) => {
  if (signal) process.kill(process.pid, signal)
  process.exit(code ?? 0)
})
