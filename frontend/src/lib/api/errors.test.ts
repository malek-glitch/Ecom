import { describe, expect, it } from 'vitest';
import { ApiError, errorMessage, fieldErrorMessage } from './errors';

describe('ApiError', () => {
	it('reads an RFC 9457 problem', () => {
		const error = ApiError.fromBody(422, {
			title: 'Unprocessable Entity',
			detail: 'Some fields are invalid',
			code: 'request.validation',
			fields: { email: { code: 'Email', message: 'must be a well-formed email address' } },
		});
		expect(error.status).toBe(422);
		expect(error.code).toBe('request.validation');
		expect(error.hasFieldErrors).toBe(true);
	});

	it('copes with a body that is not a problem', () => {
		const error = ApiError.fromBody(502, '<html>Bad gateway</html>');
		expect(error.code).toBe('http.502');
		expect(error.hasFieldErrors).toBe(false);
	});

	it('translates known codes and falls back for unknown ones', () => {
		const known = errorMessage(new ApiError(401, 'auth.invalid_credentials', ''));
		const unknown = errorMessage(new ApiError(400, 'something.new', ''));
		const server = errorMessage(new ApiError(503, 'something.new', ''));
		expect(known).not.toBe(unknown);
		expect(server).not.toBe(unknown);
		expect(errorMessage(new Error('boom'))).toBe(unknown);
	});

	it('translates field constraint names', () => {
		expect(fieldErrorMessage({ code: 'NotBlank', message: '' })).not.toBe(
			fieldErrorMessage({ code: 'Whatever', message: '' }),
		);
	});
});
