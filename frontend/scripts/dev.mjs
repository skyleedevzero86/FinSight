import { spawn } from "node:child_process"
import { createRequire } from "node:module"
import path from "node:path"
import { fileURLToPath } from "node:url"

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const root = path.resolve(__dirname, "..")
const require = createRequire(import.meta.url)
const nextBin = require.resolve("next/dist/bin/next")
const isWin = process.platform === "win32"

const useWebpack = process.env.NEXT_DEV_WEBPACK === "1"
const useTurbo =
  !useWebpack && (isWin || process.env.NEXT_DEV_TURBO === "1")

if (isWin) {
  console.log(
    useTurbo
      ? "Windows: Turbopack + 프로젝트 안 .next 로 파일 잠금(-4094)을 완화합니다."
      : "Windows: Webpack 폴링 + 프로젝트 안 .next 로 파일 잠금(-4094)을 완화합니다.",
  )
  console.log("깨지면 Ctrl+C 후 pnpm run clean && pnpm run dev (또는 pnpm run dev:clean)")
  console.log("Webpack이 필요하면 NEXT_DEV_WEBPACK=1 pnpm run dev")
}

const env = {
  ...process.env,
  WATCHPACK_POLLING:
    process.env.WATCHPACK_POLLING || (isWin && useWebpack ? "1" : process.env.WATCHPACK_POLLING),
  CHOKIDAR_USEPOLLING:
    process.env.CHOKIDAR_USEPOLLING || (isWin && useWebpack ? "1" : process.env.CHOKIDAR_USEPOLLING),
}
delete env.NEXT_DIST_DIR

const args = [nextBin, "dev", "-p", process.env.PORT || "3000"]
if (useWebpack) {
  args.push("--webpack")
} else if (useTurbo) {
  args.push("--turbopack")
}

const child = spawn(process.execPath, args, {
  cwd: root,
  env,
  stdio: "inherit",
})

child.on("exit", (code, signal) => {
  if (signal) process.kill(process.pid, signal)
  process.exit(code ?? 0)
})
