// Applies the saved light/dark theme before the first paint, so pages don't flash white.
// mode-watcher takes over once the app starts. A file rather than an inline script: the
// Content-Security-Policy allows scripts from 'self' only, and the SPA build can't use nonces.
(() => {
	try {
		const mode = localStorage.getItem('mode-watcher-mode');
		const dark = mode === 'dark' || (mode !== 'light' && matchMedia('(prefers-color-scheme: dark)').matches);
		document.documentElement.classList.toggle('dark', dark);
		document.documentElement.style.colorScheme = dark ? 'dark' : 'light';
	} catch {
		// Storage blocked: keep the default theme.
	}
})();
