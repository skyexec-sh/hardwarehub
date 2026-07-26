/** Convert yyyy-MM-dd to ISO start-of-day / exclusive end (next day start). */
export function dayStartIso(date: string): string | undefined {
  if (!date?.trim()) {
    return undefined;
  }
  return new Date(`${date}T00:00:00`).toISOString();
}

export function dayEndExclusiveIso(date: string): string | undefined {
  if (!date?.trim()) {
    return undefined;
  }
  const d = new Date(`${date}T00:00:00`);
  d.setDate(d.getDate() + 1);
  return d.toISOString();
}
