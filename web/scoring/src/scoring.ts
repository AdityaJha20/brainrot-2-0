// Brain Rot 2.0 scoring engine, TypeScript port (Phase 0 proposal "scoring-v0-proposal").
// The rules live in shared-fixtures/scoring/README.md; core/ holds the Kotlin twin.
// Both must pass the same JSON fixtures. Keep the two files structurally parallel.

export const SCORING_VERSION = "scoring-v0-proposal";

export type Measurement = "BINARY" | "QUANTITY" | "DURATION";
export type EventType = "QUANTITY" | "DURATION" | "COMPLETION";
export type DayMarkType = "REST" | "SKIPPED";
export type Status =
  | "NOT_SCHEDULED"
  | "REST"
  | "SKIPPED"
  | "COMPLETED"
  | "PARTIAL"
  | "MISSED"
  | "UNKNOWN";

export interface Schedule {
  type: "DAILY" | "DAYS_OF_WEEK";
  days?: number[]; // ISO weekday, 1 = Monday ... 7 = Sunday
}

export interface Habit {
  id: string;
  name?: string | null;
  measurement: Measurement;
  target: number;
  unit?: string | null;
  importance: number;
  schedule: Schedule;
  startDate: string; // yyyy-mm-dd
  endDate?: string | null;
}

export interface LogEvent {
  id: string;
  habitId: string;
  type: EventType;
  value: number;
  eventTime: string; // ISO-8601 instant, UTC
  zone: string; // IANA zone the user was in
  createdAt: string;
  deleted: boolean;
}

export interface DayMark {
  habitId: string;
  date: string;
  mark: DayMarkType;
}

export interface Policy {
  successThreshold: number;
  dayStartHour: number;
  heatmapBins: [number, number];
  perfectDay: "ALL_ELIGIBLE_COMPLETED";
}

export const DEFAULT_POLICY: Policy = {
  successThreshold: 1.0,
  dayStartHour: 0,
  heatmapBins: [34, 67],
  perfectDay: "ALL_ELIGIBLE_COMPLETED",
};

export interface Input {
  habits: Habit[];
  events: LogEvent[];
  dayMarks: DayMark[];
  reviewedDates: string[];
  policy?: Partial<Policy>;
}

export interface HabitDay {
  status: Status;
  recorded: number;
  progress: number;
  streak: number;
  streakPendingReview: boolean;
}

export interface DaySummary {
  date: string;
  habits: Record<string, HabitDay>;
  dailyCompletion: { completed: number; eligible: number; ratio: number | null };
  dailyScore: number | null;
  scoreIsProvisional: boolean;
  heatmapLevel: number;
  scoringVersion: string;
}

const MAX_STREAK_LOOKBACK_DAYS = 3660;

// ---- Calendar helpers (dates are plain yyyy-mm-dd strings; arithmetic via UTC midnight) ----

function toEpochDay(date: string): number {
  const [y, m, d] = date.split("-").map(Number);
  return Math.floor(Date.UTC(y, m - 1, d) / 86_400_000);
}

function fromEpochDay(day: number): string {
  return new Date(day * 86_400_000).toISOString().slice(0, 10);
}

function isoWeekday(date: string): number {
  const js = new Date(toEpochDay(date) * 86_400_000).getUTCDay(); // 0 = Sunday
  return js === 0 ? 7 : js;
}

const formatterCache = new Map<string, Intl.DateTimeFormat>();

function localDateTimeParts(instant: string, zone: string): { date: string; hour: number } {
  let fmt = formatterCache.get(zone);
  if (!fmt) {
    fmt = new Intl.DateTimeFormat("en-CA", {
      timeZone: zone,
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      hourCycle: "h23",
    });
    formatterCache.set(zone, fmt);
  }
  const parts = Object.fromEntries(fmt.formatToParts(new Date(instant)).map((p) => [p.type, p.value]));
  return { date: `${parts.year}-${parts.month}-${parts.day}`, hour: Number(parts.hour) };
}

/** The logical day an event belongs to: local time in the event's zone, shifted back by dayStartHour. */
export function logicalDate(event: LogEvent, policy: Policy): string {
  const { date, hour } = localDateTimeParts(event.eventTime, event.zone);
  return hour < policy.dayStartHour ? fromEpochDay(toEpochDay(date) - 1) : date;
}

// ---- Engine ----

export class ScoringEngine {
  readonly policy: Policy;
  private readonly habits: Habit[];
  private readonly eventsByHabitDay = new Map<string, LogEvent[]>();
  private readonly activeDays = new Set<string>();
  private readonly marks = new Map<string, DayMarkType>();
  private readonly reviewed: Set<string>;

