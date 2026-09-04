"use client"

type Action = { id: string; label: string; title: string }

type Props = {
  actions: Action[]
  onCommand: (command: string) => void
  disabled?: boolean
}

export default function CommunityMarkdownToolbar({
  actions,
  onCommand,
  disabled = false,
}: Props) {
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
            aria-label={action.title}
            disabled={disabled}
          >
            {action.label}
          </button>
        ))}
      </div>
    </div>
  )
}
