/** "Firefox · Windows" from a User-Agent header: enough to recognise a device in a sessions list. */
export function describeUserAgent(userAgent: string | null | undefined): string | null {
	if (!userAgent) return null;
	const ua = userAgent;
	// Order matters: Edge and Opera also claim to be Chrome, and Chrome claims to be Safari.
	const browser = /Edg(e|A|iOS)?\//.test(ua)
		? 'Edge'
		: /OPR\/|Opera/.test(ua)
			? 'Opera'
			: /Firefox\/|FxiOS/.test(ua)
				? 'Firefox'
				: /Chrome\/|CriOS/.test(ua)
					? 'Chrome'
					: /Safari\//.test(ua)
						? 'Safari'
						: null;
	// Android also says Linux, so it comes first.
	const os = /Android/.test(ua)
		? 'Android'
		: /iPhone|iPad|iPod/.test(ua)
			? 'iOS'
			: /Windows/.test(ua)
				? 'Windows'
				: /Mac OS X|Macintosh/.test(ua)
					? 'macOS'
					: /CrOS/.test(ua)
						? 'ChromeOS'
						: /Linux/.test(ua)
							? 'Linux'
							: null;
	if (browser && os) return `${browser} · ${os}`;
	return browser ?? os ?? (ua.length > 60 ? `${ua.slice(0, 57)}…` : ua);
}
