<script lang="ts">
	import NotebookPen from '@lucide/svelte/icons/notebook-pen';
	import Pin from '@lucide/svelte/icons/pin';
	import Plus from '@lucide/svelte/icons/plus';
	import Search from '@lucide/svelte/icons/search';
	import { goto, invalidate } from '$app/navigation';
	import { page } from '$app/state';
	import { toast } from 'svelte-sonner';
	import { api, call } from '$lib/api/client';
	import FormAlert from '$lib/components/app/FormAlert.svelte';
	import PageHeader from '$lib/components/app/PageHeader.svelte';
	import PaginationBar from '$lib/components/app/PaginationBar.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import SubmitButton from '$lib/components/app/SubmitButton.svelte';
	import NoteFields from '$lib/components/notes/NoteFields.svelte';
	import { emptyNote, noteSchema } from '$lib/components/notes/schema';
	import { Badge } from '$lib/components/ui/badge';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import * as Dialog from '$lib/components/ui/dialog';
	import * as Empty from '$lib/components/ui/empty';
	import { Input } from '$lib/components/ui/input';
	import { debounce } from '$lib/debounce';
	import { createForm } from '$lib/forms/form.svelte';
	import { formatRelative } from '$lib/i18n/format';
	import { withQuery } from '$lib/navigation';
	import { m } from '$lib/paraglide/messages';

	let { data } = $props();

	let creating = $state(false);

	const search = debounce((value: string) => {
		void goto(withQuery(page.url, { q: value.trim(), page: null }), {
			keepFocus: true,
			noScroll: true,
			replaceState: true,
		});
	});

	const form = createForm({
		schema: noteSchema,
		initial: emptyNote,
		onSubmit: async (values) => {
			await call(api.POST('/api/v1/notes', { body: values }));
			toast.success(m.notes_created());
			creating = false;
			form.reset();
			await invalidate('app:notes');
		},
	});
</script>

<Seo title={m.notes_title()} />

<PageHeader title={m.notes_title()} description={m.notes_subtitle()}>
	{#snippet actions()}
		<Button onclick={() => (creating = true)}><Plus />{m.notes_new()}</Button>
	{/snippet}
</PageHeader>

<div class="mb-6 flex flex-wrap items-center justify-between gap-3">
	<div class="relative w-full sm:max-w-xs">
		<Search class="pointer-events-none absolute start-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
		<Input
			type="search"
			class="ps-9"
			placeholder={m.notes_search()}
			aria-label={m.notes_search()}
			value={data.q}
			oninput={(event) => search(event.currentTarget.value)}
		/>
	</div>
	<p class="text-sm text-muted-foreground">{m.notes_count({ count: data.notes.totalItems })}</p>
</div>

{#if data.notes.items.length === 0}
	<Empty.Root class="border border-dashed">
		<Empty.Header>
			<Empty.Media variant="icon"><NotebookPen /></Empty.Media>
			{#if data.q}
				<Empty.Title>{m.notes_no_results()}</Empty.Title>
			{:else}
				<Empty.Title>{m.notes_empty_title()}</Empty.Title>
				<Empty.Description>{m.notes_empty_body()}</Empty.Description>
			{/if}
		</Empty.Header>
		{#if !data.q}
			<Empty.Content>
				<Button onclick={() => (creating = true)}><Plus />{m.notes_new()}</Button>
			</Empty.Content>
		{/if}
	</Empty.Root>
{:else}
	<div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
		{#each data.notes.items as note (note.id)}
			<a
				href="/notes/{note.id}"
				class="group rounded-xl outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50"
			>
				<Card.Root class="h-full transition-colors group-hover:border-primary/40">
					<Card.Header>
						<Card.Title class="line-clamp-1">{note.title}</Card.Title>
						{#if note.pinned}
							<Card.Action>
								<Badge variant="secondary"><Pin />{m.notes_pinned()}</Badge>
							</Card.Action>
						{/if}
						<Card.Description>{m.notes_updated({ time: formatRelative(note.updatedAt) })}</Card.Description>
					</Card.Header>
					{#if note.content}
						<Card.Content>
							<p class="line-clamp-3 text-sm whitespace-pre-line text-muted-foreground">{note.content}</p>
						</Card.Content>
					{/if}
				</Card.Root>
			</a>
		{/each}
	</div>
	<div class="mt-6">
		<PaginationBar
			page={data.notes.page}
			size={data.notes.size}
			totalItems={data.notes.totalItems}
			onPageChange={(next) => goto(withQuery(page.url, { page: next || null }))}
		/>
	</div>
{/if}

<Dialog.Root bind:open={creating}>
	<Dialog.Content class="sm:max-w-lg">
		<Dialog.Header>
			<Dialog.Title>{m.notes_new()}</Dialog.Title>
		</Dialog.Header>
		<form onsubmit={form.submit} novalidate class="space-y-6">
			<FormAlert message={form.formError} />
			<NoteFields {form} idPrefix="new-note" />
			<Dialog.Footer>
				<Button type="button" variant="outline" onclick={() => (creating = false)}>{m.common_cancel()}</Button>
				<SubmitButton busy={form.submitting}>{m.common_create()}</SubmitButton>
			</Dialog.Footer>
		</form>
	</Dialog.Content>
</Dialog.Root>
