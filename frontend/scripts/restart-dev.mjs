import { execSync, spawn } from "node:child_process"
import { existsSync, lstatSync, rmSync } from "node:fs"
import os from "node:os"
import { join } from "node:path"
import { fileURLToPath } from "node:url"

const root = process.cwd()
const ports = [3000, 3001]
const isWin = process.platform === "win32"
const localNext = join(root, ".next")
const legacyExternal = isWin
  ? join(process.env.LOCALAPPDATA || os.tmpdir(), "FinSight", "next-dev")
  : null

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

async function removeDir(dir, retries = 8) {
  if (!dir || !existsSync(dir)) return
  for (let i = 1; i <= retries; i++) {
    try {
      const st = lstatSync(dir)
      rmSync(dir, { recursive: true, force: true, maxRetries: 8, retryDelay: 250 })
      if (!existsSync(dir)) {
        console.log(`캐시 삭제: ${dir}${st.isSymbolicLink() ? " (정션/링크)" : ""}`)
        return
      }
    } catch (err) {
      if (i === retries) {
        console.error(`캐시 삭제 실패: ${dir}`, err)
        process.exit(1)
      }
    }
    console.warn(`캐시 삭제 재시도 ${i}/${retries}: ${dir}`)
    await sleep(500 * i)
  }
}

for (const port of ports) freePort(port)
await sleep(1200)
await removeDir(localNext)
await removeDir(legacyExternal)

const devScript = fileURLToPath(new URL("./dev.mjs", import.meta.url))
console.log("Next.js 개발 서버를 포트 3000에서 시작합니다…")
console.log("주의: Next는 한 개만 실행하세요. 동시에 두 개 띄우면 캐시가 깨집니다.")
const child = spawn(process.execPath, [devScript], {
  stdio: "inherit",
  env: process.env,
  cwd: root,
})

child.on("exit", (code, signal) => {
  if (signal) process.kill(process.pid, signal)
  process.exit(code ?? 0)
})
