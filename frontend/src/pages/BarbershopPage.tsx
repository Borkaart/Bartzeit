import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getBarbershop, listServices } from "../api/bartzeit";
import { getFriendlyErrorMessage } from "../api/errors";
import type { Barbershop, Service } from "../api/types";
import { Feedback, Loading } from "../components/Feedback";

export function BarbershopPage() {
  const { slug = "" } = useParams();
  const [shop, setShop] = useState<Barbershop | null>(null);
  const [services, setServices] = useState<Service[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    Promise.all([getBarbershop(slug), listServices(slug)])
      .then(([shopResult, serviceResult]) => {
        if (!active) return;
        setShop(shopResult);
        setServices(serviceResult);
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
  }, [slug, reloadKey]);

  function reload() {
    setLoading(true);
    setError(null);
    setReloadKey((key) => key + 1);
  }

  if (loading) return <div className="page-shell section"><Loading label="Carregando barbearia..." /></div>;
  if (error || !shop) {
    return (
      <section className="page-shell section">
        <Feedback action={reload} actionLabel="Tentar novamente" tone="error">
          {error ?? "Barbearia não encontrada."}
        </Feedback>
      </section>
    );
  }

  return (
    <>
      <section className="shop-hero">
        <div className="page-shell">
          <Link className="back-link" to="/">← Voltar às barbearias</Link>
          <p className="eyebrow">Barbearia</p>
          <h1>{shop.name}</h1>
          <div className="shop-meta">
            <span>{shop.city}</span>
            <span>{shop.address}</span>
            {shop.phone && <a href={`tel:${shop.phone}`}>{shop.phone}</a>}
          </div>
        </div>
      </section>

      <section className="page-shell section" aria-labelledby="services-heading">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Catálogo</p>
            <h2 id="services-heading">Serviços</h2>
          </div>
        </div>
        {services.length === 0 ? (
          <Feedback>Esta barbearia ainda não tem serviços ativos cadastrados.</Feedback>
        ) : (
          <ul className="service-grid">
            {services.map((service) => (
              <li className="service-card" key={service.id}>
                <div>
                  <h3>{service.name}</h3>
                  {service.description && <p>{service.description}</p>}
                  <span className="service-card__duration">{service.durationMinutes} minutos</span>
                </div>
                <div className="service-card__bottom">
                  <strong>{formatPrice(service.price)}</strong>
                  <Link className="button button--primary" to={`/barbearias/${encodeURIComponent(shop.slug)}/agendar?serviceId=${encodeURIComponent(service.id)}`}>
                    Consultar horários
                  </Link>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}

function formatPrice(price: number): string {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(price);
}