  constructor(input: Input) {
    this.policy = { ...DEFAULT_POLICY, ...(input.policy ?? {}) };
    this.habits = input.habits;
    this.reviewed = new Set(input.reviewedDates);
    for (const m of input.dayMarks) this.marks.set(`${m.habitId}|${m.date}`, m.mark);
    for (const e of input.events) {
      if (e.deleted) continue;
      const day = logicalDate(e, this.policy);
      const key = `${e.habitId}|${day}`;
      const list = this.eventsByHabitDay.get(key) ?? [];
      list.push(e);
      this.eventsByHabitDay.set(key, list);
      this.activeDays.add(day);
    }
  }

  isScheduled(habit: Habit, date: string): boolean {
    if (date < habit.startDate) return false;
    if (habit.endDate && date > habit.endDate) return false;
    if (habit.schedule.type === "DAILY") return true;
    return (habit.schedule.days ?? []).includes(isoWeekday(date));
  }

  private measure(habit: Habit, date: string): { recorded: number; progress: number; hasEvents: boolean } {
    const events = this.eventsByHabitDay.get(`${habit.id}|${date}`) ?? [];
    let sum = 0;
    let completed = false;
    for (const e of events) {
      if (e.type === "COMPLETION") completed = true;
      else sum += e.value;
    }
    const recorded = Math.max(sum, 0);
    let progress: number;
    if (completed) progress = 1;
    else if (habit.measurement === "BINARY") progress = 0;
    else progress = habit.target > 0 ? Math.min(Math.max(recorded / habit.target, 0), 1) : 0;
    return { recorded, progress, hasEvents: events.length > 0 };
  }

  private status(habit: Habit, date: string, progress: number, hasEvents: boolean): Status {
    if (!this.isScheduled(habit, date)) return "NOT_SCHEDULED";
    const mark = this.marks.get(`${habit.id}|${date}`);
    if (mark) return mark;
    if (progress >= this.policy.successThreshold) return "COMPLETED";
    if (this.reviewed.has(date)) return "MISSED";
    return hasEvents ? "PARTIAL" : "UNKNOWN";
  }

  statusOn(habit: Habit, date: string): Status {
    const m = this.measure(habit, date);
    return this.status(habit, date, m.progress, m.hasEvents);
  }

  streak(habit: Habit, date: string): { streak: number; pending: boolean } {
    let count = 0;
    const start = toEpochDay(date);
    const floor = Math.max(toEpochDay(habit.startDate), start - MAX_STREAK_LOOKBACK_DAYS);
    for (let day = start; day >= floor; day--) {
      const d = fromEpochDay(day);
      const s = this.statusOn(habit, d);
      if (s === "COMPLETED") count++;
      else if (s === "NOT_SCHEDULED" || s === "REST" || s === "SKIPPED") continue;
      else if (day === start && !this.reviewed.has(d)) continue; // today still in progress
      else if (s === "MISSED") return { streak: count, pending: false };
      else return { streak: count, pending: true }; // UNKNOWN or PARTIAL on an unreviewed earlier day
    }
    return { streak: count, pending: false };
  }

  evaluate(date: string): DaySummary {
    const habits: Record<string, HabitDay> = {};
    let completed = 0;
    let eligible = 0;
    let weightSum = 0;
    let weighted = 0;
    for (const habit of this.habits) {
      const m = this.measure(habit, date);
      const status = this.status(habit, date, m.progress, m.hasEvents);
      const { streak, pending } = this.streak(habit, date);
      habits[habit.id] = { status, recorded: m.recorded, progress: m.progress, streak, streakPendingReview: pending };
      if (status === "NOT_SCHEDULED" || status === "REST" || status === "SKIPPED") continue;
      eligible++;
      if (status === "COMPLETED") completed++;
      weightSum += habit.importance;
      weighted += habit.importance * m.progress;
    }
    const dailyScore = eligible === 0 || weightSum <= 0 ? null : (100 * weighted) / weightSum;
    return {
      date,
      habits,
      dailyCompletion: { completed, eligible, ratio: eligible === 0 ? null : completed / eligible },
      dailyScore,
      scoreIsProvisional: !this.reviewed.has(date),
      heatmapLevel: this.heatmapLevel(date, eligible, completed, dailyScore),
      scoringVersion: SCORING_VERSION,
    };
  }

  private heatmapLevel(date: string, eligible: number, completed: number, score: number | null): number {
    if (eligible === 0 || score === null || !this.activeDays.has(date)) return 0;
    if (completed === eligible) return 4;
    const [low, high] = this.policy.heatmapBins;
    if (score < low) return 1;
    if (score < high) return 2;
    return 3;
  }
}
