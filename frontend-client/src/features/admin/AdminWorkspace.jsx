import { useCallback, useEffect, useMemo, useState } from "react";
import { AlertCircle, RefreshCw } from "lucide-react";
import api from "../../services/api";
import AdminApplicationDetail from "./AdminApplicationDetail";
import AdminApplications from "./AdminApplications";
import AdminDashboard from "./AdminDashboard";
import AdminParameters from "./AdminParameters";
import AdminSidebar from "./AdminSidebar";
import AdminTopbar from "./AdminTopbar";

function messageFrom(error) {
  return error.response?.data?.detail || error.response?.data?.error || error.response?.data?.message
    || "Impossible de contacter le service.";
}

export default function AdminWorkspace({ user, onLogout }) {
  const [section, setSection] = useState("dashboard");
  const [parameters, setParameters] = useState(null);
  const [margins, setMargins] = useState({});
  const [tmm, setTmm] = useState("");
  const [applications, setApplications] = useState([]);
  const [selected, setSelected] = useState(null);
  const [note, setNote] = useState("");
  const [decisionReason, setDecisionReason] = useState("");
  const [decision, setDecision] = useState("APPROVED");
  const [busy, setBusy] = useState(false);
  const [applicationsLoading, setApplicationsLoading] = useState(true);
  const [parametersLoading, setParametersLoading] = useState(true);
  const [initialLoadError, setInitialLoadError] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [notice, setNotice] = useState("");
  const [noticeKind, setNoticeKind] = useState("error");

  const loadApplications = useCallback(async () => {
    const { data } = await api.get("/admin/applications");
    setApplications(data);
  }, []);

  const loadParameters = useCallback(async () => {
    const { data } = await api.get("/admin/parameters");
    setParameters(data);
    setTmm(String(data.tmmPercent ?? ""));
    setMargins(Object.fromEntries(data.margins.map((item) => [item.id, {
      marginPercent: String(item.marginPercent),
      maxMonths: String(item.maxMonths),
    }])));
  }, []);

  useEffect(() => {
    let active = true;
    async function loadInitialData() {
      const results = await Promise.allSettled([loadApplications(), loadParameters()]);
      if (!active) return;
      setApplicationsLoading(false);
      setParametersLoading(false);
      const failed = results.find((result) => result.status === "rejected");
      if (failed) {
        setInitialLoadError(true);
        setNotice(messageFrom(failed.reason));
        setNoticeKind("error");
      }
    }
    loadInitialData();
    return () => { active = false; };
  }, [loadApplications, loadParameters]);

  async function retryInitialLoad() {
    setApplicationsLoading(true);
    setParametersLoading(true);
    const results = await Promise.allSettled([loadApplications(), loadParameters()]);
    setApplicationsLoading(false);
    setParametersLoading(false);
    const failed = results.find((result) => result.status === "rejected");
    setInitialLoadError(Boolean(failed));
    if (failed) {
      setNotice(messageFrom(failed.reason));
      setNoticeKind("error");
    } else {
      setNotice("");
    }
  }

  async function openApplication(application) {
    setBusy(true);
    setNotice("");
    try {
      const { data } = await api.get(`/admin/applications/${application.id}`);
      setSelected(data);
      setNote("");
      setDecisionReason("");
      setSection("detail");
    } catch (error) {
      setNotice(messageFrom(error));
      setNoticeKind("error");
    } finally {
      setBusy(false);
    }
  }

  async function updateApplication(action, payload = {}) {
    if (!selected) return;
    setBusy(true);
    setNotice("");
    try {
      const { data } = await api.post(`/admin/applications/${selected.id}/${action}`, payload);
      setSelected(data);
      await loadApplications();
      setNote("");
      setDecisionReason("");
      setNotice("Action enregistrée dans l’historique.");
      setNoticeKind("success");
    } catch (error) {
      setNotice(messageFrom(error));
      setNoticeKind("error");
    } finally {
      setBusy(false);
    }
  }

  async function evaluateAi() {
    if (!selected) return;
    setBusy(true);
    setNotice("");
    try {
      const { data } = await api.post(`/admin/applications/${selected.id}/ai-evaluation`);
      setSelected((current) => current ? { ...current, ...data } : current);
      await loadApplications();
      setNotice(data.aiEvaluation?.status === "SUCCESS"
        ? "Analyse IA enregistrée. Elle reste indépendante de la décision humaine."
        : `Analyse IA terminée avec le statut ${data.aiEvaluation?.status ?? "inconnu"} ; le scoring Java reste inchangé.`);
      setNoticeKind(data.aiEvaluation?.status === "SUCCESS" ? "success" : "error");
    } catch (error) {
      setNotice(messageFrom(error));
      setNoticeKind("error");
    } finally {
      setBusy(false);
    }
  }

  async function saveTmm(event) {
    event.preventDefault();
    setBusy(true);
    setNotice("");
    try {
      await api.put("/admin/parameters/tmm", { valuePercent: Number(tmm) });
      await loadParameters();
      setNotice("TMM mis à jour.");
      setNoticeKind("success");
    } catch (error) {
      setNotice(messageFrom(error));
      setNoticeKind("error");
    } finally {
      setBusy(false);
    }
  }

  async function saveMargin(item) {
    setBusy(true);
    setNotice("");
    try {
      await api.put("/admin/parameters/margins", {
        scenario: item.scenario,
        creditType: item.creditType,
        marginPercent: Number(margins[item.id].marginPercent),
        maxMonths: Number(margins[item.id].maxMonths),
      });
      await loadParameters();
      setNotice(`Paramètres enregistrés pour « ${item.creditType} ».`);
      setNoticeKind("success");
      return true;
    } catch (error) {
      setNotice(messageFrom(error));
      setNoticeKind("error");
      return false;
    } finally {
      setBusy(false);
    }
  }

  const dirtyMargins = useMemo(() => {
    if (!parameters) return {};
    return Object.fromEntries(parameters.margins.map((item) => [
      item.id,
      Number(margins[item.id]?.marginPercent) !== Number(item.marginPercent)
        || Number(margins[item.id]?.maxMonths) !== Number(item.maxMonths),
    ]));
  }, [margins, parameters]);

  function navigate(nextSection) {
    if (nextSection !== "detail") setSection(nextSection);
    setNotice("");
  }

  const title = {
    dashboard: "Vue d’ensemble",
    applications: "Demandes de crédit",
    detail: "Détail du dossier",
    parameters: "Paramètres bancaires",
  }[section] || "Administration";

  return (
    <div className="admin-shell">
      <AdminSidebar
        section={section}
        onNavigate={navigate}
        open={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
      />
      <div className="admin-main">
        <AdminTopbar
          section={section}
          user={user}
          onLogout={onLogout}
          onMenuToggle={() => setSidebarOpen((current) => !current)}
        />
        <main className="admin-content">
          {notice && <div className={`admin-notice ${noticeKind}`} role="status">
            {noticeKind === "error" ? <AlertCircle size={18} /> : <span className="admin-notice-success">✓</span>}
            <span>{notice}</span>
            {initialLoadError && <button className="admin-retry-button" type="button" onClick={retryInitialLoad}><RefreshCw size={15} /> Réessayer</button>}
            {noticeKind === "error" && <button type="button" onClick={() => setNotice("")} aria-label="Fermer le message">×</button>}
          </div>}
          {section === "dashboard" && <AdminDashboard
            applications={applications}
            loading={applicationsLoading}
            onOpen={openApplication}
            onNavigate={navigate}
          />}
          {section === "applications" && <AdminApplications
            applications={applications}
            loading={applicationsLoading}
            onOpen={openApplication}
          />}
          {section === "detail" && (selected ? <AdminApplicationDetail
            key={selected.id}
            application={selected}
            busy={busy}
            note={note}
            decision={decision}
            decisionReason={decisionReason}
            onBack={() => navigate("applications")}
            onNoteChange={setNote}
            onDecisionChange={setDecision}
            onDecisionReasonChange={setDecisionReason}
            onClaim={() => updateApplication("claim")}
            onRequestInfo={() => updateApplication("request-info", { note })}
            onDecide={() => updateApplication("decision", { decision, reason: decisionReason })}
            onEvaluateAi={evaluateAi}
          /> : <div className="card admin-loading-panel">Chargement du dossier…</div>)}
          {section === "parameters" && <AdminParameters
            parameters={parameters}
            loading={parametersLoading}
            tmm={tmm}
            margins={margins}
            dirtyMargins={dirtyMargins}
            tmmDirty={Boolean(parameters) && Number(tmm) !== Number(parameters.tmmPercent ?? "")}
            busy={busy}
            onTmmChange={setTmm}
            onMarginChange={(id, field, value) => setMargins((current) => ({
              ...current,
              [id]: { ...current[id], [field]: value },
            }))}
            onSaveTmm={saveTmm}
            onSaveMargin={saveMargin}
          />}
          {section !== "detail" && <footer className="admin-content-footer"><span>{title}</span><span>Les décisions de crédit demeurent sous responsabilité humaine.</span></footer>}
        </main>
      </div>
    </div>
  );
}
