import LegalPageBody from "@/components/LegalPageBody"

export const LEGAL_POLICY_DOC_CLASS =
  "legal-policy-doc max-w-none overflow-x-auto [&_.board__inner]:text-gray-800 [&_table]:max-w-full [&_table]:w-full [&_table]:text-[14px] [&_table]:border-collapse"

export default function LegalHtmlDocument({ html }: { html: string }) {
  return (
    <div className="px-4 py-8 md:px-6 md:py-10">
      <LegalPageBody>
        <div
          className={LEGAL_POLICY_DOC_CLASS}
          dangerouslySetInnerHTML={{ __html: html }}
        />
      </LegalPageBody>
    </div>
  )
}
