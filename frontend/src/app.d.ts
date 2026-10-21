// See https://svelte.dev/docs/kit/types#app.d.ts
declare global {
	namespace App {
		interface Error {
			message: string;
			code?: string;
		}
	}

	/** True in `npm run build:spa` builds (static files, no server). */
	const __SPA__: boolean;
}

export {};
