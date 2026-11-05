import { Link } from "react-router-dom";

interface UnavailablePageProps {
  area: string;
  reason: string;
}

export function UnavailablePage({ area, reason }: UnavailablePageProps) {
  return (
    <section className="page-shell unavailable">
      <span className="unavailable__icon" aria-hidden="true">i</span>
      <p className="eyebrow">Acesso futuro</p>
      <h1>{area} ainda não disponível</h1>
      <p>{reason}</p>
      <Link className="button button--primary" to="/">Voltar ao marketplace</Link>
    </section>
  );
}
