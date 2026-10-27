"""Procedural 16x16 textures for The World That Forgot Itself.

Every texture is generated from a seed and a small description, so the art style stays
consistent (glitch, static, glow on dark) and the whole set can be regenerated.
"""
import math
import random
from PIL import Image

S = 16


def rgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def new(alpha=True):
    return Image.new('RGBA', (S, S), (0, 0, 0, 0) if alpha else (0, 0, 0, 255))


# ---------------------------------------------------------------- blocks

def noise_block(seed, base, accent=None, amount=0.18, accent_rate=0.0):
    r = random.Random(seed)
    img = new(False)
    b = rgb(base)
    a = rgb(accent) if accent else None
    for y in range(S):
        for x in range(S):
            c = shade(b, 1 + (r.random() - 0.5) * amount * 2)
            if a and r.random() < accent_rate:
                c = shade(a, 0.85 + r.random() * 0.3)
            img.putpixel((x, y), c + (255,))
    return img


def ore_block(seed, base, ore):
    img = noise_block(seed, base, amount=0.15)
    r = random.Random(seed + 1)
    o = rgb(ore)
    for _ in range(5):
        cx, cy = r.randrange(2, 14), r.randrange(2, 14)
        for dx, dy in [(0, 0), (1, 0), (0, 1), (-1, 0), (1, 1)]:
            if r.random() < 0.8:
                img.putpixel(((cx + dx) % S, (cy + dy) % S), shade(o, 0.8 + r.random() * 0.4) + (255,))
    return img


