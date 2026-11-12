//! What a file is, and how the app should show it.
//!
//! The rule is simple: open the thing rather than a dialog that says the thing cannot be
//! opened. Images, PDFs, Markdown and source all have a real surface; only the genuinely
//! unknown falls back to handing the file to another app.

use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "kebab-case")]
pub enum PreviewKind {
    Image,
    Pdf,
    Markdown,
    Code,
    Text,
    Audio,
    Video,
    Archive,
    Font,
    Unknown,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct Preview {
    pub kind: PreviewKind,
    /// A short language or format label, e.g. `rust`, `pdf`, `markdown`.
    pub label: String,
    /// True when the app can render it itself instead of handing it to another app.
    pub inline: bool,
    /// True when the surface should be paged rather than scrolled.
    pub paged: bool,
}

fn extension(path: &str) -> String {
    path.rsplit('/')
        .next()
        .unwrap_or(path)
        .rsplit_once('.')
        .map(|(_, ext)| ext.to_ascii_lowercase())
        .unwrap_or_default()
}

/// Classify a path by name alone: no filesystem access, so the same answer is available
/// on the phone, in the CLI and on a host.
pub fn for_path(path: &str) -> Preview {
    let ext = extension(path);
    let (kind, label) = match ext.as_str() {
        "png" | "jpg" | "jpeg" | "gif" | "webp" | "bmp" | "heic" | "avif" | "svg" => {
            (PreviewKind::Image, "image")
        }
        "pdf" => (PreviewKind::Pdf, "pdf"),
        "md" | "markdown" | "mdx" => (PreviewKind::Markdown, "markdown"),
        "rs" => (PreviewKind::Code, "rust"),
        "kt" | "kts" => (PreviewKind::Code, "kotlin"),
        "java" => (PreviewKind::Code, "java"),
        "swift" => (PreviewKind::Code, "swift"),
        "py" => (PreviewKind::Code, "python"),
        "js" | "mjs" | "cjs" | "jsx" => (PreviewKind::Code, "javascript"),
        "ts" | "tsx" => (PreviewKind::Code, "typescript"),
        "go" => (PreviewKind::Code, "go"),
        "c" | "h" => (PreviewKind::Code, "c"),
        "cc" | "cpp" | "hpp" => (PreviewKind::Code, "c++"),
        "rb" => (PreviewKind::Code, "ruby"),
        "sh" | "bash" | "zsh" | "fish" => (PreviewKind::Code, "shell"),
        "toml" => (PreviewKind::Code, "toml"),
        "yaml" | "yml" => (PreviewKind::Code, "yaml"),
        "json" => (PreviewKind::Code, "json"),
        "xml" => (PreviewKind::Code, "xml"),
        "css" => (PreviewKind::Code, "css"),
        "html" | "htm" => (PreviewKind::Code, "html"),
        "sql" => (PreviewKind::Code, "sql"),
        "txt" | "log" | "csv" | "tsv" | "ini" | "conf" | "env" => (PreviewKind::Text, "text"),
        "mp3" | "wav" | "m4a" | "ogg" | "flac" => (PreviewKind::Audio, "audio"),
        "mp4" | "webm" | "mov" | "mkv" => (PreviewKind::Video, "video"),
        "zip" | "tar" | "gz" | "tgz" | "xz" | "bz2" | "7z" => (PreviewKind::Archive, "archive"),
        "ttf" | "otf" | "woff" | "woff2" => (PreviewKind::Font, "font"),
        _ => (PreviewKind::Unknown, "file"),
    };
    Preview {
        kind,
        label: label.into(),
        inline: matches!(
            kind,
            PreviewKind::Image
                | PreviewKind::Pdf
                | PreviewKind::Markdown
                | PreviewKind::Code
                | PreviewKind::Text
                | PreviewKind::Audio
                | PreviewKind::Video
        ),
        paged: kind == PreviewKind::Pdf,
    }
}

/// Headings in the order they appear, for a Markdown outline beside the text.
pub fn markdown_outline(source: &str) -> Vec<(u8, String)> {
    source
        .lines()
        .filter_map(|line| {
            let trimmed = line.trim_start();
            if !trimmed.starts_with('#') {
                return None;
            }
            let level = trimmed.chars().take_while(|c| *c == '#').count();
            if level == 0 || level > 6 {
                return None;
            }
            let title = trimmed[level..].trim().trim_end_matches('#').trim();
            if title.is_empty() {
                None
            } else {
                Some((level as u8, title.to_string()))
            }
        })
        .collect()
}

/// A one-line summary for a jump result or a file chip.
pub fn summarise(preview: &Preview) -> String {
    match preview.kind {
        PreviewKind::Image => "Image, opens in the viewer".into(),
        PreviewKind::Pdf => "PDF, page by page, no download".into(),
        PreviewKind::Markdown => "Markdown, rendered with an outline".into(),
        PreviewKind::Code => format!("{} source, in the editor", preview.label),
        PreviewKind::Text => "Text, in the editor".into(),
        PreviewKind::Audio => "Audio, plays in place".into(),
        PreviewKind::Video => "Video, plays in place".into(),
        PreviewKind::Archive => "Archive, extract with the file manager".into(),
        PreviewKind::Font => "Font, no safe preview".into(),
        PreviewKind::Unknown => "No built-in preview; opens with another app".into(),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn pdf_is_inline_and_paged() {
        let preview = for_path("/root/projects/paper.PDF");
        assert_eq!(preview.kind, PreviewKind::Pdf);
        assert!(preview.inline);
        assert!(preview.paged);
    }

    #[test]
    fn markdown_and_code_are_inline_but_not_paged() {
        let md = for_path("README.md");
        assert_eq!(md.kind, PreviewKind::Markdown);
        assert!(md.inline);
        assert!(!md.paged);

        let rs = for_path("src/main.rs");
        assert_eq!(rs.label, "rust");
        assert!(rs.inline);
    }

    #[test]
    fn unknown_files_are_honest() {
        let preview = for_path("firmware.bin");
        assert_eq!(preview.kind, PreviewKind::Unknown);
        assert!(!preview.inline);
        assert!(summarise(&preview).contains("another app"));
    }

    #[test]
    fn outline_finds_headings_and_ignores_hashes_inside_lines() {
        let outline = markdown_outline("# Title\n\ntext # not a heading\n## Part one ##\n### \n");
        assert_eq!(
            outline,
            vec![(1, "Title".to_string()), (2, "Part one".to_string())]
        );
    }
}
