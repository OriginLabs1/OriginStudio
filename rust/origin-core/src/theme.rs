//! Premium surfaces.
//!
//! The app has one deep-space palette at its centre; these are the finishes around it.
//! Every theme keeps the same contrast discipline: a primary action is unmistakable at
//! arm's length, and no semantic colour is spent on chrome.

use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Colors {
    pub bg: String,
    pub card: String,
    pub border: String,
    pub text: String,
    pub text_dim: String,
    pub accent: String,
    pub accent_soft: String,
    pub teal: String,
    pub violet: String,
    pub rose: String,
    pub green: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PremiumTheme {
    pub id: String,
    pub name: String,
    pub note: String,
    pub colors: Colors,
}

fn colors(
    bg: &str,
    card: &str,
    border: &str,
    text: &str,
    text_dim: &str,
    accent: &str,
    accent_soft: &str,
    teal: &str,
    violet: &str,
    rose: &str,
    green: &str,
) -> Colors {
    Colors {
        bg: bg.into(),
        card: card.into(),
        border: border.into(),
        text: text.into(),
        text_dim: text_dim.into(),
        accent: accent.into(),
        accent_soft: accent_soft.into(),
        teal: teal.into(),
        violet: violet.into(),
        rose: rose.into(),
        green: green.into(),
    }
}

/// `ink` is the palette the app already ships; the rest are new finishes for v0.0.10.
pub fn catalog() -> Vec<PremiumTheme> {
    vec![
        PremiumTheme {
            id: "ink".into(),
            name: "Ink".into(),
            note: "The original deep-space navy. OLED-black, one steel accent.".into(),
            colors: colors(
                "#05070E", "#0E1322", "#232B45", "#EEF1FF", "#A7AECB", "#9EC1E6", "#249EC1E6",
                "#43C6C9", "#9A8CFF", "#FF9FBC", "#9FD4A8",
            ),
        },
        PremiumTheme {
            id: "aurora".into(),
            name: "Aurora".into(),
            note: "Ink warmed by the portal aurora: violet leads, teal answers.".into(),
            colors: colors(
                "#070511", "#140F24", "#2C2347", "#F2EDFF", "#B4A9D6", "#C3B2FF", "#2AC3B2FF",
                "#4FD8D9", "#C3B2FF", "#FFA8C8", "#A8E6C0",
            ),
        },
        PremiumTheme {
            id: "graphite".into(),
            name: "Graphite".into(),
            note: "Neutral, low-chroma surfaces for daylight and long reviews.".into(),
            colors: colors(
                "#0B0C0E", "#15171B", "#2B2F36", "#F1F3F7", "#AEB4BF", "#A8C6EA", "#26A8C6EA",
                "#63C7C9", "#A79BFF", "#F2A2BC", "#A6D6AE",
            ),
        },
        PremiumTheme {
            id: "verdant".into(),
            name: "Verdant".into(),
            note: "A green-black finish for calm, all-day sessions.".into(),
            colors: colors(
                "#050B09", "#0D1714", "#20362E", "#ECFFF7", "#A3C4B5", "#8FE3C2", "#268FE3C2",
                "#59D6C2", "#A79BFF", "#F5A8BE", "#9FE0A8",
            ),
        },
        PremiumTheme {
            id: "ember".into(),
            name: "Ember".into(),
            note: "Rose-copper warmth for evening work, still legible at arm's length.".into(),
            colors: colors(
                "#0C0709", "#1A1015", "#3A2430", "#FFF1F4", "#C9A9B6", "#FFB4C8", "#2AFFB4C8",
                "#7FD4C7", "#B9A4FF", "#FFB4C8", "#A8DFB0",
            ),
        },
    ]
}

pub fn find(id: &str) -> Option<PremiumTheme> {
    catalog().into_iter().find(|theme| theme.id == id)
}

/// WCAG relative luminance, so a consumer can check a theme without reimplementing it.
pub fn luminance(hex: &str) -> f32 {
    let raw = hex.trim_start_matches('#');
    let parse = |range: std::ops::Range<usize>| u8::from_str_radix(&raw[range], 16).unwrap_or(0) as f32 / 255.0;
    let channel = |c: f32| {
        if c <= 0.039_28 {
            c / 12.92
        } else {
            ((c + 0.055) / 1.055).powf(2.4)
        }
    };
    if raw.len() != 6 {
        return 0.0;
    }
    0.2126 * channel(parse(0..2)) + 0.7152 * channel(parse(2..4)) + 0.0722 * channel(parse(4..6))
}

/// Contrast ratio between two hex colours, as used by WCAG.
pub fn contrast(a: &str, b: &str) -> f32 {
    let (la, lb) = (luminance(a), luminance(b));
    let (hi, lo) = if la > lb { (la, lb) } else { (lb, la) };
    (hi + 0.05) / (lo + 0.05)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn every_theme_is_readable() {
        for theme in catalog() {
            let body = contrast(&theme.colors.text, &theme.colors.bg);
            assert!(body >= 7.0, "{} body contrast {body}", theme.id);
            let dim = contrast(&theme.colors.text_dim, &theme.colors.bg);
            assert!(dim >= 4.5, "{} dim contrast {dim}", theme.id);
            let accent = contrast(&theme.colors.accent, &theme.colors.bg);
            assert!(accent >= 4.5, "{} accent contrast {accent}", theme.id);
        }
    }

    #[test]
    fn ink_is_still_ink() {
        let ink = find("ink").unwrap();
        assert_eq!(ink.colors.bg, "#05070E");
        assert_eq!(ink.colors.accent, "#9EC1E6");
    }

    #[test]
    fn ids_are_unique() {
        let all = catalog();
        let mut ids: Vec<&str> = all.iter().map(|t| t.id.as_str()).collect();
        ids.sort_unstable();
        ids.dedup();
        assert_eq!(ids.len(), all.len());
    }
}
