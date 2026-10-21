# Mobile apps with Capacitor

The frontend can be built as a static single-page app, then wrapped by
[Capacitor](https://capacitorjs.com) into Android and iOS apps. The app keeps the same code and
calls the same API over HTTPS.

## 1. Build the single-page app

The app must know where the API is, because it no longer shares an origin with it:

```bash
cd frontend
VITE_API_URL=https://app.example.com npm run build:spa    # → frontend/build/ (index.html + assets)
```

`build:spa` uses the static adapter with an `index.html` fallback and renders every page in the browser.
`VITE_API_URL` is also added to the Content-Security-Policy's `connect-src`, which the build writes into a `<meta>` tag.

## 2. Allow the app's origin on the API

The app's WebView runs on its own origin, so the API must allow it through CORS. In the
server's `.env`:

```bash
CORS_ALLOWED_ORIGINS=capacitor://localhost,https://localhost   # iOS, Android
```

Restart the backend. The API uses bearer tokens, not cookies, so no other CORS settings are needed.

## 3. Add Capacitor

```bash
cd frontend
npm install @capacitor/core @capacitor/android @capacitor/ios
npm install -D @capacitor/cli
npx cap init "Acme" com.acme.app --web-dir build
npx cap add android
npx cap add ios
```

After each `build:spa`, copy the files into the native projects, then open them in Android Studio or Xcode:

```bash
npx cap sync
npx cap open android    # or: npx cap open ios
```

## 4. Token storage

Tokens go through the `TokenStorage` interface (`src/lib/auth/storage.ts`). The default,
`localStorage`, works in Capacitor's WebView. The OS can clear WebView storage when space runs
low, though, which signs the user out. To keep tokens in native storage instead, use
[`@capacitor/preferences`](https://capacitorjs.com/docs/apis/preferences) with an in-memory
mirror. Preferences is asynchronous, so the stored session is restored once before the app starts:

```ts
// src/lib/auth/preferences-storage.ts
import { Preferences } from '@capacitor/preferences';
import { SESSION_STORAGE_KEY } from '$lib/config';
import type { StoredSession, TokenStorage } from './storage';

export class PreferencesTokenStorage implements TokenStorage {
	private value: StoredSession | null = null;

	/** Call once before the app starts (see hooks.client.ts). */
	async restore() {
		const { value } = await Preferences.get({ key: SESSION_STORAGE_KEY });
		this.value = value ? (JSON.parse(value) as StoredSession) : null;
	}

	load() {
		return this.value;
	}

	save(session: StoredSession) {
		this.value = session;
		void Preferences.set({ key: SESSION_STORAGE_KEY, value: JSON.stringify(session) });
	}

	clear() {
		this.value = null;
		void Preferences.remove({ key: SESSION_STORAGE_KEY });
	}

	subscribe() {
		return () => {}; // one WebView, no other tabs to sync with
	}
}

export const preferencesStorage = new PreferencesTokenStorage();
```

```ts
// src/hooks.client.ts: SvelteKit runs `init` once, before the first page loads
import { Capacitor } from '@capacitor/core';
import { preferencesStorage } from '$lib/auth/preferences-storage';

export const init = async () => {
	if (Capacitor.isNativePlatform()) await preferencesStorage.restore();
};
```

Then pick the storage where the session is created, in `src/lib/auth/session.svelte.ts`:

```ts
import { Capacitor } from '@capacitor/core';
import { preferencesStorage } from './preferences-storage';

export const session = new Session(
	!browser ? new MemoryTokenStorage() : Capacitor.isNativePlatform() ? preferencesStorage : new LocalTokenStorage(),
);
```

## 5. Google sign-in

Google's web button doesn't work inside an app's WebView: Google blocks embedded browsers.

1. Use a native Google sign-in plugin to get a Google **ID token**.
2. Send the token to the existing endpoint:

   ```ts
   session.signIn(await call(api.POST('/api/v1/auth/google', { body: { idToken, locale: getLocale() } })));
   ```

The backend accepts ID tokens whose audience is `GOOGLE_CLIENT_ID`, your web client ID. Configure
the plugin to request tokens for that client (plugins usually call it `webClientId` or
`serverClientId`). If your platform only issues tokens for its own client ID, extend
`GoogleTokenVerifier` to accept that ID too.

Hide the web button in native builds, for example with `Capacitor.isNativePlatform()`.

## 6. Email links

Confirmation, reset and invitation emails link to `APP_URL`, so they open in the browser by default.
That works: the user finishes the step on the website, then signs in to the app.

To open the links in the app instead:

1. Set up Android App Links and iOS Universal Links for your domain.
2. Route incoming URLs inside the app:

```ts
import { App } from '@capacitor/app';
import { goto } from '$app/navigation';

App.addListener('appUrlOpen', ({ url }) => {
	const { pathname, search } = new URL(url);
	void goto(pathname + search);
});
```

## 7. Polish

- **Safe areas:** add `viewport-fit=cover` to the viewport meta in `src/app.html`. Pad fixed bars with `env(safe-area-inset-top)` and `env(safe-area-inset-bottom)`.
- **Status bar and splash screen:** use `@capacitor/status-bar` and `@capacitor/splash-screen`.
- **Versions:** release a new app build when the API changes in a way older builds can't handle. Keep API changes backward-compatible for a while, because users update late.
