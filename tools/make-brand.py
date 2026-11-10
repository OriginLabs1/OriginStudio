"""Generates the Origin Studio brand mark and every bitmap that carries it.

The mark is one shape: a bold open ring with a solid orb at its exact centre. The ring
is the loop of the workspace; the orb is the origin, the user's own machine at the
centre of it; the opening is where new work enters. It is drawn from geometry, not
traced, so every size is produced from the same numbers and stays crisp.

Run with no arguments (standard library only):

    python3 tools/make-brand.py

Outputs:
    tools/brand/logo.svg                  primary mark, steel blue on transparent
    tools/brand/logo-mono.svg             one-colour mark for stamps and themed icons
    tools/brand/app-icon.svg              mark on the dark rounded tile
    tools/brand/logo-lockup.svg           horizontal mark + wordmark
    tools/brand/logo-mark.png             transparent mark, 1024 px
    tools/brand/logo-source.png           dark tile, 1024 px (design source of record)
    app/src/main/res/mipmap-*/ic_launcher_foreground.png
    app/src/main/res/mipmap-*/ic_launcher_monochrome.png
    docs/media/logo.png                   512 px tile (site mast and favicon)
    docs/media/app-icon-512.png           512 px tile (README and store listings)
    docs/media/favicon.svg                vector favicon

The social preview card with type is rendered separately by tools/brand/render-social.mjs,
because type needs a font rasteriser.
"""

import math
import os
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BRAND = os.path.join(ROOT, "tools", "brand")
RES = os.path.join(ROOT, "app", "src", "main", "res")
MEDIA = os.path.join(ROOT, "docs", "media")

# --- mark geometry, in a 108 x 108 design box centred on (54, 54) -------------------------
BOX = 108.0
CX = CY = 54.0
RING_R = 23.5          # radius of the ring's centreline
RING_W = 10.5          # stroke weight
ORB_R = 8.5            # the origin orb
GAP_DEG = 52.0         # opening in the ring, before the round caps close part of it
GAP_CENTRE = 45.0      # opening faces lower right (SVG y grows down)
OUTER_R = RING_R + RING_W / 2.0      # 28.75
MARK_W = OUTER_R * 2.0               # 57.5
# Adaptive icons guarantee a 66 dp circle; the mark is 60 dp so it never clips.
ADAPTIVE_CANVAS_DP = 108.0
ADAPTIVE_MARK_DP = 60.0

ACCENT = (158, 193, 230)
INK = (5, 6, 15)
PAPER = (245, 247, 252)
WHITE = (255, 255, 255)

DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}


# --- geometry ---------------------------------------------------------------------------
def _pt(deg):
    a = math.radians(deg)
    return CX + RING_R * math.cos(a), CY + RING_R * math.sin(a)


def _angle_in_gap(deg):
    start = (GAP_CENTRE - GAP_DEG / 2.0) % 360.0
    end = (GAP_CENTRE + GAP_DEG / 2.0) % 360.0
    if start < end:
        return start <= deg <= end
    return deg >= start or deg <= end


def inside_mark(x, y):
    """True when (x, y) in the 108 box is inside the mark."""
    dx, dy = x - CX, y - CY
    d = math.hypot(dx, dy)
    if d <= ORB_R:
        return True
    if abs(d - RING_R) <= RING_W / 2.0:
        ang = math.degrees(math.atan2(dy, dx)) % 360.0
        if not _angle_in_gap(ang):
            return True
    # Round caps: semicircles at both arc ends.
    for end in (_pt(GAP_CENTRE + GAP_DEG / 2.0), _pt(GAP_CENTRE - GAP_DEG / 2.0)):
        if math.hypot(x - end[0], y - end[1]) <= RING_W / 2.0:
            return True
    return False


def arc_path():
    x1, y1 = _pt(GAP_CENTRE + GAP_DEG / 2.0)
    x2, y2 = _pt(GAP_CENTRE - GAP_DEG / 2.0)
    large = 1 if 360.0 - GAP_DEG > 180.0 else 0
    return (
        f"M {x1:.3f} {y1:.3f} A {RING_R:g} {RING_R:g} 0 {large} 1 {x2:.3f} {y2:.3f}"
    )


# --- SVG --------------------------------------------------------------------------------
def mark_svg(color, extra=""):
    return (
        f'<path d="{arc_path()}" fill="none" stroke="{color}" stroke-width="{RING_W:g}" '
        f'stroke-linecap="round"/>\n'
        f'  <circle cx="{CX:g}" cy="{CY:g}" r="{ORB_R:g}" fill="{color}"/>' + extra
    )


def svg_document(body, size=108):
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" '
        f'viewBox="0 0 108 108" role="img" aria-label="Origin Studio">\n  {body}\n</svg>\n'
    )


def tile_svg(mark_color, bg, size=512, mark_fraction=0.60, radius_fraction=0.225):
    scale = (size * mark_fraction) / MARK_W
    tx = size / 2.0
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" '
        f'viewBox="0 0 {size} {size}" role="img" aria-label="Origin Studio">\n'
        f'  <rect width="{size}" height="{size}" rx="{size * radius_fraction:.3f}" fill="{bg}"/>\n'
        f'  <g transform="translate({tx:g} {tx:g}) scale({scale:.6f}) translate(-{CX:g} -{CY:g})">\n'
        f'    {mark_svg(mark_color)}\n'
        f'  </g>\n</svg>\n'
    )


