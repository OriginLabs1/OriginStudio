//! Usage, limits and accounts.
//!
//! Agent subscriptions have limits, and limits have reset times. The app shows both
//! without asking anyone to sign out: switching the account a session uses is a switch,
//! not a new login. Nothing here talks to a provider; callers fill in the numbers they
//! already have.

use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Limit {
    /// `5h`, `weekly`, `credits` — a short human label.
    pub label: String,
    pub used: f64,
    pub limit: f64,
    /// `requests`, `tokens`, `%`.
    pub unit: String,
    /// Epoch milliseconds. `None` when the window does not reset.
    pub resets_at_ms: Option<i64>,
}

impl Limit {
    pub fn new(label: impl Into<String>, used: f64, limit: f64, unit: impl Into<String>) -> Self {
        Self {
            label: label.into(),
            used,
            limit,
            unit: unit.into(),
            resets_at_ms: None,
        }
    }

    pub fn resetting_at(mut self, epoch_ms: i64) -> Self {
        self.resets_at_ms = Some(epoch_ms);
        self
    }

    pub fn remaining(&self) -> f64 {
        (self.limit - self.used).max(0.0)
    }

    pub fn fraction(&self) -> f32 {
        if self.limit <= 0.0 {
            return 0.0;
        }
        ((self.used / self.limit) as f32).clamp(0.0, 1.0)
    }

    pub fn percent_left(&self) -> u32 {
        ((1.0 - self.fraction()) * 100.0).round() as u32
    }

    pub fn is_exhausted(&self) -> bool {
        self.used >= self.limit
    }

    /// A short line for a card: `72% left · resets in 3h 12m`.
    pub fn summary(&self, now_ms: i64) -> String {
        let left = format!(
            "{}% left",
            self.percent_left()
        );
        match self.resets_at_ms.and_then(|at| reset_in(at, now_ms)) {
            Some(text) => format!("{left} · resets in {text}"),
            None => left,
        }
    }
}

/// `3h 12m`, `4d`, `now`. Negative or tiny gaps read as `now`.
pub fn reset_in(reset_at_ms: i64, now_ms: i64) -> Option<String> {
    let delta = reset_at_ms - now_ms;
    if delta <= 0 {
        return Some("now".into());
    }
    let minutes = delta / 60_000;
    let hours = minutes / 60;
    let days = hours / 24;
    Some(if days > 0 {
        format!("{days}d")
    } else if hours > 0 {
        format!("{hours}h {}m", minutes % 60)
    } else if minutes > 0 {
        format!("{minutes}m")
    } else {
        "under a minute".into()
    })
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Account {
    pub id: String,
    /// `Claude`, `Codex`, `OpenCode` ...
    pub provider: String,
    /// The label the user gave it, e.g. `Work`, `Personal`.
    pub label: String,
    pub plan: String,
    pub limits: Vec<Limit>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UsageSnapshot {
    pub accounts: Vec<Account>,
    /// The account sessions use right now.
    pub active: Option<String>,
}

impl UsageSnapshot {
    pub fn new(accounts: Vec<Account>, active: impl Into<String>) -> Self {
        let active = active.into();
        let active = (!active.is_empty()).then_some(active);
        Self { accounts, active }
    }

    pub fn active_account(&self) -> Option<&Account> {
        let id = self.active.as_deref()?;
        self.accounts.iter().find(|account| account.id == id)
    }

    /// Switching never signs anything out and never copies a key: it only chooses which
    /// profile the next session is handed. Returns false when the id is unknown.
    pub fn switch_to(&mut self, id: &str) -> bool {
        if self.accounts.iter().any(|account| account.id == id) {
            self.active = Some(id.to_string());
            true
        } else {
            false
        }
    }

    /// The tightest limit across every account, for the status line.
    pub fn most_constrained(&self) -> Option<(&Account, &Limit)> {
        self.accounts
            .iter()
            .flat_map(|account| account.limits.iter().map(move |limit| (account, limit)))
            .max_by(|a, b| a.1.fraction().total_cmp(&b.1.fraction()))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn remaining_never_goes_negative() {
        let limit = Limit::new("5h", 120.0, 100.0, "requests");
        assert_eq!(limit.remaining(), 0.0);
        assert!(limit.is_exhausted());
        assert_eq!(limit.percent_left(), 0);
    }

    #[test]
    fn summaries_say_when_the_window_resets() {
        let now = 1_000_000_000_000;
        let limit = Limit::new("5h", 25.0, 100.0, "requests").resetting_at(now + 3 * 3_600_000 + 12 * 60_000);
        assert_eq!(limit.summary(now), "75% left · resets in 3h 12m");
    }

    #[test]
    fn reset_in_reads_naturally() {
        assert_eq!(reset_in(5_000, 10_000).unwrap(), "now");
        assert_eq!(reset_in(10_000 + 4 * 86_400_000, 10_000).unwrap(), "4d");
        assert_eq!(reset_in(10_000 + 90_000, 10_000).unwrap(), "1m");
    }

    #[test]
    fn switching_needs_no_logout_and_rejects_unknown_ids() {
        let mut snapshot = UsageSnapshot::new(
            vec![
                Account {
                    id: "work".into(),
                    provider: "Claude".into(),
                    label: "Work".into(),
                    plan: "Max".into(),
                    limits: vec![Limit::new("5h", 30.0, 100.0, "requests")],
                },
                Account {
                    id: "personal".into(),
                    provider: "Codex".into(),
                    label: "Personal".into(),
                    plan: "Plus".into(),
                    limits: vec![Limit::new("weekly", 90.0, 100.0, "requests")],
                },
            ],
            "work",
        );
        assert!(snapshot.switch_to("personal"));
        assert_eq!(snapshot.active.as_deref(), Some("personal"));
        assert!(!snapshot.switch_to("missing"));
        assert_eq!(snapshot.active.as_deref(), Some("personal"));
    }

    #[test]
    fn most_constrained_account_is_found() {
        let snapshot = UsageSnapshot::new(
            vec![
                Account {
                    id: "a".into(),
                    provider: "Claude".into(),
                    label: "A".into(),
                    plan: "Pro".into(),
                    limits: vec![Limit::new("5h", 40.0, 100.0, "requests")],
                },
                Account {
                    id: "b".into(),
                    provider: "Codex".into(),
                    label: "B".into(),
                    plan: "Plus".into(),
                    limits: vec![Limit::new("weekly", 95.0, 100.0, "requests")],
                },
            ],
            "a",
        );
        let (account, limit) = snapshot.most_constrained().unwrap();
        assert_eq!(account.id, "b");
        assert_eq!(limit.label, "weekly");
    }
}
