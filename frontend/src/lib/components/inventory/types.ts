import type { components } from '$lib/api/schema';
import { m } from '$lib/paraglide/messages';

export type InventoryRow = components['schemas']['InventoryRow'];
export type Movement = components['schemas']['MovementResponse'];
export type MovementType = Movement['type'];
export type AdjustmentKind = NonNullable<components['schemas']['AdjustStockRequest']['kind']>;

export const MOVEMENT_LABELS: Record<MovementType, () => string> = {
	INITIAL: m.movement_initial,
	RECEIVED: m.movement_received,
	SOLD: m.movement_sold,
	RETURNED: m.movement_returned,
	REMOVED: m.movement_removed,
	COUNTED: m.movement_counted,
};

/** The stock after an adjustment, for the dialog's preview (null when it would go below zero). */
export function stockAfter(current: number, kind: AdjustmentKind, quantity: number): number | null {
	const after = kind === 'RECEIVE' ? current + quantity : kind === 'REMOVE' ? current - quantity : quantity;
	return after < 0 ? null : after;
}
