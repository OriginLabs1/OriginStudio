//! The agent catalog.
//!
//! Origin Studio is not an agent. It is the room the agents work in: the files, the
//! terminal, the browser, the phone. This catalog says which agents the app offers,
//! how each one arrives, and what it can take from the composer.

use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "kebab-case")]
pub enum AgentKind {
    /// Ships with Origin Studio and speaks the app's own protocol.
    Origin,
    /// An ACP agent, installed from the ACP registry on first use.
    Acp,
    /// Any command that runs in the Linux environment.
    Shell,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "kebab-case")]
pub enum Install {
    /// Already in the app; nothing to download.
    Bundled,
    /// Downloaded through the ACP registry.
    Registry,
    /// Already in the Debian guest, or installable with apt.
    Apt,
    /// Whatever the user types.
    UserCommand,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AgentSpec {
    pub id: String,
    pub name: String,
    pub tagline: String,
    pub kind: AgentKind,
    pub install: Install,
    /// The registry id (ACP) or the command (shell).
    pub entry: String,
    pub images: bool,
    pub files: bool,
    pub terminal: bool,
    pub note: String,
}

fn agent(
    id: &str,
    name: &str,
    tagline: &str,
    kind: AgentKind,
    install: Install,
    entry: &str,
    images: bool,
    files: bool,
    terminal: bool,
    note: &str,
) -> AgentSpec {
    AgentSpec {
        id: id.into(),
        name: name.into(),
        tagline: tagline.into(),
        kind,
        install,
        entry: entry.into(),
        images,
        files,
        terminal,
        note: note.into(),
    }
}

/// Every agent the app is designed around. "Origin Agent" is the built-in one; the rest
/// are first-class CLI agents that keep their own accounts, models and habits.
pub fn catalog() -> Vec<AgentSpec> {
    vec![
        agent(
            "origin",
            "Origin Agent",
            "The built-in profile: your workspace, your chosen agent, one approvals sheet.",
            AgentKind::Origin,
            Install::Bundled,
            "profile:auto",
            true,
            true,
            true,
            "No second subscription and no second login: it pairs the workspace tools with the agent you already use, and says so when none is installed.",
        ),
        agent(
            "claude-acp",
            "Claude Code",
            "Anthropic's terminal agent, in the room with your files and browser.",
            AgentKind::Acp,
            Install::Registry,
            "claude-acp",
            true,
            true,
            true,
            "Sign in once in the terminal; the session keeps its own profile.",
        ),
        agent(
            "codex-acp",
            "Codex",
            "OpenAI's coding agent, with the same files and previews beside it.",
            AgentKind::Acp,
            Install::Registry,
            "codex-acp",
            true,
            true,
            true,
            "Uses the Codex CLI login already in the guest when there is one.",
        ),
        agent(
            "opencode",
            "OpenCode",
            "An open, provider-agnostic agent with a large model menu.",
            AgentKind::Acp,
            Install::Registry,
            "opencode",
            true,
            true,
            true,
            "Bring any provider key, or use a local model.",
        ),
        agent(
            "cursor",
            "Cursor",
            "The Cursor agent for people already living in that workflow.",
            AgentKind::Acp,
            Install::Registry,
            "cursor",
            true,
            true,
            true,
            "Requires a Cursor account.",
        ),
        agent(
            "gemini",
            "Gemini CLI",
            "Google's terminal agent, with a generous free tier.",
            AgentKind::Acp,
            Install::Registry,
            "gemini",
            true,
            true,
            true,
            "Uses the Gemini CLI login.",
        ),
        agent(
            "devin",
            "Devin",
            "Cognition's agent, for work you want to hand off.",
            AgentKind::Acp,
            Install::Registry,
            "devin",
            true,
            true,
            false,
            "Runs against a Devin account.",
        ),
        agent(
            "pi-acp",
            "Pi",
            "A small, fast agent that stays out of the way.",
            AgentKind::Acp,
            Install::Registry,
            "pi-acp",
            false,
            true,
            true,
            "Good on a phone: short turns, small context.",
        ),
        agent(
            "omp",
            "Oh My Pi",
            "A standalone ACP build with no Node dependency.",
            AgentKind::Acp,
            Install::Registry,
            "omp",
            true,
            true,
            true,
            "Ships as a single binary in the guest.",
        ),
        agent(
            "any-cli",
            "Any CLI agent",
            "A shell session running whichever agent you already trust.",
            AgentKind::Shell,
            Install::UserCommand,
            "",
            false,
            false,
            true,
            "Type `aider`, `goose`, `amp`, or your own script. The app gets out of the way.",
        ),
    ]
}

pub fn find(id: &str) -> Option<AgentSpec> {
    catalog().into_iter().find(|agent| agent.id == id)
}

/// The agents offered first when someone adds a session, in order. "Any CLI agent" is a
/// route, not a download, so it stays last.
pub fn offered() -> Vec<AgentSpec> {
    catalog()
        .into_iter()
        .filter(|agent| agent.kind != AgentKind::Shell)
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn origin_agent_is_first_and_bundled() {
        let first = catalog().into_iter().next().unwrap();
        assert_eq!(first.id, "origin");
        assert_eq!(first.kind, AgentKind::Origin);
        assert_eq!(first.install, Install::Bundled);
    }

    #[test]
    fn every_agent_has_a_reason_to_exist() {
        for agent in catalog() {
            assert!(!agent.tagline.is_empty(), "{} has no tagline", agent.id);
            assert!(!agent.note.is_empty(), "{} has no note", agent.id);
            if agent.kind == AgentKind::Acp {
                assert_eq!(agent.install, Install::Registry);
            }
        }
    }

    #[test]
    fn shell_route_is_last() {
        let all = catalog();
        assert_eq!(all.last().unwrap().id, "any-cli");
        assert!(offered().iter().all(|a| a.kind != AgentKind::Shell));
    }
}
