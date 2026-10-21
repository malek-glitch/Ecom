import { z } from 'zod';
import { m } from '$lib/paraglide/messages';

// Messages are functions so they're translated when validation runs, in the current language.
// Rules mirror the backend's (PasswordPolicy.kt), which stays the source of truth.

export const email = () =>
	z
		.string()
		.trim()
		.min(1, { error: () => m.validation_required() })
		.pipe(z.email({ error: () => m.validation_email() }));

export const required = (max = 255) =>
	z
		.string()
		.trim()
		.min(1, { error: () => m.validation_required() })
		.max(max, { error: () => m.validation_too_long() });

export const password = () =>
	z
		.string()
		.min(8, { error: () => m.validation_password() })
		.max(64, { error: () => m.validation_password() })
		.refine((value) => new TextEncoder().encode(value).length <= 72, { error: () => m.validation_password() });

export const anyPassword = () => z.string().min(1, { error: () => m.validation_required() });
