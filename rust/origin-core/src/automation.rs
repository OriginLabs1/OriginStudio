//! Scheduled automations.
//!
//! An automation is a prompt plus a schedule. It runs in a real session, with the same
//! approvals as if the user had typed it, and it is visible: every run leaves a session
//! you can read. Recurrence arithmetic lives here so the app, the CLI and a host agree on
//! what "every weekday at 08:30" means.

use serde::{Deserialize, Serialize};

pub const MINUTE: i64 = 60_000;
pub const HOUR: i64 = 60 * MINUTE;
pub const DAY: i64 = 24 * HOUR;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "kebab-case")]
pub enum Schedule {
    /// Seconds are not exposed: a phone does not need them and a host should not either.
    Interval { every_minutes: u32 },
    /// UTC hour and minute. The caller can pre-shift for a local time.
    Daily { hour: u8, minute: u8 },
    /// UTC weekday: 0 is Sunday.
    Weekly { weekday: u8, hour: u8, minute: u8 },
    /// Run only when the user presses Run.
    Manual,
}

fn valid_time(hour: u8, minute: u8) -> bool {
    hour < 24 && minute < 60
}

/// The next run strictly after `from_ms`, or `None` for [Schedule::Manual].
pub fn next_run(schedule: Schedule, from_ms: i64) -> Option<i64> {
    match schedule {
        Schedule::Manual => None,
        Schedule::Interval { every_minutes } => {
            let step = (every_minutes.max(1) as i64) * MINUTE;
            // Align to the step boundary so runs do not drift with every app launch.
            Some((from_ms / step + 1) * step)
        }
        Schedule::Daily { hour, minute } => {
            if !valid_time(hour, minute) {
                return None;
            }
            let today = (from_ms / DAY) * DAY;
            let at = today + (hour as i64) * HOUR + (minute as i64) * MINUTE;
            Some(if at > from_ms { at } else { at + DAY })
        }
        Schedule::Weekly { weekday, hour, minute } => {
            if !valid_time(hour, minute) || weekday > 6 {
                return None;
            }
            let today = (from_ms / DAY) * DAY;
            // 1970-01-01 was a Thursday (4).
            let today_weekday = ((today / DAY + 4).rem_euclid(7)) as u8;
            let mut ahead = (weekday + 7 - today_weekday) % 7;
            let at = today + ahead as i64 * DAY + (hour as i64) * HOUR + (minute as i64) * MINUTE;
            if at <= from_ms {
                ahead += 7;
            }
            Some(today + ahead as i64 * DAY + (hour as i64) * HOUR + (minute as i64) * MINUTE)
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Automation {
    pub id: String,
    pub name: String,
    pub prompt: String,
    pub schedule: Schedule,
    pub enabled: bool,
    pub agent_id: String,
    pub workspace_id: String,
    pub last_run_ms: Option<i64>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct Due {
    pub id: String,
    pub name: String,
    pub run_at_ms: i64,
}

/// Automations that should run. A run is due when its next slot after the last run is at
/// or before now. Disabled ones are skipped. The list is ordered so the oldest slot runs
/// first, which keeps a phone that was asleep from running everything out of order.
pub fn due(items: &[Automation], now_ms: i64) -> Vec<Due> {
    let mut due: Vec<Due> = items
        .iter()
        .filter(|item| item.enabled)
        .filter_map(|item| {
            let from = item.last_run_ms.unwrap_or(now_ms - DAY);
            let next = next_run(item.schedule, from)?;
            (next <= now_ms).then_some(Due {
                id: item.id.clone(),
                name: item.name.clone(),
                run_at_ms: next,
            })
        })
        .collect();
    due.sort_by_key(|run| run.run_at_ms);
    due
}

/// The line shown in the Studio list: `Weekdays at 08:30` is the caller's job; this is
/// the honest UTC description.
pub fn describe(schedule: Schedule) -> String {
    match schedule {
        Schedule::Manual => "Only when you press Run".into(),
        Schedule::Interval { every_minutes } => {
            if every_minutes % 60 == 0 {
                format!("Every {} hours", every_minutes / 60)
            } else {
                format!("Every {every_minutes} minutes")
            }
        }
        Schedule::Daily { hour, minute } => format!("Every day at {hour:02}:{minute:02} UTC"),
        Schedule::Weekly { weekday, hour, minute } => {
            let days = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
            let day = days.get(weekday as usize).copied().unwrap_or("Sunday");
            format!("Every {day} at {hour:02}:{minute:02} UTC")
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn at(year: i64, month: i64, day: i64, hour: i64, minute: i64) -> i64 {
        // Days from civil, so the tests do not need a date crate.
        let y = year - if month <= 2 { 1 } else { 0 };
        let era = if y >= 0 { y } else { y - 399 } / 400;
        let yoe = y - era * 400;
        let mp = (month + 9) % 12;
        let doy = (153 * mp + 2) / 5 + day - 1;
        let doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        let days = era * 146_097 + doe - 719_468;
        days * DAY + hour * HOUR + minute * MINUTE
    }

    #[test]
    fn intervals_align_to_the_step() {
        assert_eq!(next_run(Schedule::Interval { every_minutes: 15 }, 0), Some(15 * MINUTE));
        assert_eq!(next_run(Schedule::Interval { every_minutes: 15 }, 15 * MINUTE), Some(30 * MINUTE));
        assert_eq!(next_run(Schedule::Interval { every_minutes: 0 }, 0), Some(MINUTE));
    }

    #[test]
    fn daily_rolls_to_tomorrow() {
        let now = at(2026, 11, 12, 9, 0);
        assert_eq!(
            next_run(Schedule::Daily { hour: 8, minute: 30 }, now),
            Some(at(2026, 11, 13, 8, 30))
        );
        assert_eq!(
            next_run(Schedule::Daily { hour: 10, minute: 0 }, now),
            Some(at(2026, 11, 12, 10, 0))
        );
    }

    #[test]
    fn weekly_finds_the_right_day() {
        // 2026-11-12 is a Thursday.
        let now = at(2026, 11, 12, 9, 0);
        assert_eq!(
            next_run(Schedule::Weekly { weekday: 6, hour: 8, minute: 0 }, now),
            Some(at(2026, 11, 14, 8, 0))
        );
        // Thursday 08:00 has passed, so next Thursday, not today.
        assert_eq!(
            next_run(Schedule::Weekly { weekday: 4, hour: 8, minute: 0 }, now),
            Some(at(2026, 11, 19, 8, 0))
        );
    }

    #[test]
    fn invalid_times_are_refused() {
        assert_eq!(next_run(Schedule::Daily { hour: 24, minute: 0 }, 0), None);
        assert_eq!(next_run(Schedule::Weekly { weekday: 7, hour: 8, minute: 0 }, 0), None);
        assert_eq!(next_run(Schedule::Manual, 0), None);
    }

    #[test]
    fn due_skips_disabled_and_orders_oldest_first() {
        let now = at(2026, 11, 12, 12, 0);
        let items = vec![
            Automation {
                id: "a".into(),
                name: "Late".into(),
                prompt: "".into(),
                schedule: Schedule::Interval { every_minutes: 30 },
                enabled: true,
                agent_id: "origin".into(),
                workspace_id: "w".into(),
                last_run_ms: Some(now - 31 * MINUTE),
            },
            Automation {
                id: "b".into(),
                name: "Off".into(),
                prompt: "".into(),
                schedule: Schedule::Interval { every_minutes: 1 },
                enabled: false,
                agent_id: "origin".into(),
                workspace_id: "w".into(),
                last_run_ms: None,
            },
        ];
        let runs = due(&items, now);
        assert_eq!(runs.len(), 1);
        assert_eq!(runs[0].id, "a");
    }

    #[test]
    fn descriptions_are_plain() {
        assert_eq!(
            describe(Schedule::Daily { hour: 8, minute: 5 }),
            "Every day at 08:05 UTC"
        );
        assert_eq!(
            describe(Schedule::Interval { every_minutes: 120 }),
            "Every 2 hours"
        );
    }
}
