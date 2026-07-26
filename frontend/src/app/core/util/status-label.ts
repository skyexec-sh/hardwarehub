/** Human-readable labels for fulfillment enum statuses (e.g. PARTIALLY_DELIVERED → Partially delivered). */
export function statusLabel(status: string | null | undefined): string {
  if (!status) {
    return '—';
  }
  const words = status.toLowerCase().split('_').filter(Boolean);
  if (words.length === 0) {
    return status;
  }
  return words
    .map((word, index) => (index === 0 ? word.charAt(0).toUpperCase() + word.slice(1) : word))
    .join(' ');
}

/** CSS modifier class for status chips (e.g. PARTIALLY_DELIVERED → partially_delivered). */
export function statusClass(status: string | null | undefined): string {
  return (status || '').toLowerCase();
}
