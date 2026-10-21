<script lang="ts">
	import Trash2 from '@lucide/svelte/icons/trash-2';
	import { goto, invalidate } from '$app/navigation';
	import { toast } from 'svelte-sonner';
	import { api, call } from '$lib/api/client';
	import { ApiError, errorMessage } from '$lib/api/errors';
	import type { components } from '$lib/api/schema';
	import ConfirmDialog from '$lib/components/app/ConfirmDialog.svelte';
	import FormAlert from '$lib/components/app/FormAlert.svelte';
	import SubmitButton from '$lib/components/app/SubmitButton.svelte';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import { createForm } from '$lib/forms/form.svelte';
	import { formatDateTime, formatRelative } from '$lib/i18n/format';
	import { m } from '$lib/paraglide/messages';
	import NoteFields from './NoteFields.svelte';
	import { noteSchema } from './schema';

	/** Render inside {#key note.id} so opening another note starts a fresh form. */
	let { note }: { note: components['schemas']['NoteResponse'] } = $props();

	let stale = $state(false);
	let confirmingDelete = $state(false);

	const form = createForm({
		schema: noteSchema,
		initial: () => ({ title: note.title, content: note.content, pinned: note.pinned }),
		onSubmit: async (values) => {
			try {
				// `version` is what this edit started from: the API refuses to overwrite a newer one (409).
				await call(
					api.PUT('/api/v1/notes/{id}', {
						params: { path: { id: note.id } },
						body: { ...values, version: note.version },
					}),
				);
			} catch (error) {
				if (error instanceof ApiError && error.code === 'conflict.stale') stale = true;
				throw error;
			}
			toast.success(m.notes_saved());
			await invalidate('app:note'); // fresh `note` (new version and date) from the server
		},
	});

	/** After a conflict: load the other edit and start over from it. */
	async function reload() {
		await invalidate('app:note');
		form.reset();
		stale = false;
	}

	async function remove() {
		try {
			await call(api.DELETE('/api/v1/notes/{id}', { params: { path: { id: note.id } } }));
		} catch (error) {
			toast.error(errorMessage(error));
			throw error;
		}
		toast.success(m.notes_deleted());
		await goto('/notes', { replaceState: true });
	}
</script>

<Card.Root>
	<Card.Header>
		<Card.Title class="text-xl break-words">{form.values.title || note.title}</Card.Title>
		<Card.Description title={formatDateTime(note.updatedAt)}>
			{m.notes_updated({ time: formatRelative(note.updatedAt) })}
		</Card.Description>
		<Card.Action>
			<Button variant="ghost" size="icon" aria-label={m.common_delete()} onclick={() => (confirmingDelete = true)}>
				<Trash2 />
			</Button>
		</Card.Action>
	</Card.Header>
	<Card.Content>
		<form onsubmit={form.submit} novalidate class="space-y-6">
			<FormAlert message={form.formError} />
			{#if stale}
				<Button type="button" variant="outline" size="sm" onclick={reload}>{m.common_reload()}</Button>
			{/if}
			<NoteFields {form} />
			<div class="flex justify-end">
				<SubmitButton busy={form.submitting} disabled={stale}>{m.common_save()}</SubmitButton>
			</div>
		</form>
	</Card.Content>
</Card.Root>

<ConfirmDialog
	bind:open={confirmingDelete}
	title={m.notes_delete_title()}
	description={m.notes_delete_body()}
	confirmLabel={m.common_delete()}
	onConfirm={remove}
/>
