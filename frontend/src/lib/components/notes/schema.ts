import { z } from 'zod';
import { required } from '$lib/forms/schemas';
import { m } from '$lib/paraglide/messages';

// Limits mirror the backend's NoteDtos.kt.
export const noteSchema = z.object({
	title: required(200),
	content: z.string().max(20_000, { error: () => m.validation_too_long() }),
	pinned: z.boolean(),
});

export type NoteValues = z.infer<typeof noteSchema>;

export const emptyNote: NoteValues = { title: '', content: '', pinned: false };
