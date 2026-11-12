//! Jump Anywhere.
//!
//! One keystroke (`Ctrl/Cmd+K` on a keyboard, the Jump pill on a phone) searches every
//! place the app can go: workspaces, sessions, repos, files, hosts, agents and app
//! actions. This module is the ranking, deliberately independent of the UI so the phone,
//! the CLI and a host all put the same result first.

use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "kebab-case")]
pub enum JumpKind {
    Workspace,
    Session,
    Repo,
    File,
    Host,
    Agent,
    Action,
    Environment,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct JumpEntry {
    pub id: String,
    pub title: String,
    pub subtitle: String,
    pub kind: JumpKind,
    /// The deep link or command the app will run. Opaque to the core.
    pub target: String,
    /// Extra words that should match but are not shown.
    pub keywords: Vec<String>,
}

impl JumpEntry {
    pub fn new(
        id: impl Into<String>,
        title: impl Into<String>,
        subtitle: impl Into<String>,
        kind: JumpKind,
        target: impl Into<String>,
    ) -> Self {
        Self {
            id: id.into(),
            title: title.into(),
            subtitle: subtitle.into(),
            kind,
            target: target.into(),
            keywords: Vec::new(),
        }
    }

    pub fn with_keywords(mut self, keywords: &[&str]) -> Self {
        self.keywords = keywords.iter().map(|k| k.to_string()).collect();
        self
    }
}

#[derive(Debug, Clone, Default)]
pub struct JumpIndex {
    entries: Vec<JumpEntry>,
}

/// The score for a match. 0 means "do not show".
fn score(entry: &JumpEntry, query: &str) -> u32 {
    if query.is_empty() {
        return 1;
    }
    let title = entry.title.to_lowercase();
    let subtitle = entry.subtitle.to_lowercase();
    let query = query.to_lowercase();

    if title == query {
        return 1000;
    }
    if title.starts_with(&query) {
        return 900;
    }
    if title.split_whitespace().any(|word| word.starts_with(&query)) {
        return 800;
    }
    if title.contains(&query) {
        return 600;
    }
    if entry.keywords.iter().any(|k| k.to_lowercase() == query) {
        return 550;
    }
    if subtitle.contains(&query) {
        return 400;
    }
    if entry.keywords.iter().any(|k| k.to_lowercase().contains(&query)) {
        return 300;
    }
    // A subsequence match keeps "opn" or "wk" useful for people who type fast.
    let mut chars = title.chars();
    if query.chars().all(|needle| chars.any(|c| c == needle)) {
        return 150;
    }
    0
}

impl JumpIndex {
    pub fn new(entries: Vec<JumpEntry>) -> Self {
        Self { entries }
    }

    pub fn entries(&self) -> &[JumpEntry] {
        &self.entries
    }

    pub fn add(&mut self, entry: JumpEntry) {
        self.entries.push(entry);
    }

    /// Results, best first, capped. An empty query shows the recent/important order the
    /// caller supplied.
    pub fn search(&self, query: &str, limit: usize) -> Vec<&JumpEntry> {
        let query = query.trim();
        let mut scored: Vec<(u32, usize, &JumpEntry)> = self
            .entries
            .iter()
            .enumerate()
            .filter_map(|(index, entry)| {
                let score = score(entry, query);
                (score > 0).then_some((score, index, entry))
            })
            .collect();
        // Stable: equal scores keep the caller's order, which is the recency order.
        scored.sort_by(|a, b| b.0.cmp(&a.0).then(a.1.cmp(&b.1)));
        scored.into_iter().take(limit).map(|(_, _, entry)| entry).collect()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn index() -> JumpIndex {
        JumpIndex::new(vec![
            JumpEntry::new("f1", "Origin Studio", "workspace", JumpKind::Workspace, "folder:1"),
            JumpEntry::new("f2", "origin-core", "repo", JumpKind::Repo, "folder:2")
                .with_keywords(&["rust", "core"]),
            JumpEntry::new("s1", "fix the release job", "Origin Studio", JumpKind::Session, "session:1"),
            JumpEntry::new("a1", "Dakota OS", "environment", JumpKind::Environment, "env:dakota"),
        ])
    }

    #[test]
    fn exact_title_wins() {
        let index = index();
        let results = index.search("origin-core", 10);
        assert_eq!(results[0].id, "f2");
    }

    #[test]
    fn prefix_beats_substring() {
        let index = index();
        let results = index.search("origin", 10);
        assert_eq!(results[0].id, "f1");
        assert_eq!(results[1].id, "f2");
    }

    #[test]
    fn keywords_and_subtitles_match() {
        let index = index();
        let results = index.search("rust", 10);
        assert_eq!(results.first().unwrap().id, "f2");
        let results = index.search("release", 10);
        assert_eq!(results.first().unwrap().id, "s1");
    }

    #[test]
    fn typo_friendly_subsequence_matches() {
        let index = index();
        let results = index.search("ostd", 10);
        assert_eq!(results.first().unwrap().id, "f1");
    }

    #[test]
    fn empty_query_keeps_caller_order_and_limit_applies() {
        let index = index();
        let results = index.search("", 2);
        assert_eq!(results.len(), 2);
        assert_eq!(results[0].id, "f1");
        assert_eq!(results[1].id, "f2");
    }
}
