<script lang="ts">
	import { mode } from 'mode-watcher';
	import { onMount } from 'svelte';
	import { api, call } from '$lib/api/client';
	import { errorMessage } from '$lib/api/errors';
	import { session } from '$lib/auth/session.svelte';
	import { getLocale } from '$lib/paraglide/runtime';

	interface Props {
		clientId: string;
		onSuccess: () => void;
		onError: (message: string) => void;
	}

	interface GoogleIdentity {
		accounts: {
			id: {
				initialize(options: {
					client_id: string;
					callback: (response: { credential: string }) => void;
					ux_mode?: 'popup';
				}): void;
				renderButton(element: HTMLElement, options: Record<string, unknown>): void;
			};
		};
	}

	let { clientId, onSuccess, onError }: Props = $props();
	let container: HTMLDivElement;

	const SCRIPT_SRC = 'https://accounts.google.com/gsi/client';

	function loadScript(): Promise<GoogleIdentity> {
		const existing = (window as unknown as { google?: GoogleIdentity }).google;
		if (existing) return Promise.resolve(existing);
		return new Promise((resolve, reject) => {
			const script = document.createElement('script');
			script.src = SCRIPT_SRC;
			script.async = true;
			script.onload = () => resolve((window as unknown as { google: GoogleIdentity }).google);
			script.onerror = reject;
			document.head.appendChild(script);
		});
	}

	/** Google hands us an ID token; the backend verifies it and returns our own session. */
	async function handleCredential({ credential }: { credential: string }) {
		try {
			session.signIn(
				await call(api.POST('/api/v1/auth/google', { body: { idToken: credential, locale: getLocale() } })),
			);
			onSuccess();
		} catch (error) {
			onError(errorMessage(error));
		}
	}

	onMount(() => {
		let cancelled = false;
		loadScript()
			.then((google) => {
				if (cancelled) return;
				google.accounts.id.initialize({ client_id: clientId, callback: handleCredential, ux_mode: 'popup' });
				google.accounts.id.renderButton(container, {
					theme: mode.current === 'dark' ? 'filled_black' : 'outline',
					size: 'large',
					text: 'continue_with',
					shape: 'rectangular',
					width: Math.min(container.clientWidth || 320, 400),
					locale: getLocale(),
				});
			})
			.catch(() => onError(''));
		return () => {
			cancelled = true;
		};
	});
</script>

<div bind:this={container} class="flex min-h-10 w-full justify-center"></div>
