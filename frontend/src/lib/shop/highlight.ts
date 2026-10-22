/** A piece of text, marked when it matches the search. Rendered as text, never as HTML. */
export interface Segment {
	text: string;
	match: boolean;
}

const fold = (char: string) => char.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();

/** The words of a query, as the backend sees them (letters and digits only). */
export const queryWords = (query: string) =>
	(query.toLowerCase().match(/[\p{L}\p{N}]+/gu) ?? []).map((w) => [...w].map(fold).join(''));

/**
 * Splits [text] into segments, marking where a query word appears at the start of a word,
 * ignoring case and accents ("creme" marks "Crème"). Typo matches simply aren't marked.
 */
export function highlight(text: string, query: string): Segment[] {
	const words = queryWords(query).filter(Boolean);
	if (!words.length) return [{ text, match: false }];
	// Fold the text character by character, remembering where each folded character came from.
	const chars = [...text];
	let folded = '';
	const origin: number[] = [];
	chars.forEach((char, index) => {
		for (const f of fold(char)) {
			folded += f;
			origin.push(index);
		}
	});
	const marked = new Array<boolean>(chars.length).fill(false);
	for (const word of words) {
		let from = 0;
		for (let at = folded.indexOf(word, from); at !== -1; at = folded.indexOf(word, from)) {
			const startsWord = at === 0 || !/[\p{L}\p{N}]/u.test(folded[at - 1]);
			if (startsWord) for (let i = at; i < at + word.length; i++) marked[origin[i]] = true;
			from = at + 1;
		}
	}
	const segments: Segment[] = [];
	chars.forEach((char, index) => {
		const last = segments.at(-1);
		if (last && last.match === marked[index]) last.text += char;
		else segments.push({ text: char, match: marked[index] });
	});
	return segments;
}