def missing(seed, a='#f800f8', b='#000000'):
    img = new(False)
    for y in range(S):
        for x in range(S):
            img.putpixel((x, y), (rgb(a) if ((x // 8) + (y // 8)) % 2 == 0 else rgb(b)) + (255,))
    return img


def glitch_block(seed, base, glitch):
    """A block whose rows have slipped sideways and whose colours bleed."""
    img = noise_block(seed, base, glitch, 0.2, 0.08)
    r = random.Random(seed)
    for _ in range(4):
        y = r.randrange(S)
        shift = r.randrange(1, 6)
        row = [img.getpixel((x, y)) for x in range(S)]
        for x in range(S):
            img.putpixel((x, y), row[(x + shift) % S])
    return img


def static(seed, dark='#101018', light='#d0d0e0'):
    r = random.Random(seed)
    img = new(False)
    d, l = rgb(dark), rgb(light)
    for y in range(S):
        for x in range(S):
            img.putpixel((x, y), mix(d, l, r.random() ** 2) + (255,))
    return img


def circuit(seed, bg='#07120a', trace='#39ff6a'):
    r = random.Random(seed)
    img = new(False)
    b, t = rgb(bg), rgb(trace)
    for y in range(S):
        for x in range(S):
            img.putpixel((x, y), shade(b, 0.9 + r.random() * 0.2) + (255,))
    for _ in range(5):
        x, y = r.randrange(S), r.randrange(S)
        horizontal = r.random() < 0.5
        for i in range(r.randrange(4, 12)):
            img.putpixel((x % S, y % S), shade(t, 0.7 + r.random() * 0.3) + (255,))
            if horizontal:
                x += 1
            else:
                y += 1
            if r.random() < 0.2:
                horizontal = not horizontal
        img.putpixel((x % S, y % S), (255, 255, 255, 255))
    return img


def rune_block(seed, bg, glow):
    """Dark stone with glowing lines: anchors, engines, finality."""
    img = noise_block(seed, bg, amount=0.12)
    r = random.Random(seed)
    g = rgb(glow)
    for i in range(S):
        img.putpixel((i, 0), shade(g, 0.6) + (255,))
        img.putpixel((i, S - 1), shade(g, 0.6) + (255,))
        img.putpixel((0, i), shade(g, 0.6) + (255,))
        img.putpixel((S - 1, i), shade(g, 0.6) + (255,))
    cx = cy = 7.5
    for a in range(0, 360, 12):
        rad = math.radians(a + r.random() * 6)
        for d in (3.2, 5.0):
            x, y = int(cx + math.cos(rad) * d), int(cy + math.sin(rad) * d)
            if r.random() < 0.7:
                img.putpixel((x, y), shade(g, 0.8 + r.random() * 0.3) + (255,))
    img.putpixel((7, 7), (255, 255, 255, 255))
    img.putpixel((8, 8), (255, 255, 255, 255))
    return img


def glass(seed, tint, alpha=110, border=None):
    img = new()
    t = rgb(tint)
    bc = rgb(border) if border else shade(t, 0.7)
    r = random.Random(seed)
    for y in range(S):
        for x in range(S):
            edge = x in (0, S - 1) or y in (0, S - 1)
            if edge:
                img.putpixel((x, y), bc + (230,))
            elif r.random() < 0.06:
                img.putpixel((x, y), (255, 255, 255, 200))
            else:
                img.putpixel((x, y), t + (alpha,))
    return img


def sky(seed):
    r = random.Random(seed)
    img = new(False)
    top, bottom = rgb('#5b8fe8'), rgb('#bcd6ff')
    for y in range(S):
        for x in range(S):
            img.putpixel((x, y), mix(top, bottom, y / (S - 1)) + (255,))
    for _ in range(3):
        cx, cy = r.randrange(S), r.randrange(4, 12)
        for dx in range(-3, 4):
            for dy in range(-1, 2):
                if abs(dx) + abs(dy) * 2 < 4:
                    img.putpixel(((cx + dx) % S, cy + dy), (250, 250, 255, 255))
    return img


def log_side(seed, bark, stripe):
    r = random.Random(seed)
    img = new(False)
    b, s = rgb(bark), rgb(stripe)
    for y in range(S):
        for x in range(S):
            c = b if (x + r.randrange(2)) % 4 else shade(b, 0.75)
            if r.random() < 0.05:
                c = s
            img.putpixel((x, y), shade(c, 0.9 + r.random() * 0.2) + (255,))
    return img


def log_top(seed, bark, core):
    img = new(False)
    b, c = rgb(bark), rgb(core)
    for y in range(S):
        for x in range(S):
            d = max(abs(x - 7.5), abs(y - 7.5))
            col = b if d > 6 else (shade(c, 0.85) if int(d) % 2 else c)
            img.putpixel((x, y), col + (255,))
    return img


def flesh(seed):
    return glitch_block(seed, '#9c3b4a', '#e3a0ae')


# ---------------------------------------------------------------- items

def blank():
    return new()


def outline(img, color='#1a1020'):
    o = rgb(color)
    src = img.copy()
    for y in range(S):
        for x in range(S):
            if src.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < S and 0 <= ny < S and src.getpixel((nx, ny))[3] > 0:
                        img.putpixel((x, y), o + (255,))
                        break
    return img


def fill_shape(img, test, color, seed, shine=True):
    r = random.Random(seed)
    c = rgb(color)
    for y in range(S):
        for x in range(S):
            if test(x, y):
                f = 1.15 - 0.4 * (x + y) / (2 * S) + (r.random() - 0.5) * 0.12
                img.putpixel((x, y), shade(c, f) + (255,))
    if shine:
        for y in range(S):
            for x in range(S):
                if test(x, y) and not test(x - 1, y - 1):
                    img.putpixel((x, y), shade(c, 1.5) + (255,))
    return img


def shard(seed, color):
    img = blank()
    test = lambda x, y: abs(x - 7.5) * 1.6 + abs(y - 7.5) * 0.9 < 7 and 1 < y < 15
    return outline(fill_shape(img, test, color, seed))


def orb(seed, color, glyph=None):
    img = blank()
    test = lambda x, y: (x - 7.5) ** 2 + (y - 7.5) ** 2 < 6.2 ** 2
    fill_shape(img, test, color, seed)
    if glyph is not None:
        r = random.Random(glyph)
        for _ in range(6):
            x, y = r.randrange(5, 11), r.randrange(5, 11)
            img.putpixel((x, y), (255, 255, 255, 255))
    return outline(img)


def book(seed, cover, page='#f2ead2'):
    img = blank()
    fill_shape(img, lambda x, y: 2 <= x <= 13 and 1 <= y <= 14, cover, seed)
    for y in range(2, 14):
        img.putpixel((13, y), rgb(page) + (255,))
    for x in range(4, 12, 2):
        img.putpixel((x, 7), (255, 255, 255, 180))
    return outline(img)


def scroll(seed, paper='#e8dcb0', ink='#6b2fa8'):
    img = blank()
    fill_shape(img, lambda x, y: 3 <= x <= 12 and 2 <= y <= 13, paper, seed, shine=False)
    for x in range(2, 14):
        img.putpixel((x, 2), shade(rgb(paper), 0.7) + (255,))
        img.putpixel((x, 13), shade(rgb(paper), 0.7) + (255,))
    r = random.Random(seed)
    for y in range(4, 12, 2):
        for x in range(5, 11):
            if r.random() < 0.7:
                img.putpixel((x, y), rgb(ink) + (255,))
    return outline(img)


def lens(seed):
    img = blank()
    ring = lambda x, y: 4.0 ** 2 < (x - 8) ** 2 + (y - 6) ** 2 < 5.5 ** 2
    inner = lambda x, y: (x - 8) ** 2 + (y - 6) ** 2 <= 4.0 ** 2
    fill_shape(img, ring, '#9b6bd6', seed)
    for y in range(S):
        for x in range(S):
            if inner(x, y):
                img.putpixel((x, y), (190, 160, 255, 150))
    for i in range(5):
        img.putpixel((3 - i // 2 + 2, 11 + i // 2), rgb('#c8a24a') + (255,))
    img.putpixel((7, 4), (255, 255, 255, 255))
    return outline(img)


def stick_tool(seed, head, handle='#7a5a32', head_shape='gem'):
    img = blank()
    for i in range(10):
        x, y = 3 + i // 1, 13 - i
        if x < S and y >= 0:
            img.putpixel((min(x, 15) - 0, y), rgb(handle) + (255,)) if i < 8 else None
    h = rgb(head)
    if head_shape == 'gem':
        for dx, dy in [(0, 0), (1, 0), (0, -1), (1, -1), (2, -1), (1, -2), (-1, 0), (0, 1)]:
            img.putpixel((11 + dx, 4 + dy), shade(h, 1.0 + 0.1 * dx) + (255,))
        img.putpixel((12, 3), (255, 255, 255, 255))
    elif head_shape == 'needle':
        for i in range(5):
            img.putpixel((11 + i // 2, 5 - i), shade(h, 1.2) + (255,))
    elif head_shape == 'fork':
        for dx, dy in [(0, 0), (2, 0), (0, -1), (2, -1), (0, -2), (2, -2), (1, 1)]:
            img.putpixel((10 + dx, 4 + dy), h + (255,))
    return outline(img)


def sword(seed, blade, hilt='#3a2a48'):
    img = blank()
    for i in range(10):
        img.putpixel((5 + i, 10 - i), shade(rgb(blade), 1.1) + (255,))
        img.putpixel((6 + i, 10 - i), shade(rgb(blade), 0.8) + (255,)) if i < 9 else None
    for i in range(4):
        img.putpixel((2 + i, 11 - i + 2), rgb(hilt) + (255,))
    img.putpixel((3, 10), rgb(hilt) + (255,))
    img.putpixel((6, 13), rgb(hilt) + (255,))
    return outline(img)


def chestplate(seed, color):
    img = blank()
    test = lambda x, y: (2 <= x <= 13 and 3 <= y <= 13 and not (5 <= x <= 10 and y <= 4)) and not (y >= 9 and (x <= 3 or x >= 12))
    return outline(fill_shape(img, test, color, seed))


def crown(seed, color='#e6c34a', gem='#7d3bd6'):
    img = blank()
    test = lambda x, y: 9 <= y <= 12 and 2 <= x <= 13 or (y >= 5 and y <= 9 and x in (2, 3, 7, 8, 12, 13))
    fill_shape(img, test, color, seed)
    for x in (3, 8, 12):
        img.putpixel((x, 10), rgb(gem) + (255,))
    return outline(img)


def key(seed, color):
    img = blank()
    ring = lambda x, y: 2.0 ** 2 < (x - 5) ** 2 + (y - 5) ** 2 < 3.6 ** 2
    fill_shape(img, ring, color, seed)
    for i in range(7):
        img.putpixel((7 + i, 7 + i), rgb(color) + (255,))
    img.putpixel((11, 13), rgb(color) + (255,))
    img.putpixel((13, 11), rgb(color) + (255,))
    return outline(img)


def seed_item(seed, color):
    img = blank()
    test = lambda x, y: ((x - 8) / 3.0) ** 2 + ((y - 9) / 4.5) ** 2 < 1
    fill_shape(img, test, color, seed)
    for y in range(2, 6):
        img.putpixel((8, y), rgb('#5fbf3a') + (255,))
    img.putpixel((9, 3), rgb('#5fbf3a') + (255,))
    return outline(img)


def cartridge(seed, color, label='#f0f0f0'):
    img = blank()
    fill_shape(img, lambda x, y: 2 <= x <= 13 and 3 <= y <= 13, color, seed, shine=False)
    for x in range(4, 12):
        for y in range(5, 9):
            img.putpixel((x, y), rgb(label) + (255,))
    for x in range(4, 12, 2):
        img.putpixel((x, 12), rgb('#d4b03a') + (255,))
    return outline(img)


def chip(seed, color='#2b2f3a'):
    img = blank()
    fill_shape(img, lambda x, y: 3 <= x <= 12 and 3 <= y <= 12, color, seed, shine=False)
    r = random.Random(seed)
    for _ in range(10):
        x, y = r.randrange(4, 12), r.randrange(4, 12)
        img.putpixel((x, y), rgb('#5ad1ff') + (255,))
    for i in range(4, 12, 2):
        for p in [(i, 2), (i, 13), (2, i), (13, i)]:
            img.putpixel(p, rgb('#c0c0c0') + (255,))
    return img


def bottle(seed, liquid):
    img = blank()
    body = lambda x, y: ((x - 7.5) / 4.5) ** 2 + ((y - 10) / 4.2) ** 2 < 1
    neck = lambda x, y: 6 <= x <= 9 and 2 <= y <= 6
    fill_shape(img, lambda x, y: body(x, y) or neck(x, y), '#dfe8ef', seed)
    for y in range(S):
        for x in range(S):
            if body(x, y) and y >= 8:
                img.putpixel((x, y), shade(rgb(liquid), 0.9 + 0.2 * random.Random(x * 31 + y).random()) + (255,))
    return outline(img)


def egg(seed, base, spots):
    img = blank()
    test = lambda x, y: ((x - 7.5) / 5.0) ** 2 + ((y - 8.5) / 6.5) ** 2 < 1
    fill_shape(img, test, base, seed)
    r = random.Random(seed)
    for _ in range(7):
        x, y = r.randrange(4, 12), r.randrange(4, 14)
        if test(x, y):
            img.putpixel((x, y), rgb(spots) + (255,))
    return outline(img)


def glitch_item(seed):
    img = missing(seed)
    img = img.resize((8, 8), Image.NEAREST).resize((S, S), Image.NEAREST)
    r = random.Random(seed)
    for _ in range(14):
        img.putpixel((r.randrange(S), r.randrange(S)), (r.randrange(256), 0, r.randrange(256), 255))
    return img


def block_icon(seed, color):
    img = blank()
    fill_shape(img, lambda x, y: 3 <= x <= 12 and 3 <= y <= 12, color, seed)
    return outline(img)


def door(seed, frame='#d8d8e8', inside='#120a1e'):
    img = blank()
    fill_shape(img, lambda x, y: 3 <= x <= 12 and 1 <= y <= 14, frame, seed, shine=False)
    for y in range(3, 14):
        for x in range(5, 11):
            img.putpixel((x, y), rgb(inside) + (255,))
    img.putpixel((9, 8), (255, 220, 120, 255))
    return outline(img)
