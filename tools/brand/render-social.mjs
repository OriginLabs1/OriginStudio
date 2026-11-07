// Renders the type-bearing brand assets. Requires the resvg rasteriser once:
//
//   npm install --no-save @resvg/resvg-js
//   node tools/brand/render-social.mjs
//
// The PNGs are committed, so a normal build never needs Node or this script.
import { Resvg } from "@resvg/resvg-js";
import { readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, "..", "..");
const fontFiles = [join(root, "app/src/main/res/font/jetbrains_mono.ttf")];

const jobs = [
  ["social-preview.svg", join(root, "docs/media/social-preview.png"), 1],
  ["logo-lockup.svg", join(here, "logo-lockup.png"), 2],
];

for (const [src, out, zoom] of jobs) {
  const svg = readFileSync(join(here, src), "utf8");
  const png = new Resvg(svg, {
    font: { fontFiles, loadSystemFonts: false, defaultFontFamily: "JetBrains Mono" },
    fitTo: { mode: "zoom", value: zoom },
  }).render().asPng();
  writeFileSync(out, png);
  console.log(`${out} (${png.length} bytes)`);
}
