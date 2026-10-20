import { useEffect, useState } from "react";
import api, { fetchHealth } from "./services/api";
import AccountBudget from "./features/accounts/AccountBudget";
import ClientApplications from "./features/applications/ClientApplications";
import AdminWorkspace from "./features/admin/AdminWorkspace";
import CreditSimulator from "./features/simulation/CreditSimulator";
import "./styles.css";

const scenarios = [
  { value: "SANS_RACHAT", label: "Crédit sans rachat", endpoint: "/simulations/sans-rachat" },
  { value: "RACHAT_INTERNE", label: "Rachat de crédits BNA", endpoint: "/simulations/rachat-interne" },
  { value: "RACHAT_EXTERNE", label: "Rachat de crédits externe", endpoint: "/simulations/rachat-externe" },
];

const initialValues = {
  salaireMensuelMoyen: "",
  loyerMensuel: "0",
  retenuesSed: "0",
  retenuesCreditsBna: "0",
  retenuesCreditsBnaNonRachete: "0",
  capitalRestantARacheter: "0",
  dureeMois: "",
  montantCreditDemande: "",
};

function errorMessage(error) {
  return error.response?.data?.detail
    || error.response?.data?.error
    || error.response?.data?.message
    || (error.response?.status === 401 ? "Session expirée. Connectez-vous à nouveau." : "Impossible de contacter le service.");
}

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem("bna_user") || "null");
  } catch {
    return null;
  }
}

