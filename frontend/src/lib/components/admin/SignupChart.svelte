<script lang="ts">
	import { formatDate, formatNumber } from '$lib/i18n/format';

	/** Daily counts, oldest first, one entry per day (the API fills empty days with 0). Dates are UTC days. */
	let { data, label }: { data: { date: string; count: number }[]; label: string } = $props();

	const max = $derived(Math.max(1, ...data.map((point) => point.count)));
	const total = $derived(data.reduce((sum, point) => sum + point.count, 0));
	const day = (date: string, options: Intl.DateTimeFormatOptions) =>
		formatDate(`${date}T00:00:00Z`, { ...options, timeZone: 'UTC' });
</script>

<figure aria-label={label}>
	<p class="mb-4 text-2xl font-semibold tracking-tight tabular-nums">{formatNumber(total)}</p>
	{#if data.length > 0}
		<!-- Time runs left to right in every language, as in most charts. -->
		<div dir="ltr" class="flex h-40 items-end gap-[3px]">
			{#each data as point (point.date)}
				<div
					class="min-h-[2px] flex-1 rounded-t-sm bg-primary/80 transition-colors hover:bg-primary"
					style:height="{(point.count / max) * 100}%"
					title="{day(point.date, { dateStyle: 'medium' })}: {formatNumber(point.count)}"
				></div>
			{/each}
		</div>
		<figcaption class="mt-2 flex justify-between text-xs text-muted-foreground" dir="ltr">
			<span>{day(data[0].date, { day: 'numeric', month: 'short' })}</span>
			<span>{day(data[data.length - 1].date, { day: 'numeric', month: 'short' })}</span>
		</figcaption>
	{/if}
</figure>
