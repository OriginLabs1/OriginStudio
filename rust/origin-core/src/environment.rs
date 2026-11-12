//! Environment choices.
//!
//! Origin Studio never pretends that every place it can run is the same machine. The
//! catalog is explicit about where an environment runs, what it costs and which one is
//! recommended for the device in hand.

use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "kebab-case")]
pub enum EnvironmentId {
    Debian,
    Dakota,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "kebab-case")]
pub enum Runtime {
    /// Runs inside the app on the device, offline, through PRoot.
    Local,
    /// Runs on a paired host, desktop or cloud machine you control.
    Host,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "kebab-case")]
pub enum Recommendation {
    Recommended,
    MostRecommended,
    Available,
}

impl Recommendation {
    pub fn caption(self) -> &'static str {
        match self {
            Recommendation::Recommended => "Recommended",
            Recommendation::MostRecommended => "Most Recommended",
            Recommendation::Available => "Available",
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EnvironmentOption {
    pub id: EnvironmentId,
    pub name: String,
    pub version: String,
    pub runtime: Runtime,
    pub recommendation: Recommendation,
    pub caption: String,
    pub summary: String,
    pub storage: String,
    pub update_channel: String,
    pub docs: String,
    pub best_for: Vec<String>,
    pub honest_limits: Vec<String>,
}

/// The two first-class choices. The order is intentional: the local, offline option
/// comes first for a phone, while the host option is the most recommended when there is
/// a real machine to run it on.
pub fn catalog() -> Vec<EnvironmentOption> {
    vec![
        EnvironmentOption {
            id: EnvironmentId::Debian,
            name: "Debian 13".into(),
            version: "Trixie / latest point release".into(),
            runtime: Runtime::Local,
            recommendation: Recommendation::Recommended,
            caption: "Recommended on phones".into(),
            summary: "A full Debian userland inside Origin Studio. Works offline, needs no root and no account.".into(),
            storage: "about 400 MB once; tools you add use more".into(),
            update_channel: "Debian stable security updates, fetched with apt inside the guest".into(),
            docs: "docs/universal.md".into(),
            best_for: vec![
                "Working on a phone or tablet without a laptop".into(),
                "Offline code, edits, builds and agent sessions".into(),
                "Reproducible, inspectable local workspaces".into(),
            ],
            honest_limits: vec![
                "PRoot is not a virtual machine: no kernel modules, systemd or raw sockets".into(),
                "Android kills background processes aggressively; keep long runs in the foreground".into(),
            ],
        },
        EnvironmentOption {
            id: EnvironmentId::Dakota,
            name: "Dakota OS".into(),
            version: "Bluefin Dakota, latest stream".into(),
            runtime: Runtime::Host,
            recommendation: Recommendation::MostRecommended,
            caption: "Most Recommended on laptops and hosts".into(),
            summary: "Bluefin's GNOME OS-based, atomic Dakota image, paired as a host you control. Same workspace, same approvals, honest guarantees.".into(),
            storage: "lives on the host; the phone stays a thin client".into(),
            update_channel: "bootc image stream from ghcr.io/projectbluefin/dakota (stable, testing or next)".into(),
            docs: "docs/strategy/DUAL-MODE.md".into(),
            best_for: vec![
                "Long builds, emulators and desktop-class tooling".into(),
                "Atomic updates and rollback on a machine you own".into(),
                "Using the same workspace from a phone and a laptop".into(),
            ],
            honest_limits: vec![
                "Not unpacked into PRoot on the phone; it runs on a paired host".into(),
                "Dakota is alpha software from the Bluefin community: expect rough edges".into(),
                "A host needs a network path and its own update discipline".into(),
            ],
        },
    ]
}

/// The recommendation for a surface. Phones get Debian as the offline default; a
/// desktop or a paired host gets Dakota as the most recommended.
pub fn recommended_for(is_handheld: bool) -> EnvironmentId {
    if is_handheld {
        EnvironmentId::Debian
    } else {
        EnvironmentId::Dakota
    }
}

pub fn find(id: EnvironmentId) -> EnvironmentOption {
    catalog()
        .into_iter()
        .find(|option| option.id == id)
        .expect("the catalog always contains both first-class environments")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn both_environments_have_a_caption() {
        for option in catalog() {
            assert!(!option.caption.is_empty());
            assert!(!option.recommendation.caption().is_empty());
        }
    }

    #[test]
    fn phone_recommends_local_debian() {
        assert_eq!(recommended_for(true), EnvironmentId::Debian);
        assert_eq!(recommended_for(false), EnvironmentId::Dakota);
    }

    #[test]
    fn dakota_is_most_recommended_and_hosted() {
        let dakota = find(EnvironmentId::Dakota);
        assert_eq!(dakota.recommendation, Recommendation::MostRecommended);
        assert_eq!(dakota.runtime, Runtime::Host);
        assert!(dakota.summary.contains("atomic"));
    }
}
