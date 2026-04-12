export default function LegalPageBody({
  children,
  className = "",
}: {
  children: React.ReactNode
  className?: string
}) {
  return (
    <div
      className={[
        "mx-auto max-w-4xl bg-white px-4 py-10 text-gray-900 shadow-sm md:px-10 md:py-14",
        "border border-gray-100/90",
        "[&_.fr-element]:text-[15px] [&_.fr-element]:leading-[1.8] [&_.fr-element]:tracking-[-0.01em]",
        "[&_.fr-element_p]:mb-0 [&_.fr-element_p+p]:mt-3",
        "[&_.fr-element_strong]:text-gray-900",
        "[&_a]:text-finsight-primary [&_a]:underline",
        className,
      ]
        .filter(Boolean)
        .join(" ")}
    >
      {children}
    </div>
  )
}
