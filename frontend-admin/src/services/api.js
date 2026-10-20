import axios from "axios";

const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080/api", timeout: 5000 });

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("bna_access_token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export const fetchHealth = () => api.get("/health");
export default api;
