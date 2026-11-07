# Origin Studio brand

The identity is one geometric mark, used everywhere: launcher icon, website, releases,
notifications and documentation. It replaces the retired folder mark in full.

## The mark

**The Origin Loop.** A bold open ring with a solid orb at its exact centre.

- The **ring** is the workspace: one continuous loop around the work.
- The **orb** is the origin: your own machine at the centre, not a vendor's cloud.
- The **opening** faces lower right, the direction work moves. It is also where the
  next thing enters a system that is designed to be extended and replaced.

It is drawn from four numbers, not traced from a screenshot, so every size is sharp:
ring radius 23.5, ring weight 10.5, orb radius 8.5, 52° opening, in a 108 × 108 box.
Round caps soften the engineering geometry without making it toy-like.

The mark is deliberately not a folder, a terminal prompt, a cloud, a robot or a
letter-in-a-box. It has to survive being 16 px in a notification tray and 512 px on a
store listing, and it does: two shapes, one colour.

## Colour

| Token | Value | Use |
| --- | --- | --- |
| Origin Orange | `#FF7A2E` | The mark on ink, the primary action colour in the app |
| Ink | `#0B0B0D` | Icon tile, dark surfaces, monochrome mark on light |
| Paper | `#F6F2EA` | Website and documentation background |
| Muted | `#6A645A` | Secondary type on paper |
| Rule | `#D8D0C0` | Hairlines on paper |

The mark is a single flat colour. No gradients, shadows, bevels or outlines. On ink,
use Orange. On paper, prefer the ink tile so the orange keeps its warmth and contrast.
The monochrome layer is pure white so Android themed icons can tint it.

## Clear space and sizes

- **Clear space:** at least half the orb diameter on every side. Nothing touches the ring.
- **Launcher:** the mark occupies 60 dp of the 108 dp adaptive canvas, safely inside the
  66 dp guaranteed-visible circle. The background is Ink, edge to edge.
- **Minimum size:** 16 px for the mark; 24 px for the full tile. Below that, use the tile
  without additional detail.
- **Wordmark:** JetBrains Mono, uppercase, generous tracking, paired with the mark. It is
  a type lockup, never redrawn as art.

## Asset inventory

| File | Purpose |
| --- | --- |
| `tools/brand/logo.svg` | Primary vector mark, orange on transparent |
| `tools/brand/logo-mono.svg` | One-colour vector mark |
| `tools/brand/app-icon.svg` | Mark on the rounded ink tile |
| `tools/brand/logo-lockup.svg` | Horizontal mark + wordmark |
| `tools/brand/logo-lockup.png` | Raster lockup, 2× |
| `tools/brand/logo-mark.png` | Transparent master, 1024 px |
| `tools/brand/logo-source.png` | Ink tile master, 1024 px |
| `tools/brand/social-preview.svg` | Social card source (1280 × 640) |
| `docs/media/logo.png` | Website mast and PNG favicon, 512 px tile |
| `docs/media/favicon.svg` | Vector favicon |
| `docs/media/app-icon-512.png` | README and store listing, 512 px tile |
| `docs/media/social-preview.png` | Open Graph / Twitter card |
| `app/src/main/res/mipmap-*/ic_launcher_foreground.png` | Adaptive foreground, all densities |
| `app/src/main/res/mipmap-*/ic_launcher_monochrome.png` | Themed icon layer, all densities |

## Regenerating

The mark and every bitmap come from standard-library Python:

```bash
python3 tools/make-brand.py
```

The social card and lockup contain type and are rendered once with the resvg rasteriser:

```bash
npm install --no-save @resvg/resvg-js
node tools/brand/render-social.mjs
```

Both PNG sets are committed, so builds never need Node, a font server or an image
pipeline. `tools/make-brand.py` is the source of record; edit the numbers at the top of
the file, not the generated files.

## Usage

- Use the mark on a plain Ink tile for launcher, store, social and documentation.
- Use the transparent mark when it sits on a surface the brand already controls.
- Keep the mark upright. Do not rotate, stretch, outline, recolour outside the palette,
  add a second colour, place it in a speech bubble, or rebuild it from a different shape.
- Do not pair it with another logo at equal weight. OneOrigin endorses; it does not compete.

## Release history and immutability

`v0.0.1` shipped the retired folder mark. Published artifacts are never re-signed,
re-tagged or silently modified, so that release stays exactly as published. The Origin
Loop debuts in `v0.0.2` and everything after it. `v0.0.2` supersedes `v0.0.1` as the
branded release; it does not rewrite it.
