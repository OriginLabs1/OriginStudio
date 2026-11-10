import { Easing } from "remotion";

export const FPS = 60;

export const C = {
  bg: "#05070E",
  text: "#EEF1FF",
  dim: "#A7AECB",
  faint: "#7B83A3",
  accent: "#9EC1E6",
  accentSoft: "rgba(158,193,230,0.14)",
};

export const SANS = "Outfit, sans-serif";
export const MONO = "JetBrains Mono, monospace";

/** The one easing curve everything moves on: quick start, long soft landing. */
export const EASE = Easing.bezier(0.16, 1, 0.3, 1);
/** For camera moves between shots: symmetric and unhurried. */
export const GLIDE = Easing.bezier(0.65, 0, 0.35, 1);

export const s = (seconds: number) => Math.round(seconds * FPS);
