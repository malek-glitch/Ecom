<script lang="ts">
	import { Checkbox } from '$lib/components/ui/checkbox';
	import * as Field from '$lib/components/ui/field';
	import { Input } from '$lib/components/ui/input';
	import { Textarea } from '$lib/components/ui/textarea';
	import type { Form } from '$lib/forms/form.svelte';
	import { m } from '$lib/paraglide/messages';
	import type { NoteValues } from './schema';

	/** Shared by the "new note" dialog and the edit page. */
	let { form, idPrefix = 'note' }: { form: Form<NoteValues>; idPrefix?: string } = $props();
</script>

<Field.Group>
	<Field.Field data-invalid={!!form.errors.title}>
		<Field.Label for="{idPrefix}-title">{m.notes_field_title()}</Field.Label>
		<Input
			id="{idPrefix}-title"
			bind:value={form.values.title}
			aria-invalid={!!form.errors.title}
			maxlength={200}
			required
		/>
		<Field.Error errors={form.fieldErrors('title')} />
	</Field.Field>
	<Field.Field data-invalid={!!form.errors.content}>
		<Field.Label for="{idPrefix}-content">{m.notes_field_content()}</Field.Label>
		<Textarea
			id="{idPrefix}-content"
			bind:value={form.values.content}
			aria-invalid={!!form.errors.content}
			class="min-h-40"
			maxlength={20000}
		/>
		<Field.Error errors={form.fieldErrors('content')} />
	</Field.Field>
	<Field.Field orientation="horizontal">
		<Checkbox id="{idPrefix}-pinned" bind:checked={form.values.pinned} />
		<Field.Label for="{idPrefix}-pinned" class="font-normal">{m.notes_field_pinned()}</Field.Label>
	</Field.Field>
</Field.Group>