# --- rasteriser -------------------------------------------------------------------------
def _rounded_rect_inside(x, y, w, h, r):
    qx = abs(x - w / 2.0) - (w / 2.0 - r)
    qy = abs(y - h / 2.0) - (h / 2.0 - r)
    outside = math.hypot(max(qx, 0.0), max(qy, 0.0))
    inside = min(max(qx, qy), 0.0)
    return outside + inside - r <= 0.0


def _coverage(fn, px, py, scale, ox, oy, samples=4):
    hits = 0
    for sy in range(samples):
        for sx in range(samples):
            x = (px + (sx + 0.5) / samples) / scale - ox
            y = (py + (sy + 0.5) / samples) / scale - oy
            hits += 1 if fn(x, y) else 0
    return hits / float(samples * samples)


def render_mark(size, color, mark_fraction=0.92, samples=4):
    """Transparent RGBA mark, mark_fraction of the canvas width."""
    scale = (size * mark_fraction) / MARK_W
    ox = (size / scale - BOX) / 2.0
    oy = ox
    buf = bytearray()
    r, g, b = color
    for py in range(size):
        buf.append(0)  # PNG filter: none
        for px in range(size):
            a = _coverage(inside_mark, px, py, scale, ox, oy, samples)
            buf += bytes((r, g, b, int(round(a * 255.0))))
    return bytes(buf)


def render_tile(size, mark_color, bg, mark_fraction=0.60, radius_fraction=0.225, samples=4):
    """RGBA rounded tile with the mark centred on it."""
    scale = (size * mark_fraction) / MARK_W
    ox = (size / scale - BOX) / 2.0
    radius = size * radius_fraction
    buf = bytearray()
    mr, mg, mb = mark_color
    br, bgc, bb = bg
    for py in range(size):
        buf.append(0)
        for px in range(size):
            ma = _coverage(inside_mark, px, py, scale, ox, ox, samples)
            ba = _coverage(
                lambda x, y: _rounded_rect_inside(x, y, size, size, radius),
                px, py, 1.0, 0.0, 0.0, samples,
            )
            a = ma + ba * (1.0 - ma)
            if a <= 0.0:
                buf += b"\x00\x00\x00\x00"
                continue
            cr = (mr * ma + br * ba * (1.0 - ma)) / a
            cg = (mg * ma + bgc * ba * (1.0 - ma)) / a
            cb = (mb * ma + bb * ba * (1.0 - ma)) / a
            buf += bytes((int(round(cr)), int(round(cg)), int(round(cb)), int(round(a * 255.0))))
    return bytes(buf)


def write_png(path, size, raw):
    def chunk(tag, data):
        return (
            struct.pack(">I", len(data))
            + tag
            + data
            + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        )

    png = (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as fh:
        fh.write(png)
    return len(png)


def main():
    # SVG sources of record.
    files = {
        "logo.svg": svg_document(mark_svg("#9EC1E6")),
        "logo-mono.svg": svg_document(mark_svg("#05060F")),
        "app-icon.svg": tile_svg("#9EC1E6", "#05060F", 512),
        "favicon.svg": tile_svg("#9EC1E6", "#05060F", 64, radius_fraction=0.225),
    }
    for name, content in files.items():
        path = os.path.join(MEDIA if name == "favicon.svg" else BRAND, name)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(content)
        print(f"  {os.path.relpath(path, ROOT)}")

    # Full-resolution brand masters.
    write_png(os.path.join(BRAND, "logo-mark.png"), 1024,
              render_mark(1024, ACCENT, 0.94, samples=4))
    write_png(os.path.join(BRAND, "logo-source.png"), 1024,
              render_tile(1024, ACCENT, INK, 0.60, samples=4))
    print("  tools/brand/logo-mark.png")
    print("  tools/brand/logo-source.png")

    # Adaptive launcher layers: 108 dp canvas, 60 dp mark.
    for name, factor in DENSITIES.items():
        canvas = int(round(ADAPTIVE_CANVAS_DP * factor))
        fraction = ADAPTIVE_MARK_DP / ADAPTIVE_CANVAS_DP
        folder = os.path.join(RES, f"mipmap-{name}")
        write_png(os.path.join(folder, "ic_launcher_foreground.png"), canvas,
                  render_mark(canvas, ACCENT, fraction, samples=4))
        write_png(os.path.join(folder, "ic_launcher_monochrome.png"), canvas,
                  render_mark(canvas, WHITE, fraction, samples=4))
        print(f"  mipmap-{name}: {canvas} px")

    # Web assets.
    tile = render_tile(512, ACCENT, INK, 0.60, samples=4)
    write_png(os.path.join(MEDIA, "logo.png"), 512, tile)
    write_png(os.path.join(MEDIA, "app-icon-512.png"), 512, tile)
    print("  docs/media/logo.png")
    print("  docs/media/app-icon-512.png")
    print("  docs/media/favicon.svg")


if __name__ == "__main__":
    main()
