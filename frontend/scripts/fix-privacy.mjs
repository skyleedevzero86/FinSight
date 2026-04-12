import fs from "node:fs"
import path from "node:path"
import { fileURLToPath } from "node:url"

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const p = path.join(__dirname, "..", "public", "privacy-policy.html")
let s = fs.readFileSync(p, "utf8")

s = s.replace('<p class="fr-element fr-view">', '<div class="fr-element fr-view">')
s = s.replace(
  '<p align="center"><span style="font-size: 14px;"><strong>제이티비씨</strong><strong>(</strong><strong>주</strong><strong>)&nbsp;</strong><strong>개인정보처리방침</strong></span></p><p align="center"><span style="font-size: 14px;">&nbsp;</span></p>',
  '<p style="text-align: center;"><strong><u>finsight 주식회사 개인정보처리방침</u></strong></p><p><br></p><p><br></p><p><br></p>'
)
s = s.replace(/제이티비씨\(주\)/g, "finsight 주식회사")
s = s.replace(/제이티비씨㈜/g, "finsight 주식회사")
s = s.replace(/제이티비씨/g, "finsight")
s = s.replace(/JTBC_privacy@jtbc\.co\.kr/g, "privacy@finsight.kr")
s = s.replace(/JTBC 회원/g, "finsight 회원")
s = s.replace(/JTBC 제보/g, "finsight 제보")
s = s.replace(/\bJTBC\b/g, "finsight")

if (!s.endsWith("</div></div></div>")) {
  s = s.replace(/<\/span><\/p><\/p><\/div><\/div>\s*$/, "</span></p></div></div></div>")
}

fs.writeFileSync(p, s)
console.log("privacy-policy.html updated, tail:", s.slice(-90))
