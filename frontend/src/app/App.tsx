import { Navigate, Route, Routes } from "react-router-dom";
import { Layout } from "../components/Layout";
import { AvailabilityPage } from "../pages/AvailabilityPage";
import { BarbershopPage } from "../pages/BarbershopPage";
import { MarketplacePage } from "../pages/MarketplacePage";
import { UnavailablePage } from "../pages/UnavailablePage";

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<MarketplacePage />} />
        <Route path="barbearias/:slug" element={<BarbershopPage />} />
        <Route path="barbearias/:slug/agendar" element={<AvailabilityPage />} />
        <Route
          path="cliente"
          element={<UnavailablePage area="Área do cliente" reason="Perfil e histórico dependem de identidade real, endpoints próprios de consulta e autorização vinculada ao cliente." />}
        />
        <Route
          path="barbeiro"
          element={<UnavailablePage area="Área do barbeiro" reason="Agenda e alteração de status dependem de autenticação e autorização de barbeiro por tenant." />}
        />
        <Route
          path="administracao"
          element={<UnavailablePage area="Painel administrativo" reason="O painel requer autenticação administrativa real. Nenhum acesso ou métrica demonstrativa é exibido." />}
        />
        <Route path="*" element={<Navigate replace to="/" />} />
      </Route>
    </Routes>
  );
}
