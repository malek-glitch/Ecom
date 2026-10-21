/** Calls `fn` once calls have stopped for `ms` (search boxes). */
export function debounce<A extends unknown[]>(fn: (...args: A) => void, ms = 300) {
	let timer: ReturnType<typeof setTimeout> | undefined;
	return (...args: A) => {
		clearTimeout(timer);
		timer = setTimeout(() => fn(...args), ms);
	};
}
