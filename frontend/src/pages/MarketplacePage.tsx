import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { listBarbershops } from "../api/bartzeit";
import { getFriendlyErrorMessage } from "../api/errors";
import type { Barbershop } from "../api/types";
import { Feedback, Loading } from "../components/Feedback";

const PAGE_SIZE = 20;

export function MarketplacePage() {
  const [shops, setShops] = useState<Barbershop[]>([]);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [slug, setSlug] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const navigate = useNavigate();

  useEffect(() => {
    let active = true;
    listBarbershops(page, PAGE_SIZE)
      .then((result) => {
        if (active) setShops(result);
      })
      .catch((requestError: unknown) => {
        if (active) setError(getFriendlyErrorMessage(requestError));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [page, reloadKey]);

  function openSlug(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalized = slug.trim().toLowerCase();
    if (normalized) navigate(`/barbearias/${encodeURIComponent(normalized)}`);
  }

  function reload() {
    setError(null);
    setLoading(true);
    setReloadKey((key) => key + 1);
  }

  function changePage(nextPage: number) {
    setError(null);
    setLoading(true);
    setPage(nextPage);
  }

  return (
    <>
      <section className="hero">
        <div className="page-shell hero__content">
          <p className="eyebrow">Seu próximo corte começa aqui</p>
          <h1>Encontre sua<br /><span>barbearia.</span></h1>
          <p className="hero__copy">Explore os estabelecimentos cadastrados e consulte serviços e horários direto na plataforma.</p>
          <form className="slug-search" onSubmit={openSlug}>
            <label htmlFor="shop-slug">Já sabe o nome de usuário da barbearia?</label>
            <div className="slug-search__row">
              <input
                autoComplete="off"
                id="shop-slug"
                onChange={(event) => setSlug(event.target.value)}
                placeholder="Ex.: barbearia-central"
                value={slug}
              />
              <button className="button button--primary" disabled={!slug.trim()} type="submit">Buscar por slug</button>
            </div>
          </form>
          <p className="hero__note">A busca por localização não está disponível na API atual.</p>
        </div>
      </section>

      <section className="page-shell section" aria-labelledby="shops-heading">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Marketplace</p>
            <h2 id="shops-heading">Barbearias cadastradas</h2>
          </div>
          {!loading && !error && shops.length > 0 && (
            <span className="count-label">Página {page + 1} · até {PAGE_SIZE} por página</span>
          )}
        </div>

        {loading && <Loading label="Buscando barbearias..." />}
        {error && <Feedback action={reload} actionLabel="Tentar novamente" tone="error">{error}</Feedback>}
        {!loading && !error && shops.length === 0 && (
          <Feedback>Não há barbearias cadastradas nesta página. Os dados vêm do backend e podem ser apagados quando ele reiniciar.</Feedback>
        )}

        {!loading && !error && shops.length > 0 && (
          <>
            <ul className="shop-grid">
              {shops.map((shop) => (
                <li className="shop-card" key={shop.id}>
                  <div className="shop-card__topline">
                    <span className="shop-card__icon" aria-hidden="true">B</span>
                    <span className="status-pill"><span />{shop.active ? "Ativa" : "Inativa"}</span>
                  </div>
                  <h3>{shop.name}</h3>
                  <p className="shop-card__location">{shop.city}</p>
                  <p className="shop-card__address">{shop.address}</p>
                  <Link className="text-link" to={`/barbearias/${encodeURIComponent(shop.slug)}`}>
                    Ver barbearia <span aria-hidden="true">→</span>
                  </Link>
                </li>
              ))}
            </ul>
            <div className="pagination" aria-label="Paginação das barbearias">
              <button className="button button--secondary" disabled={page === 0} onClick={() => changePage(Math.max(0, page - 1))} type="button">
                Anterior
              </button>
              <span>Página {page + 1}</span>
              <button className="button button--secondary" disabled={shops.length < PAGE_SIZE} onClick={() => changePage(page + 1)} type="button">
                Próxima
              </button>
            </div>
          </>
        )}
      </section>
    </>
  );
}
