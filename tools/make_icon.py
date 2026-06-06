"""One-off generator for the mod icon. Draws three neutral "duplicate" cells on the
left merging — via converging emerald arrows — into a single glowing emerald
"canonical" cell on the right: the unification motif of Delightful Compat (sibling to
bedrock-crafting-controls' 3x3 grid, bedrock-line-placement's locked block row, and
partially-craftable-recipes' partial 2x2 grid). Supersampled for clean edges."""
from PIL import Image, ImageDraw, ImageFilter

S = 4  # supersampling factor
N = 256  # final size
W = N * S

img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
d = ImageDraw.Draw(img)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def sx(v):
    """Scale a value expressed in final (256px) units up to the supersampled canvas."""
    return v * S


# --- shared palette with the sibling mods -----------------------------------
top = (38, 42, 56)
bot = (24, 27, 38)
accent = (61, 199, 142)  # emerald highlight (the canonical item)
accent_dark = (38, 150, 104)
neutral = (62, 69, 92)
neutral_hi = (78, 86, 112)

# --- background: vertical gradient inside a rounded square ------------------
bg = Image.new("RGBA", (W, W), (0, 0, 0, 0))
bgd = ImageDraw.Draw(bg)
for y in range(W):
    bgd.line([(0, y), (W, y)], fill=lerp(top, bot, y / W) + (255,))
mask = Image.new("L", (W, W), 0)
ImageDraw.Draw(mask).rounded_rectangle([0, 0, W - 1, W - 1], radius=sx(52), fill=255)
img.paste(bg, (0, 0), mask)

# subtle border
d.rounded_rectangle(
    [sx(2), sx(2), W - sx(2), W - sx(2)], radius=sx(50), outline=(70, 78, 104, 255), width=sx(2)
)

# --- geometry (all in final 256px units, scaled by sx) ----------------------
# Three duplicate cells stacked on the left.
dup_cell = 46
dup_radius = 9
dup_x0 = 30
dup_x1 = dup_x0 + dup_cell
dup_centers_y = [72, 128, 184]  # top / middle / bottom

# One canonical cell on the right.
canon_cell = 82
canon_radius = 13
canon_x1 = 224
canon_x0 = canon_x1 - canon_cell
canon_cy = 128
canon_y0 = canon_cy - canon_cell / 2
canon_y1 = canon_cy + canon_cell / 2
canon_cx = (canon_x0 + canon_x1) / 2

# --- converging merge arrows (behind the cells) -----------------------------
# Each duplicate funnels into a convergence point just left of the canonical cell.
conv_x = canon_x0 - 16
conv_y = canon_cy
arrow_layer = Image.new("RGBA", (W, W), (0, 0, 0, 0))
ad = ImageDraw.Draw(arrow_layer)
stroke = sx(7)
for cy in dup_centers_y:
    pts = [(sx(dup_x1 + 8), sx(cy)), (sx(conv_x), sx(conv_y))]
    ad.line(pts, fill=accent + (200,), width=int(stroke), joint="curve")
    r = stroke / 2
    for px, py in pts:
        ad.ellipse([px - r, py - r, px + r, py + r], fill=accent + (200,))
# a single solid arrowhead pointing into the canonical cell
head = 13
tip_x = canon_x0 - 1
ad.polygon(
    [
        (sx(tip_x), sx(canon_cy)),
        (sx(tip_x - head), sx(canon_cy - head * 0.8)),
        (sx(tip_x - head), sx(canon_cy + head * 0.8)),
    ],
    fill=accent + (255,),
)
img.alpha_composite(arrow_layer)

# --- the three duplicate cells ----------------------------------------------
for cy in dup_centers_y:
    y0 = cy - dup_cell / 2
    y1 = cy + dup_cell / 2
    d.rounded_rectangle([sx(dup_x0), sx(y0), sx(dup_x1), sx(y1)], radius=sx(dup_radius), fill=neutral + (255,))
    # top highlight strip for a little depth (matches the siblings)
    d.rounded_rectangle(
        [sx(dup_x0), sx(y0), sx(dup_x1), sx(y0 + dup_cell * 0.5)],
        radius=sx(dup_radius),
        fill=neutral_hi + (90,),
    )

# --- the canonical cell (glowing emerald) -----------------------------------
# glow behind the cell
glow = Image.new("RGBA", (W, W), (0, 0, 0, 0))
ImageDraw.Draw(glow).rounded_rectangle(
    [sx(canon_x0 - 8), sx(canon_y0 - 8), sx(canon_x1 + 8), sx(canon_y1 + 8)],
    radius=sx(canon_radius + 6),
    fill=accent + (95,),
)
glow = glow.filter(ImageFilter.GaussianBlur(sx(6)))
img.alpha_composite(glow)
# vertical gradient fill
cw = int(sx(canon_cell))
cellimg = Image.new("RGBA", (cw, cw), (0, 0, 0, 0))
cd = ImageDraw.Draw(cellimg)
for yy in range(cw):
    cd.line([(0, yy), (cw, yy)], fill=lerp(accent, accent_dark, yy / cw) + (255,))
cmask = Image.new("L", (cw, cw), 0)
ImageDraw.Draw(cmask).rounded_rectangle([0, 0, cw - 1, cw - 1], radius=sx(canon_radius), fill=255)
img.paste(cellimg, (int(sx(canon_x0)), int(sx(canon_y0))), cmask)

# --- unify glyph inside the canonical cell: a white "merge into one" Y -------
# Two short strokes that join into one downward stem — reads as "many -> one".
gx, gy = canon_cx, canon_cy
arm = canon_cell * 0.20
gstroke = sx(9)
join = (sx(gx), sx(gy + canon_cell * 0.04))
left_top = (sx(gx - arm), sx(gy - arm))
right_top = (sx(gx + arm), sx(gy - arm))
stem_bot = (sx(gx), sx(gy + arm))
gd = d
for seg in ([left_top, join], [right_top, join], [join, stem_bot]):
    gd.line(seg, fill=(255, 255, 255, 255), width=int(gstroke), joint="curve")
r = gstroke / 2
for px, py in (left_top, right_top, join, stem_bot):
    gd.ellipse([px - r, py - r, px + r, py + r], fill=(255, 255, 255, 255))

# --- downscale --------------------------------------------------------------
out = img.resize((N, N), Image.LANCZOS)
out.save(r"C:\Users\jenny\delightful-compat\src\main\resources\delightfulcompat.png")
print("wrote icon")
