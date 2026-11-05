import type { ReactNode } from "react";

interface FeedbackProps {
  children: ReactNode;
  action?: () => void;
  actionLabel?: string;
  tone?: "error" | "neutral" | "success";
}

export function Feedback({ children, action, actionLabel, tone = "neutral" }: FeedbackProps) {
  return (
    <div className={`feedback feedback--${tone}`} role={tone === "error" ? "alert" : "status"}>
      <p>{children}</p>
      {action && actionLabel && (
        <button className="button button--small" type="button" onClick={action}>
          {actionLabel}
        </button>
      )}
    </div>
  );
}

export function Loading({ label = "Carregando..." }: { label?: string }) {
  return (
    <div className="loading" role="status" aria-live="polite">
      <span className="loading__spinner" aria-hidden="true" />
      {label}
    </div>
  );
}