function App() {
  const [token, setToken] = useState(() => localStorage.getItem("bna_access_token"));
  const [user, setUser] = useState(readStoredUser);
  const [checkingSession, setCheckingSession] = useState(Boolean(localStorage.getItem("bna_access_token")));
  const [authMode, setAuthMode] = useState("login");
  const [authValues, setAuthValues] = useState({ fullName: "", email: "", password: "" });
  const [scenario, setScenario] = useState(scenarios[0].value);
  const [catalogue, setCatalogue] = useState({});
  const [values, setValues] = useState(initialValues);
  const [result, setResult] = useState(null);
  const [confirmationOpen, setConfirmationOpen] = useState(false);
  const [applications, setApplications] = useState([]);
  const [activeSection, setActiveSection] = useState("simulation");
  const [editingApplicationId, setEditingApplicationId] = useState(null);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");
  const [apiStatus, setApiStatus] = useState("Vérification de l’API…");

  useEffect(() => {
    fetchHealth()
      .then(() => setApiStatus("API disponible"))
      .catch(() => setApiStatus("API indisponible"));
  }, []);

  useEffect(() => {
    if (!token || user?.role !== "CLIENT") return;
    api.get("/simulations/catalogue", { params: { scenario } })
      .then(({ data }) => {
        setCatalogue(data);
        setValues((current) => {
          const typeCredit = data[current.typeCredit] ? current.typeCredit : Object.keys(data)[0] || "";
          const maxMonths = data[typeCredit]?.maxMonths;
          const duration = Number(current.dureeMois) > 0 && Number(current.dureeMois) <= maxMonths
            ? current.dureeMois
            : maxMonths?.toString() || "";
          return { ...current, typeCredit, dureeMois: duration };
        });
        setNotice("");
      })
      .catch((error) => setNotice(errorMessage(error)));
  }, [token, scenario, user?.role]);

  useEffect(() => {
    if (!token) return;
    let active = true;
    api.get("/users/me")
      .then(({ data }) => {
        if (!active) return;
        setUser(data);
        localStorage.setItem("bna_user", JSON.stringify(data));
        setCheckingSession(false);
      })
      .catch((error) => {
        if (!active) return;
        localStorage.removeItem("bna_access_token");
        localStorage.removeItem("bna_user");
        setToken(null);
        setUser(null);
        setCheckingSession(false);
        if (error.response?.status !== 401) setNotice(errorMessage(error));
      });
    return () => { active = false; };
  }, [token]);

  useEffect(() => {
    if (!token || user?.role !== "CLIENT") return;
    api.get("/applications")
      .then(({ data }) => setApplications(data))
      .catch((error) => setNotice(errorMessage(error)));
  }, [token, user?.role]);

  const scenarioInfo = scenarios.find((item) => item.value === scenario);

  function buildApplicationPayload() {
    return {
      scenario,
      creditType: values.typeCredit,
      requestedAmount: values.montantCreditDemande === "" ? null : Number(values.montantCreditDemande),
      durationMonths: Number(values.dureeMois),
      averageMonthlySalary: Number(values.salaireMensuelMoyen),
      halfRentIncome: Number(values.loyerMensuel || 0) / 2,
      sedDeductions: Number(values.retenuesSed || 0),
      bnaDeductions: Number(values.retenuesCreditsBna || 0),
      bnaNonBuyoutDeductions: Number(values.retenuesCreditsBnaNonRachete || 0),
      remainingBuyoutCapital: Number(values.capitalRestantARacheter || 0),
    };
  }

  async function handleAuth(event) {
    event.preventDefault();
    setBusy(true);
    setNotice("");
    try {
      const { data } = await api.post(
        authMode === "login" ? "/auth/login" : "/auth/register",
        authMode === "login"
          ? { email: authValues.email, password: authValues.password }
          : authValues,
      );
      localStorage.setItem("bna_access_token", data.accessToken);
      localStorage.setItem("bna_user", JSON.stringify(data.user));
      setToken(data.accessToken);
      setUser(data.user);
      setNotice(`Bienvenue ${data.user.fullName}.`);
    } catch (error) {
      setNotice(errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  async function handleSimulation(event) {
    event.preventDefault();
    setBusy(true);
    setNotice("");
    setResult(null);
    setConfirmationOpen(false);
    const payload = {
      salaireMensuelMoyen: Number(values.salaireMensuelMoyen),
      cinquantePourcentLoyers: Number(values.loyerMensuel || 0) / 2,
      retenuesSed: Number(values.retenuesSed || 0),
      typeCredit: values.typeCredit,
      dureeMois: Number(values.dureeMois),
      montantCreditDemande: values.montantCreditDemande === "" ? null : Number(values.montantCreditDemande),
    };
    if (scenario === "SANS_RACHAT") {
      payload.retenuesCreditsBna = Number(values.retenuesCreditsBna || 0);
    } else {
      payload.capitalRestantARacheter = Number(values.capitalRestantARacheter || 0);
      if (scenario === "RACHAT_INTERNE") {
        payload.retenuesCreditsBnaNonRachete = Number(values.retenuesCreditsBnaNonRachete || 0);
      }
    }

    try {
      const { data } = await api.post(scenarioInfo.endpoint, payload);
      setResult(data);
      setConfirmationOpen(false);
    } catch (error) {
      setNotice(errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  async function createAndSubmitApplication() {
    setBusy(true);
    setNotice("");
    let draft;
    try {
      const { data } = editingApplicationId
        ? await api.put(`/applications/${editingApplicationId}`, buildApplicationPayload())
        : await api.post("/applications", buildApplicationPayload());
      draft = data;
    } catch (error) {
      setNotice(errorMessage(error));
      setBusy(false);
      return;
    }
    try {
      await api.post(`/applications/${draft.id}/submit`);
    } catch (error) {
      setEditingApplicationId(draft.id);
      try {
        const { data } = await api.get("/applications");
        setApplications(data);
        setActiveSection("applications");
        setNotice(`Le brouillon a été enregistré, mais sa soumission a échoué : ${errorMessage(error)}`);
      } catch (refreshError) {
        setNotice(`La soumission a échoué : ${errorMessage(error)} L’actualisation des demandes a aussi échoué : ${errorMessage(refreshError)}`);
      }
      setBusy(false);
      return;
    }
    setEditingApplicationId(null);
    setConfirmationOpen(false);
    setActiveSection("applications");
    try {
      const { data } = await api.get("/applications");
      setApplications(data);
      setNotice(`Votre demande ${draft.reference} a été soumise.`);
    } catch (error) {
      setNotice(`La demande ${draft.reference} est soumise, mais la liste n’a pas pu être actualisée : ${errorMessage(error)}`);
    } finally {
      setBusy(false);
    }
  }

  async function saveDraft() {
    setBusy(true);
    setNotice("");
    let draft;
    try {
      const { data } = editingApplicationId
        ? await api.put(`/applications/${editingApplicationId}`, buildApplicationPayload())
        : await api.post("/applications", buildApplicationPayload());
      draft = data;
      setEditingApplicationId(draft.id);
    } catch (error) {
      setNotice(errorMessage(error));
      setBusy(false);
      return;
    }
    try {
      const { data } = await api.get("/applications");
      setApplications(data);
      setEditingApplicationId(null);
      setActiveSection("applications");
      setNotice("Votre brouillon a été enregistré.");
    } catch (error) {
      setNotice(`Le brouillon ${draft.reference} est enregistré, mais la liste n’a pas pu être actualisée : ${errorMessage(error)}`);
    } finally {
      setBusy(false);
    }
  }

  function editDraft(application) {
    setScenario(application.scenario);
    setValues({
      salaireMensuelMoyen: application.averageMonthlySalary,
      loyerMensuel: Number(application.halfRentIncome || 0) * 2,
      retenuesSed: application.sedDeductions,
      retenuesCreditsBna: application.bnaDeductions,
      retenuesCreditsBnaNonRachete: application.bnaNonBuyoutDeductions,
      capitalRestantARacheter: application.remainingBuyoutCapital,
      dureeMois: String(application.durationMonths),
      montantCreditDemande: application.requestedAmount ?? "",
      typeCredit: application.creditType,
    });
    setEditingApplicationId(application.id);
    setResult(null);
    setConfirmationOpen(false);
    setActiveSection("simulation");
  }

  async function provideAdditionalInfo(application, note) {
    setBusy(true);
    setNotice("");
    try {
      await api.post(`/applications/${application.id}/provide-info`, { note });
      const { data } = await api.get("/applications");
      setApplications(data);
      setNotice("Votre complément a été transmis.");
      return true;
    } catch (error) {
      setNotice(errorMessage(error));
      return false;
    } finally {
      setBusy(false);
    }
  }

  async function withdrawApplication(application) {
    setBusy(true);
    setNotice("");
    try {
      await api.post(`/applications/${application.id}/withdraw`);
      const { data } = await api.get("/applications");
      setApplications(data);
      setNotice("Votre demande a été retirée.");
    } catch (error) {
      setNotice(errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  function logout() {
    localStorage.removeItem("bna_access_token");
    localStorage.removeItem("bna_user");
    setToken(null);
    setUser(null);
    setResult(null);
    setApplications([]);
    setEditingApplicationId(null);
    setCatalogue({});
    setNotice("");
  }

  function updateValue(event) {
    setValues((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  return (
    <main className="client-app">
      <header className="topbar">
        <a className="brand" href="#accueil">BNA <span>Digital Banking</span></a>
        <div className="topbar-right">
          <span className="api-indicator"><i />{apiStatus}</span>
          {user && <span className="signed-in-user">{user.fullName} · {user.role === "ADMIN" ? "Administration" : "Client"}</span>}
          {token && <button className="button button-quiet" onClick={logout}>Se déconnecter</button>}
        </div>
      </header>
      {!token ? (
        <section className="auth-layout">
          <div className="intro-panel">
            <p className="eyebrow">Espace BNA Digital Banking</p>
            <h1>Vos services bancaires, dans un espace unique.</h1>
            <p>Connectez-vous pour accéder à votre espace client ou administrateur selon les droits de votre compte.</p>
            <div className="info-note">Les simulations sont indicatives. L’inscription publique crée uniquement des comptes clients.</div>
          </div>
          <form className="card auth-card" onSubmit={handleAuth}>
            <div className="tab-switch" role="tablist" aria-label="Accès au compte">
              <button type="button" className={authMode === "login" ? "active" : ""} onClick={() => setAuthMode("login")}>Connexion</button>
              <button type="button" className={authMode === "register" ? "active" : ""} onClick={() => setAuthMode("register")}>Créer un compte</button>
            </div>
            <h2>{authMode === "login" ? "Se connecter" : "Créer un espace client"}</h2>
            {authMode === "register" && <label>Nom complet
              <input required maxLength="120" autoComplete="name" value={authValues.fullName} onChange={(event) => setAuthValues({ ...authValues, fullName: event.target.value })} />
            </label>}
            <label>Adresse e-mail
              <input required type="email" autoComplete="email" value={authValues.email} onChange={(event) => setAuthValues({ ...authValues, email: event.target.value })} />
            </label>
            <label>Mot de passe
              <input required type="password" minLength={authMode === "register" ? 12 : 1} autoComplete={authMode === "login" ? "current-password" : "new-password"} value={authValues.password} onChange={(event) => setAuthValues({ ...authValues, password: event.target.value })} />
              {authMode === "register" && <small>12 caractères minimum.</small>}
            </label>
            {notice && <p className="notice" role="alert">{notice}</p>}
            <button className="button button-primary button-full" disabled={busy}>{busy ? "Veuillez patienter…" : authMode === "login" ? "Se connecter" : "Créer mon compte"}</button>
          </form>
        </section>
      ) : checkingSession || !user ? (
        <div className="workspace"><div className="card loading-card">Vérification de votre session…</div></div>
      ) : user.role === "ADMIN" ? (
        <AdminWorkspace />
      ) : user.role === "CLIENT" ? (
        <div className="workspace">
          <section className="page-heading">
            <p className="eyebrow">Espace client</p>
            <h1>{activeSection === "accounts" ? "Mes comptes et mon budget" : "Vos demandes de crédit"}</h1>
            <p>{activeSection === "accounts"
              ? "Consultez vos comptes fictifs et suivez vos opérations et objectifs mensuels."
              : "Simulez, vérifiez les informations puis soumettez votre demande."}</p>
          </section>
          <div className="tab-switch workspace-tabs" role="tablist" aria-label="Espace client">
            <button type="button" className={activeSection === "simulation" ? "active" : ""} onClick={() => setActiveSection("simulation")}>Demander un crédit</button>
            <button type="button" className={activeSection === "applications" ? "active" : ""} onClick={() => setActiveSection("applications")}>Mes demandes ({applications.length})</button>
            <button type="button" className={activeSection === "accounts" ? "active" : ""} onClick={() => setActiveSection("accounts")}>Mes comptes & budget</button>
          </div>
          {notice && <p className="notice page-notice" role="alert">{notice}</p>}
          {activeSection === "accounts" ? <AccountBudget /> : activeSection === "applications" ? <ClientApplications
            applications={applications}
            busy={busy}
            onEditDraft={editDraft}
            onResumeDraft={editDraft}
            onProvideInfo={provideAdditionalInfo}
            onWithdraw={withdrawApplication}
          /> : <CreditSimulator
            scenarios={scenarios}
            scenario={scenario}
            onScenarioChange={(event) => {
              setScenario(event.target.value);
              setResult(null);
              setConfirmationOpen(false);
            }}
            catalogue={catalogue}
            values={values}
            onValueChange={updateValue}
            onProductChange={(event) => {
              const selected = catalogue[event.target.value];
              setValues((current) => ({ ...current, typeCredit: event.target.value, dureeMois: selected?.maxMonths?.toString() || "" }));
              setResult(null);
              setConfirmationOpen(false);
            }}
            isBuyout={scenario !== "SANS_RACHAT"}
            busy={busy}
            result={result}
            onSimulate={handleSimulation}
            onSaveDraft={saveDraft}
            onBeginConfirmation={() => setConfirmationOpen(true)}
            onConfirmSubmission={createAndSubmitApplication}
            confirming={confirmationOpen}
            editing={Boolean(editingApplicationId)}
          />}
        </div>
      ) : <div className="workspace"><p className="notice" role="alert">Rôle non reconnu. Déconnectez-vous puis contactez l’administrateur.</p></div>}
    </main>
  );
}

export default App;
