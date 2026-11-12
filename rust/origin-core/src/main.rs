//! `origin-core` CLI: the same answers the app gives, from a terminal.
//!
//! No flags library, no network, no state. It is a small window into the core so the
//! contract can be checked from a script, a host or a support session.

use origin_core::{agents, automation, environment, fingerprint, jump, preview, theme, usage};
use serde::Serialize;

fn flag(args: &[String], name: &str) -> Option<String> {
    args.iter()
        .position(|arg| arg == name)
        .and_then(|index| args.get(index + 1))
        .cloned()
}

fn has(args: &[String], name: &str) -> bool {
    args.iter().any(|arg| arg == name)
}

fn print_json<T: Serialize>(value: &T) {
    println!("{}", serde_json::to_string_pretty(value).expect("serialisable"));
}

fn main() {
    let args: Vec<String> = std::env::args().skip(1).collect();
    let command = args.first().map(String::as_str).unwrap_or("fingerprint");
    let json = has(&args, "--json");

    match command {
        "fingerprint" | "version" => {
            if json {
                print_json(&serde_json::json!({
                    "fingerprint": fingerprint(),
                    "release": origin_core::RELEASE,
                    "core": origin_core::CORE_VERSION,
                }));
            } else {
                println!("{}", fingerprint());
            }
        }
        "environments" | "envs" => {
            let catalog = environment::catalog();
            if json {
                print_json(&catalog);
            } else {
                for option in catalog {
                    println!(
                        "{:<10} {:<6} {}\n  {} — {}",
                        option.name,
                        option.recommendation.caption(),
                        option.version,
                        option.summary,
                        option.storage
                    );
                }
            }
        }
        "recommend" => {
            let handheld = !has(&args, "--host");
            let option = environment::find(environment::recommended_for(handheld));
            if json {
                print_json(&option);
            } else {
                println!("{} — {}", option.name, option.caption);
                println!("{}", option.summary);
                for limit in option.honest_limits {
                    println!("  · {limit}");
                }
            }
        }
        "agents" => {
            let catalog = agents::catalog();
            if json {
                print_json(&catalog);
            } else {
                for agent in catalog {
                    println!("{:<16} {:<10} {}", agent.name, format!("{:?}", agent.kind).to_lowercase(), agent.tagline);
                }
            }
        }
        "themes" => {
            let catalog = theme::catalog();
            if json {
                print_json(&catalog);
            } else {
                for item in catalog {
                    println!("{:<10} {} — {}", item.id, item.name, item.note);
                }
            }
        }
        "preview" => {
            let path = args.get(1).cloned().unwrap_or_else(|| {
                eprintln!("usage: origin-core preview <path> [--json]");
                std::process::exit(2);
            });
            let preview = preview::for_path(&path);
            if json {
                print_json(&serde_json::json!({ "path": path, "preview": preview, "summary": preview::summarise(&preview) }));
            } else {
                println!("{}: {} — {}", path, preview.label, preview::summarise(&preview));
            }
        }
        "outline" => {
            let path = args.get(1).cloned().unwrap_or_else(|| {
                eprintln!("usage: origin-core outline <markdown-file>");
                std::process::exit(2);
            });
            match std::fs::read_to_string(&path) {
                Ok(source) => {
                    for (level, title) in preview::markdown_outline(&source) {
                        println!("{}{} {}", "  ".repeat(level.saturating_sub(1) as usize), "#".repeat(level as usize), title);
                    }
                }
                Err(error) => {
                    eprintln!("could not read {path}: {error}");
                    std::process::exit(1);
                }
            }
        }
        "next-run" => {
            let now = std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .map(|d| d.as_millis() as i64)
                .unwrap_or(0);
            let schedule = if let Some(minutes) = flag(&args, "--every-minutes").and_then(|v| v.parse::<u32>().ok()) {
                automation::Schedule::Interval { every_minutes: minutes }
            } else if let Some(time) = flag(&args, "--daily") {
                parse_time(&time).map(|(hour, minute)| automation::Schedule::Daily { hour, minute }).unwrap_or(automation::Schedule::Manual)
            } else if let Some(time) = flag(&args, "--weekly") {
                let weekday = flag(&args, "--weekday").and_then(|v| v.parse::<u8>().ok()).unwrap_or(1);
                parse_time(&time)
                    .map(|(hour, minute)| automation::Schedule::Weekly { weekday, hour, minute })
                    .unwrap_or(automation::Schedule::Manual)
            } else {
                automation::Schedule::Manual
            };
            let next = automation::next_run(schedule, now);
            if json {
                print_json(&serde_json::json!({
                    "schedule": schedule,
                    "description": automation::describe(schedule),
                    "next_run_ms": next,
                    "now_ms": now,
                }));
            } else {
                println!("{}", automation::describe(schedule));
                match next {
                    Some(at) => println!("next run at {at} ms ({})", usage::reset_in(at, now).unwrap_or_else(|| "now".into())),
                    None => println!("manual only"),
                }
            }
        }
        "jump" => {
            let query = flag(&args, "--query").unwrap_or_default();
            let mut index = jump::JumpIndex::default();
            let mut i = 0;
            while i < args.len() {
                if args[i] == "--entry" {
                    if let Some(raw) = args.get(i + 1) {
                        let parts: Vec<&str> = raw.splitn(3, '|').collect();
                        if parts.len() == 3 {
                            index.add(jump::JumpEntry::new(
                                format!("entry-{i}"),
                                parts[0],
                                parts[1],
                                jump::JumpKind::Action,
                                parts[2],
                            ));
                        }
                    }
                    i += 1;
                }
                i += 1;
            }
            let results = index.search(&query, 8);
            if json {
                print_json(&results);
            } else if results.is_empty() {
                println!("no matches for {query:?}");
            } else {
                for (rank, entry) in results.iter().enumerate() {
                    println!("{}. {}  ({})", rank + 1, entry.title, entry.subtitle);
                }
            }
        }
        "why" => {
            println!("Origin Studio is the room, not another agent.");
            println!("  · One workspace, every agent: Origin Agent, Claude Code, Codex, OpenCode, Cursor, Gemini, Pi, omp, or any CLI.");
            println!("  · A real Linux environment on the phone (Debian, offline, no root), or Dakota OS on a host you own.");
            println!("  · Jump Anywhere: Ctrl/Cmd+K across workspaces, sessions, repos, files, hosts and actions.");
            println!("  · Previews that are real: images, PDFs page by page, rendered Markdown with an outline, source in the editor.");
            println!("  · The phone is a first-class surface: dictation, drag-and-drop from another app, a device lab beside the agent.");
            println!("  · Automations run visibly, in sessions you can read, with the approvals you already set.");
            println!("  · No account wall. Usage and limits are shown, and accounts switch without signing out.");
        }
        "-h" | "--help" | "help" => {
            println!("origin-core {}", origin_core::RELEASE);
            println!();
            println!("  origin-core fingerprint");
            println!("  origin-core environments [--json]");
            println!("  origin-core recommend [--host]");
            println!("  origin-core agents [--json]");
            println!("  origin-core themes [--json]");
            println!("  origin-core preview <path> [--json]");
            println!("  origin-core outline <markdown-file>");
            println!("  origin-core next-run [--every-minutes N | --daily HH:MM | --weekly HH:MM --weekday N]");
            println!("  origin-core jump --query <text> [--entry \"title|subtitle|target\"]...");
            println!("  origin-core why");
        }
        other => {
            eprintln!("unknown command {other:?}; try `origin-core help`");
            std::process::exit(2);
        }
    }
}

fn parse_time(value: &str) -> Option<(u8, u8)> {
    let (hour, minute) = value.split_once(':')?;
    Some((hour.parse().ok()?, minute.parse().ok()?))
}
