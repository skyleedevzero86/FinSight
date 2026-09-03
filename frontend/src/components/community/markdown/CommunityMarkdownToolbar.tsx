"use client"

import type { MarkdownCommandId, ToolbarAction } from "@/lib/communityMarkdown"

type Props = {
  actions: ToolbarAction[]
  onCommand: (command: MarkdownCommandId) => void
}

export default function CommunityMarkdownToolbar({ actions, onCommand }: Props) {
  return (
    <div className="fcb-md-toolbar">
      <div className="fcb-md-toolbar__group">
        {actions.map((action) => (
          <button
            key={action.id}
            type="button"
            className="fcb-md-toolbar__button"
            onClick={() => onCommand(action.id)}
            title={action.title}
          >
            {action.label}
          </button>
        ))}
      </div>
    </div>
  )
}
