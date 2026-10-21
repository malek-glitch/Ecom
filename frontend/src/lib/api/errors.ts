import { m } from '$lib/paraglide/messages';

export interface FieldProblem {
	code: string;
	message: string;
}

/**
 * An error response from the API (RFC 9457 problem+json). `code` is stable and
 * machine-readable (`auth.invalid_credentials`); show [errorMessage] to users, not `detail`.
 */
export class ApiError extends Error {
	constructor(
		readonly status: number,
		readonly code: string,
		readonly detail: string,
		readonly fields: Record<string, FieldProblem> = {},
	) {
		super(detail);
		this.name = 'ApiError';
	}

	static fromBody(status: number, body: unknown): ApiError {
		const problem = (body ?? {}) as {
			code?: string;
			detail?: string;
			title?: string;
			fields?: Record<string, FieldProblem>;
		};
		return new ApiError(
			status,
			problem.code ?? `http.${status}`,
			problem.detail ?? problem.title ?? 'Request failed',
			problem.fields ?? {},
		);
	}

	static network(): ApiError {
		return new ApiError(0, 'network', 'Network error');
	}

	get hasFieldErrors() {
		return Object.keys(this.fields).length > 0;
	}
}

const MESSAGES: Record<string, () => string> = {
	network: m.error_network,
	'auth.invalid_credentials': m.error_invalid_credentials,
	'auth.email_not_verified': m.error_email_not_verified,
	'auth.email_taken': m.error_email_taken,
	'auth.account_disabled': m.error_account_disabled,
	'auth.registration_disabled': m.error_registration_disabled,
	'auth.token_invalid': m.error_link_invalid,
	'auth.refresh_invalid': m.error_session_expired,
	'auth.refresh_reused': m.error_session_revoked,
	'auth.google_invalid': m.error_google,
	'auth.google_disabled': m.error_google,
	'auth.google_email_unverified': m.error_google_unverified,
	'auth.forbidden': m.error_forbidden,
	'auth.unauthorized': m.error_session_expired,
	'auth.invalid_token': m.error_session_expired,
	'request.not_found': m.error_not_found,
	'note.not_found': m.error_not_found, // [example:notes]
	'user.not_found': m.error_not_found,
	'session.not_found': m.error_not_found,
	'request.rate_limited': m.error_rate_limited,
	'request.validation': m.error_validation,
	'conflict.stale': m.error_stale,
	'admin.last_admin': m.error_last_admin,
	'admin.cannot_change_own_role': m.error_own_role,
	'admin.cannot_disable_self': m.error_disable_self,
	'admin.cannot_delete_self': m.error_delete_self,
	'admin.user_disabled': m.error_user_disabled,
	'storage.invalid_image': m.error_invalid_image,
	'storage.empty_file': m.error_invalid_image,
	'storage.image_too_large': m.error_file_too_large,
	'storage.too_large': m.error_file_too_large,
	'request.too_large': m.error_file_too_large,
	'storage.disabled': m.error_storage_disabled,
};

/** A translated, user-facing message for any error thrown by an API call. */
export function errorMessage(error: unknown): string {
	if (!(error instanceof ApiError)) return m.error_generic();
	const translate = MESSAGES[error.code];
	if (translate) return translate();
	return error.status >= 500 ? m.error_server() : m.error_generic();
}

const FIELD_MESSAGES: Record<string, () => string> = {
	NotBlank: m.validation_required,
	NotNull: m.validation_required,
	Email: m.validation_email,
	ValidPassword: m.validation_password,
	Size: m.validation_too_long,
	Incorrect: m.validation_incorrect_password,
	Mismatch: m.validation_mismatch,
};

/** Translates a field error returned by the API (Bean Validation constraint names). */
export function fieldErrorMessage(problem: FieldProblem): string {
	return (FIELD_MESSAGES[problem.code] ?? m.validation_invalid)();
}
