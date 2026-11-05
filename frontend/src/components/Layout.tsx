import { Link, NavLink, Outlet } from "react-router-dom";

export function Layout() {
  return (
    <>
      <header className="site-header">
        <div className="site-header__inner">
          <Link className="brand" to="/" aria-label="Bartzeit, início">
            <span className="brand__mark" aria-hidden="true">B</span>
            <span>Bartzeit</span>
          </Link>
          <nav aria-label="Navegação principal" className="main-nav">
            <NavLink to="/" end>Explorar</NavLink>
            <NavLink to="/cliente">Cliente</NavLink>
            <NavLink to="/barbeiro">Barbeiro</NavLink>
            <NavLink to="/administracao">Administração</NavLink>
          </nav>
        </div>
      </header>
      <main id="main-content">
        <Outlet />
      </main>
      <footer className="site-footer">
        <div className="page-shell site-footer__inner">
          <span>Bartzeit</span>
          <span>Catálogo conectado à API · horários sujeitos à confirmação</span>
        </div>
      </footer>
    </>
  );
}
