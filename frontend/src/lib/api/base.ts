/**
 * Where the browser sends API calls. Empty (the default) means "same origin": the Vite dev
 * server proxies /api in development and the reverse proxy (Traefik) routes it in production. Set VITE_API_URL at
 * build time only when the app runs on another origin (a Capacitor build, a separate API domain).
 */
export const API_BASE: string = (import.meta.env.VITE_API_URL ?? '').replace(/\/+$/, '');

export const apiUrl = (path: string) => `${API_BASE}${path}`;
