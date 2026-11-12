//! `origin-core` is the portable, dependency-light heart of Origin Studio.
//!
//! It exists so the pieces that decide *what an environment is*, *what a file is*,
//! *where a jump goes*, *when an automation runs* and *which agents are supported* are
//! shared by every surface: the Android app, the companion, the CLI and a host you own.
//!
//! The crate deliberately has no platform bindings, no network client and no storage
//! backend. Callers pass values in and get values out. That keeps it testable on any
//! machine and makes the contract easy to reimplement if a platform needs to.

pub mod agents;
pub mod automation;
pub mod environment;
pub mod jump;
pub mod preview;
pub mod theme;
pub mod usage;

/// The version of the core contract. This is not the app version: bump it only when a
/// consumer-visible shape changes.
pub const CORE_VERSION: &str = "origin.core/v1";

/// The release line that first shipped this core.
pub const RELEASE: &str = "0.0.10";

/// A short, stable fingerprint for diagnostics and support reports.
pub fn fingerprint() -> String {
    format!("{CORE_VERSION}@{RELEASE}")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn fingerprint_is_stable() {
        assert_eq!(fingerprint(), "origin.core/v1@0.0.10");
    }
}
