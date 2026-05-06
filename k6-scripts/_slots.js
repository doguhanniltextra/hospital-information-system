/** Tomorrow 10:00–11:00 UTC as ISO-8601 (used for availability query params). */
export function nextDaySlotIso() {
  const d = new Date();
  d.setUTCDate(d.getUTCDate() + 1);
  d.setUTCHours(10, 0, 0, 0);
  const start = d.toISOString();
  d.setUTCHours(11, 0, 0, 0);
  const end = d.toISOString();
  return { start, end };
}
