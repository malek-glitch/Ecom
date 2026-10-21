import type { z } from 'zod';
import { ApiError, errorMessage, fieldErrorMessage } from '$lib/api/errors';

type FieldErrors<T> = Partial<Record<keyof T & string, string>>;

export interface FormOptions<T extends Record<string, unknown>> {
	schema: z.ZodType<T>;
	/** Starting values. Pass a function to start from props: `reset()` re-reads it, so it picks up fresh data. */
	initial: T | (() => T);
	onSubmit: (values: T) => Promise<void> | void;
}

/**
 * A small runes-based form: `values` is deep $state, validation uses a zod schema, and field
 * errors returned by the API (422) land on the matching inputs. Pair it with the shadcn
 * Field components:
 *
 *   const form = createForm({ schema, initial: { email: '' }, onSubmit: async (v) => { ... } });
 *   <form onsubmit={form.submit}>
 *     <Input bind:value={form.values.email} aria-invalid={!!form.errors.email} />
 *     <Field.Error errors={form.fieldErrors('email')} />
 */
export function createForm<T extends Record<string, unknown>>(options: FormOptions<T>) {
	const initialValues = (): T =>
		structuredClone($state.snapshot(typeof options.initial === 'function' ? options.initial() : options.initial)) as T;
	const values = $state(initialValues()) as T;
	let errors = $state<FieldErrors<T>>({});
	let formError = $state<string | null>(null);
	let submitting = $state(false);

	function validate(): T | null {
		const result = options.schema.safeParse($state.snapshot(values));
		if (result.success) {
			errors = {};
			return result.data;
		}
		const next: Record<string, string> = {};
		for (const issue of result.error.issues) {
			const key = issue.path[0];
			if (typeof key === 'string' && !(key in next)) next[key] = issue.message;
		}
		errors = next as FieldErrors<T>;
		return null;
	}

	return {
		values,
		get errors() {
			return errors;
		},
		/** A message for the whole form (e.g. "wrong password"), already translated. */
		get formError() {
			return formError;
		},
		get submitting() {
			return submitting;
		},
		/** A field's error in the shape `<Field.Error errors={...}>` expects. */
		fieldErrors(name: keyof T & string) {
			const message = errors[name];
			return message ? [{ message }] : undefined;
		},
		setError(name: keyof T & string, message: string) {
			errors = { ...errors, [name]: message };
		},
		setFormError(message: string | null) {
			formError = message;
		},
		reset(next: T = initialValues()) {
			Object.assign(values, structuredClone($state.snapshot(next)));
			errors = {};
			formError = null;
		},
		submit: async (event?: Event) => {
			event?.preventDefault();
			if (submitting) return;
			formError = null;
			const data = validate();
			if (!data) return;
			submitting = true;
			try {
				await options.onSubmit(data);
			} catch (error) {
				if (error instanceof ApiError && error.hasFieldErrors) {
					const next: Record<string, string> = {};
					for (const [field, problem] of Object.entries(error.fields)) next[field] = fieldErrorMessage(problem);
					errors = next as FieldErrors<T>;
				} else {
					formError = errorMessage(error);
				}
			} finally {
				submitting = false;
			}
		},
	};
}

export type Form<T extends Record<string, unknown>> = ReturnType<typeof createForm<T>>;
