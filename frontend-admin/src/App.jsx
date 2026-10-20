import { useEffect } from "react";

export default function App() {
  useEffect(() => {
    const destination = window.location.port === "5174"
      ? `${window.location.protocol}//${window.location.hostname}:5173`
      : `${window.location.origin}/`;
    if (window.location.href !== destination) window.location.replace(destination);
  }, []);

  return <p>Ouverture du portail BNA Digital Banking…</p>;
}
