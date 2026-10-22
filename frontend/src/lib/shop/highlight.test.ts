import { describe, expect, it } from 'vitest';
import { highlight } from './highlight';

const marked = (text: string, query: string) =>
	highlight(text, query)
		.filter((s) => s.match)
		.map((s) => s.text);

describe('highlight', () => {
	it('marks word prefixes, ignoring case and accents', () => {
		expect(marked('Crème Brûlée Set', 'creme brul')).toEqual(['Crème', 'Brûl']);
		expect(marked('Cast Iron Skillet', 'IRON')).toEqual(['Iron']);
	});

	it('only marks the start of words', () => {
		expect(marked('Notebook and note', 'book')).toEqual([]);
		expect(marked('Notebook and note', 'note')).toEqual(['Note', 'note']);
	});

	it('keeps the text intact', () => {
		const text = 'Café <b>Noir</b> 250g';
		expect(
			highlight(text, 'noir 250')
				.map((s) => s.text)
				.join(''),
		).toBe(text);
		expect(highlight(text, '')).toEqual([{ text, match: false }]);
		expect(marked('قهوة عربية', 'عربي')).toEqual(['عربي']);
	});
});
