<script lang="ts">
	import ArrowLeft from '@lucide/svelte/icons/arrow-left';
	import Seo from '$lib/components/app/Seo.svelte';
	import NoteEditor from '$lib/components/notes/NoteEditor.svelte';
	import { Button } from '$lib/components/ui/button';
	import { m } from '$lib/paraglide/messages';

	let { data } = $props();
</script>

<Seo title={data.note.title} />

<div class="mx-auto max-w-3xl">
	<Button variant="ghost" size="sm" href="/notes" class="-ms-2 mb-4">
		<ArrowLeft class="rtl:rotate-180" />
		{m.notes_back()}
	</Button>
	{#key data.note.id}
		<NoteEditor note={data.note} />
	{/key}
</div>
