import type { RequestHandler } from './$types';

/** Container health check: answers without calling the API, so a slow backend can't fail it. */
export const GET: RequestHandler = () => new Response('ok', { headers: { 'Cache-Control': 'no-store' } });
