import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { getAvailability, getBarbershop, listServices } from "../api/bartzeit";
import { getFriendlyErrorMessage } from "../api/errors";
import type { Availability, Barbershop, Service } from "../api/types";
import { Feedback, Loading } from "../components/Feedback";

export function AvailabilityPage() {
  const { slug = "" } = useParams();
  const [searchParams] = useSearchParams();
  const [shop, setShop] = useState<Barbershop | null>(null);
  const [services, setServices] = useState<Service[]>([]);
  const [serviceId, setServiceId] = useState(searchParams.get("serviceId") ?? "");
  const [date, setDate] = useState(todayLocal());
  const [availability, setAvailability] = useState<Availability[] | null>(null);
  const [loadingPage, setLoadingPage] = useState(true);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [slotsError, setSlotsError] = useState<string | null>(null);
  const [selectedSlot, setSelectedSlot] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    Promise.all([getBarbershop(slug), listServices(slug)])
      .then(([shopResult, servicesResult]) => {
        if (!active) return;
        setShop(shopResult);
        setServices(servicesResult);
        const preferredServiceId = searchParams.get("serviceId");
        setServiceId(
          servicesResult.some((service) => service.id === preferredServiceId)
            ? preferredServiceId ?? ""
            : servicesResult[0]?.id ?? "",
        );
      })
      .catch((requestError: unknown) => {
        if (active) setError(getFriendlyErrorMessage(requestError));
      })
      .finally(() => {
        if (active) setLoadingPage(false);
      });
    return () => {
      active = false;
    };
  }, [slug, searchParams, reloadKey]);

  function reloadCatalog() {
    setLoadingPage(true);
    setError(null);
    setReloadKey((key) => key + 1);
  }

  function changeService(service: string) {
    setServiceId(service);
    setAvailability(null);
    setSelectedSlot(null);
    setSlotsError(null);
  }

  function changeDate(nextDate: string) {
    setDate(nextDate);
    setAvailability(null);
    setSelectedSlot(null);
    setSlotsError(null);
  }

  const checkAvailability = useCallback(async (event?: FormEvent<HTMLFormElement>) => {
    event?.preventDefault();
    if (!serviceId || !date) return;
    setLoadingSlots(true);
    setSlotsError(null);
    setAvailability(null);
    setSelectedSlot(null);
    try {
      setAvailability(await getAvailability(slug, date, serviceId));
    } catch (requestError) {
      setSlotsError(getFriendlyErrorMessage(requestError));
    } finally {
      setLoadingSlots(false);
    }
  }, [date, serviceId, slug]);

  if (loadingPage) return <div className="page-shell section"><Loading label="Carregando catálogo..." /></div>;
  if (error || !shop) {
    return (
      <section className="page-shell section">
        <Feedback action={reloadCatalog} actionLabel="Tentar novamente" tone="error">
          {error ?? "Barbearia não encontrada."}
        </Feedback>
      </section>
    );
  }

  return (
    <section className="page-shell section booking-page">
      <Link className="back-link" to={`/barbearias/${encodeURIComponent(slug)}`}>← Voltar para {shop.name}</Link>
      <p className="eyebrow">Consulta de horários</p>
      <h1>Escolha um serviço e uma data</h1>
      <p className="page-intro">Os horários abaixo são consultados diretamente na API. Consultar não reserva nem confirma um agendamento.</p>

      {services.length === 0 ? (
        <Feedback>Esta barbearia ainda não tem serviços ativos disponíveis.</Feedback>
      ) : (
        <form className="availability-form" onSubmit={(event) => void checkAvailability(event)}>
          <div className="field">
            <label htmlFor="service">Serviço</label>
            <select id="service" onChange={(event) => changeService(event.target.value)} required value={serviceId}>
              {services.map((service) => (
                <option key={service.id} value={service.id}>{service.name} · {service.durationMinutes} min</option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="date">Data</label>
            <input id="date" min={todayLocal()} onChange={(event) => changeDate(event.target.value)} required type="date" value={date} />
          </div>
          <button className="button button--primary" disabled={!serviceId || !date || loadingSlots} type="submit">
            {loadingSlots ? "Consultando..." : "Consultar disponibilidade"}
          </button>
        </form>
      )}

      {loadingSlots && <Loading label="Consultando horários na barbearia..." />}
      {slotsError && <Feedback action={() => void checkAvailability()} actionLabel="Tentar novamente" tone="error">{slotsError}</Feedback>}
      {!loadingSlots && availability && availability.length === 0 && (
        <Feedback>A API não retornou barbeiros ativos para esta consulta.</Feedback>
      )}
      {!loadingSlots && availability && availability.length > 0 && (
        <div className="availability-results" aria-live="polite">
          {availability.map((barber) => (
            <section className="barber-slots" key={barber.barberId}>
              <div className="barber-slots__heading">
                <h2>{barber.barberName}</h2>
                <span>{barber.available ? "Tem horários disponíveis" : "Sem horários nesta data"}</span>
              </div>
              {barber.slots.length > 0 ? (
                <ul className="slot-list" aria-label={`Horários de ${barber.barberName}`}>
                  {barber.slots.map((slot) => (
                    <li key={slot}>
                      <button
                        aria-pressed={selectedSlot === slot}
                        className={`slot-button${selectedSlot === slot ? " slot-button--selected" : ""}`}
                        onClick={() => setSelectedSlot(slot)}
                        type="button"
                      >
                        {formatTime(slot)}
                      </button>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="muted-copy">Nenhum horário retornado.</p>
              )}
            </section>
          ))}
          {selectedSlot && (
            <Feedback tone="success">
              A API listou {formatDateTime(selectedSlot)}. Nenhum horário foi reservado: a criação de agendamentos exige autenticação que ainda não está configurada.
            </Feedback>
          )}
        </div>
      )}

      <aside className="notice" role="note">
        <strong>Consulta sem reserva</strong>
        <p>Esta versão não confirma agendamentos. Para evitar uma falsa confirmação, o envio está indisponível até existir uma autenticação real no backend.</p>
      </aside>
    </section>
  );
}

function todayLocal(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function formatTime(dateTime: string): string {
  return new Intl.DateTimeFormat("pt-BR", { hour: "2-digit", minute: "2-digit" }).format(new Date(dateTime));
}

function formatDateTime(dateTime: string): string {
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "long",
    timeStyle: "short",
  }).format(new Date(dateTime));
}
