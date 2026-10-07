#!/usr/bin/env python3
"""
Quacky Surfer asset generator.
Builds low-poly glTF 2.0 binary (.glb) models with node animations (no skins), using only numpy.
Run:  python3 make_assets.py <out_dir>
Conventions: meters, +Y up, the duck faces -Z (away from the camera), trains/props face +Z (toward the player).
"""
import json, math, os, random, struct, sys
import numpy as np

# ---------------------------------------------------------------- math helpers
def hex2rgb(h):
    h = h.lstrip('#')
    return [int(h[i:i + 2], 16) / 255.0 for i in (0, 2, 4)]

def srgb2lin(c):
    return [(x / 12.92 if x <= 0.04045 else ((x + 0.055) / 1.055) ** 2.4) for x in c]

def q_axis(axis, deg):
    a = math.radians(deg) / 2
    ax = np.array(axis, float); ax /= np.linalg.norm(ax)
    return np.array([*(ax * math.sin(a)), math.cos(a)])

def q_mul(a, b):
    x1, y1, z1, w1 = a; x2, y2, z2, w2 = b
    return np.array([w1 * x2 + x1 * w2 + y1 * z2 - z1 * y2,
                     w1 * y2 - x1 * z2 + y1 * w2 + z1 * x2,
                     w1 * z2 + x1 * y2 - y1 * x2 + z1 * w2,
                     w1 * w2 - x1 * x2 - y1 * y2 - z1 * z2])

def q_euler(x=0, y=0, z=0):
    """Rotation: first X, then Y, then Z applied as q = qz * qy * qx (degrees)."""
    return q_mul(q_axis((0, 0, 1), z), q_mul(q_axis((0, 1, 0), y), q_axis((1, 0, 0), x)))

def q_slerp(a, b, t):
    a = np.array(a, float); b = np.array(b, float)
    d = float(np.dot(a, b))
    if d < 0: b = -b; d = -d
    if d > 0.9995:
        r = a + t * (b - a); return r / np.linalg.norm(r)
    th = math.acos(d); s = math.sin(th)
    return (math.sin((1 - t) * th) * a + math.sin(t * th) * b) / s

def q_mat(q):
    x, y, z, w = q
    return np.array([[1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
                     [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
                     [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)]])

def trs_mat(t, q, s):
    m = np.eye(4); m[:3, :3] = q_mat(q) @ np.diag(s); m[:3, 3] = t; return m

# ---------------------------------------------------------------- mesh primitives
def _fix(P, N, I):
    P = np.array(P, float); N = np.array(N, float); out = []
    for a, b, c in I:
        n = np.cross(P[b] - P[a], P[c] - P[a])
        if np.linalg.norm(n) < 1e-10: continue
        if np.dot(n, N[a] + N[b] + N[c]) < 0: b, c = c, b
        out.append((a, b, c))
    return P, N, np.array(out, dtype=np.int64)

def ellipsoid(rx, ry, rz, lat=10, lon=18, theta_max=math.pi):
    P = []; N = []
    for i in range(lat + 1):
        th = theta_max * i / lat
        for j in range(lon + 1):
            ph = 2 * math.pi * j / lon
            x = math.sin(th) * math.cos(ph); y = math.cos(th); z = math.sin(th) * math.sin(ph)
            P.append((rx * x, ry * y, rz * z))
            n = np.array([x / rx, y / ry, z / rz]); N.append(n / np.linalg.norm(n))
    I = []
    for i in range(lat):
        for j in range(lon):
            a = i * (lon + 1) + j; b = a + lon + 1
            I += [(a, b, a + 1), (a + 1, b, b + 1)]
    return _fix(P, N, I)

def frustum(r0, r1, h, seg=16, caps=True):
    """Along Y, centered on origin. r0 at -h/2, r1 at +h/2."""
    P = []; N = []; I = []
    slope = (r0 - r1) / h
    for k, (r, y) in enumerate(((r0, -h / 2), (r1, h / 2))):
        for j in range(seg + 1):
            ph = 2 * math.pi * j / seg
            P.append((r * math.cos(ph), y, r * math.sin(ph)))
            n = np.array([math.cos(ph), slope, math.sin(ph)]); N.append(n / np.linalg.norm(n))
    for j in range(seg):
        a = j; b = j + seg + 1
        I += [(a, b, a + 1), (a + 1, b, b + 1)]
    if caps:
        for sign, r, y in ((-1, r0, -h / 2), (1, r1, h / 2)):
            if r <= 1e-9: continue
            c = len(P); P.append((0, y, 0)); N.append((0, sign, 0))
            for j in range(seg + 1):
                ph = 2 * math.pi * j / seg
                P.append((r * math.cos(ph), y, r * math.sin(ph))); N.append((0, sign, 0))
            for j in range(seg):
                I.append((c, c + 1 + j, c + 2 + j))
    return _fix(P, N, I)

def box(sx, sy, sz):
    hx, hy, hz = sx / 2, sy / 2, sz / 2
    faces = [((1, 0, 0), [(hx, -hy, -hz), (hx, hy, -hz), (hx, hy, hz), (hx, -hy, hz)]),
             ((-1, 0, 0), [(-hx, -hy, hz), (-hx, hy, hz), (-hx, hy, -hz), (-hx, -hy, -hz)]),
             ((0, 1, 0), [(-hx, hy, -hz), (-hx, hy, hz), (hx, hy, hz), (hx, hy, -hz)]),
             ((0, -1, 0), [(-hx, -hy, hz), (-hx, -hy, -hz), (hx, -hy, -hz), (hx, -hy, hz)]),
             ((0, 0, 1), [(-hx, -hy, hz), (hx, -hy, hz), (hx, hy, hz), (-hx, hy, hz)]),
             ((0, 0, -1), [(hx, -hy, -hz), (-hx, -hy, -hz), (-hx, hy, -hz), (hx, hy, -hz)])]
    P = []; N = []; I = []
    for n, vs in faces:
        b = len(P); P += vs; N += [n] * 4; I += [(b, b + 1, b + 2), (b, b + 2, b + 3)]
    return _fix(P, N, I)

def prism(poly, depth):
    """Extrude a convex 2D polygon (XY) along Z, centered. Flat shaded."""
    poly = [tuple(p) for p in poly]; n = len(poly); hz = depth / 2
    P = []; N = []; I = []
    for z, nz in ((hz, 1), (-hz, -1)):
        b = len(P)
        for x, y in poly: P.append((x, y, z)); N.append((0, 0, nz))
        for k in range(1, n - 1): I.append((b, b + k, b + k + 1))
    for k in range(n):
        x0, y0 = poly[k]; x1, y1 = poly[(k + 1) % n]
        e = np.array([x1 - x0, y1 - y0]); nn = np.array([e[1], -e[0], 0.0])
        if np.linalg.norm(nn) < 1e-12: continue
        nn /= np.linalg.norm(nn); b = len(P)
        P += [(x0, y0, hz), (x1, y1, hz), (x1, y1, -hz), (x0, y0, -hz)]
        N += [tuple(nn)] * 4; I += [(b, b + 1, b + 2), (b, b + 2, b + 3)]
    # normals of sides could point inward for CW polygons; _fix handles winding, so make polygon CCW first
    return _fix(P, N, I)

def clip_poly_rect(poly, x0, y0, x1, y1):
    def clip(pts, inside, inter):
        out = []
        for i in range(len(pts)):
            a = pts[i]; b = pts[(i + 1) % len(pts)]
            ia, ib = inside(a), inside(b)
            if ia and ib: out.append(b)
            elif ia and not ib: out.append(inter(a, b))
            elif not ia and ib: out.append(inter(a, b)); out.append(b)
        return out
    def ix(x):
        return lambda a, b: (x, a[1] + (b[1] - a[1]) * (x - a[0]) / (b[0] - a[0]))
    def iy(y):
        return lambda a, b: (a[0] + (b[0] - a[0]) * (y - a[1]) / (b[1] - a[1]), y)
    pts = list(poly)
    for ins, it in ((lambda p: p[0] >= x0, ix(x0)), (lambda p: p[0] <= x1, ix(x1)),
                    (lambda p: p[1] >= y0, iy(y0)), (lambda p: p[1] <= y1, iy(y1))):
        if not pts: break
        pts = clip(pts, ins, it)
    return pts

def tf(mesh, t=(0, 0, 0), rot=(0, 0, 0), s=(1, 1, 1)):
    P, N, I = mesh
    M = trs_mat(t, q_euler(*rot), s); L = M[:3, :3]
    P2 = P @ L.T + M[:3, 3]
    N2 = N @ np.linalg.inv(L); N2 /= np.linalg.norm(N2, axis=1, keepdims=True)
    I2 = I.copy()
    if np.linalg.det(L) < 0: I2 = I2[:, [0, 2, 1]]
    return P2, N2, I2

def merge(meshes):
    Ps, Ns, Is = [], [], []; off = 0
    for P, N, I in meshes:
        Ps.append(P); Ns.append(N); Is.append(I + off); off += len(P)
    return np.vstack(Ps), np.vstack(Ns), np.vstack(Is)

def shift(mesh, d):  # translate by d
    return tf(mesh, t=d)

# ---------------------------------------------------------------- materials
MATS = {}
def mat(name, color, metallic=0.0, rough=0.7, emissive=None, emissive_k=1.0, double=False, alpha=1.0):
    MATS[name] = dict(color=color, metallic=min(metallic, 0.25), rough=rough, emissive=emissive, ek=emissive_k, double=double, alpha=alpha)
    return name

M_WHITE = mat('duck_white', '#F4F4F2', 0.0, 0.75)
M_ORANGE = mat('duck_orange', '#FF9A1F', 0.0, 0.6)
M_HAT = mat('hardhat_orange', '#FF7A12', 0.1, 0.45)
M_BAND = mat('headband_cyan', '#22C4F2', 0.0, 0.6)
M_BLACK = mat('eye_black', '#0F0F12', 0.0, 0.3)
M_GLINT = mat('eye_glint', '#FFFFFF', 0.0, 0.2, emissive='#FFFFFF', emissive_k=0.6)
M_LAMP = mat('headlamp_yellow', '#FFD400', 0.0, 0.3, emissive='#FFD400', emissive_k=0.9)
M_GOLD = mat('breadcrumb_gold', '#FFB800', 0.9, 0.28, emissive='#FF9F00', emissive_k=0.45)
M_GOLD2 = mat('breadcrumb_gold_dark', '#C98600', 0.9, 0.35)
M_TRAIN = mat('train_body', '#2A2E38', 0.3, 0.55)
M_TRAIN2 = mat('train_roof', '#3A3F4C', 0.3, 0.5)
M_TRAIN_DARK = mat('train_under', '#14151A', 0.2, 0.8)
M_WINDOW = mat('train_window', '#2F6BFF', 0.1, 0.25, emissive='#2F6BFF', emissive_k=0.35)
M_YELLOW = mat('hazard_yellow', '#FFC800', 0.0, 0.55)
M_HBLACK = mat('hazard_black', '#15161A', 0.0, 0.7)
M_HEAD = mat('headlight_white', '#FFFFFF', 0.0, 0.2, emissive='#FFF6D6', emissive_k=0.9)
M_METAL = mat('duct_metal', '#585D69', 0.6, 0.4)
M_POST = mat('post_gray', '#363A43', 0.4, 0.55)
M_RED = mat('cap_red', '#D32F2F', 0.1, 0.5)
M_BEACON = mat('beacon_orange', '#FF8A00', 0.0, 0.3, emissive='#FF8A00', emissive_k=0.8)
M_FLOOR = mat('floor_dark', '#17181C', 0.1, 0.85)
M_LINE = mat('lane_line', '#34373F', 0.0, 0.8)
M_SLEEPER = mat('floor_tile', '#1E2026', 0.0, 0.85)
M_WALL = mat('wall_dark', '#22242A', 0.1, 0.7)
M_STRIP = mat('wall_strip', '#FFC83A', 0.0, 0.5, emissive='#FFC83A', emissive_k=0.55)
M_PILLAR = mat('pillar_dark', '#1A1C21', 0.2, 0.7)
M_LAMPW = mat('lamp_warm', '#FFE9A8', 0.0, 0.4, emissive='#FFE9A8', emissive_k=0.9)
M_VOID = mat('void_ground', '#0C0C0E', 0.0, 0.95)
M_BLD1 = mat('building_a', '#13151A', 0.1, 0.8)
M_BLD2 = mat('building_b', '#1A1D23', 0.1, 0.8)
M_BLD3 = mat('building_c', '#101216', 0.1, 0.8)
M_WIN1 = mat('window_warm', '#FFD25A', 0.0, 0.5, emissive='#FFD25A', emissive_k=0.7)
M_BLOB = mat('blob_shadow', '#000000', 0.0, 1.0, alpha=0.42)
M_WIN2 = mat('window_cool', '#7FB6FF', 0.0, 0.5, emissive='#7FB6FF', emissive_k=0.6)

# ---------------------------------------------------------------- model/scene container
class Node:
    def __init__(self, name, t=(0, 0, 0), q=(0, 0, 0, 1), s=(1, 1, 1), prims=None, parent=None):
        self.name = name; self.t = np.array(t, float); self.q = np.array(q, float); self.s = np.array(s, float)
        self.prims = prims or []   # list of (mesh, material_name)
        self.children = []; self.parent = parent
        if parent: parent.children.append(self)

    def walk(self):
        yield self
        for c in self.children: yield from c.walk()

class Model:
    def __init__(self, roots):
        self.roots = roots; self.anims = []   # anims: dict(name, loop, tracks={node_name:{'t':[], 'tr':[...]|None, 'rot':[...]|None, 'sc':...}})
    def nodes(self):
        for r in self.roots: yield from r.walk()
    def by_name(self):
        return {n.name: n for n in self.nodes()}

def part(parent, name, pivot_world, parent_pivot_world, prims_world):
    """Create a child node whose origin is pivot_world; prims given in world (model) coordinates."""
    pw = np.array(pivot_world, float)
    prims = [(shift(m, -pw), mt) for m, mt in prims_world]
    return Node(name, t=pw - np.array(parent_pivot_world, float), prims=prims, parent=parent)

# ---------------------------------------------------------------- DUCK
def build_duck():
    root = Node('Duck')
    P_lean = (0, 0.20, 0)
    lean = Node('LeanPivot', t=P_lean, parent=root)
    P_body = (0, 0.40, 0)
    body_prims = [
        (shift(ellipsoid(0.30, 0.26, 0.35, 12, 20), (0, 0.40, 0.0)), M_WHITE),
    ]
    body = part(lean, 'Body', P_body, P_lean, body_prims)
    # Head
    P_head = (0, 0.62, -0.10)
    hc = (0, 0.80, -0.12)  # head center
    head_prims = [
        (shift(ellipsoid(0.205, 0.20, 0.20, 12, 20), hc), M_WHITE),
        # beak
        (shift(ellipsoid(0.095, 0.042, 0.115, 8, 14), (0, hc[1] - 0.045, hc[2] - 0.21)), M_ORANGE),
        # eyes + glints
        (shift(ellipsoid(0.034, 0.040, 0.030, 8, 12), (0.105, hc[1] + 0.035, hc[2] - 0.15)), M_BLACK),
        (shift(ellipsoid(0.034, 0.040, 0.030, 8, 12), (-0.105, hc[1] + 0.035, hc[2] - 0.15)), M_BLACK),
        (shift(ellipsoid(0.011, 0.011, 0.011, 6, 8), (0.112, hc[1] + 0.052, hc[2] - 0.175)), M_GLINT),
        (shift(ellipsoid(0.011, 0.011, 0.011, 6, 8), (-0.098, hc[1] + 0.052, hc[2] - 0.175)), M_GLINT),
        # hard hat dome + brim + ridge
        (shift(ellipsoid(0.222, 0.15, 0.222, 8, 20, theta_max=math.pi / 2), (0, hc[1] + 0.085, hc[2] + 0.005)), M_HAT),
        (shift(frustum(0.245, 0.245, 0.02, 24), (0, hc[1] + 0.085, hc[2] - 0.01)), M_HAT),
        (shift(box(0.05, 0.03, 0.32), (0, hc[1] + 0.245, hc[2])), M_HAT),
        # headlamp on the hat front
        (shift(ellipsoid(0.04, 0.04, 0.03, 8, 12), (0, hc[1] + 0.13, hc[2] - 0.225)), M_LAMP),
        # cyan headband ring just under the hat
        (shift(frustum(0.213, 0.213, 0.05, 24, caps=False), (0, hc[1] + 0.07, hc[2])), M_BAND),
    ]
    head = part(body, 'Head', P_head, P_body, head_prims)
    # headband tails (trail behind = +Z)
    for side, nm in ((1, 'Band_Tail_R'), (-1, 'Band_Tail_L')):
        piv = (side * 0.07, hc[1] + 0.07, hc[2] + 0.2)
        tail_m = shift(box(0.05, 0.012, 0.26), (piv[0], piv[1], piv[2] + 0.13))
        tail_m = tf(tail_m, t=(0, 0, 0))
        part(head, nm, piv, P_head, [(tail_m, M_BAND)])
    # wings
    for side, nm in ((1, 'Wing_R'), (-1, 'Wing_L')):
        piv = (side * 0.27, 0.48, -0.03)
        wing = ellipsoid(0.055, 0.17, 0.21, 8, 14)
        wing = tf(wing, t=(side * 0.04, -0.12, 0.02), rot=(0, 0, side * -12))
        wing = shift(wing, piv)
        part(body, nm, piv, P_body, [(wing, M_WHITE)])
    # tail feathers
    piv = (0, 0.42, 0.30)
    tail = tf(ellipsoid(0.10, 0.07, 0.15, 8, 14), t=(0, 0.04, 0.12), rot=(-20, 0, 0))
    part(body, 'Tail', piv, P_body, [(shift(tail, piv), M_WHITE)])
    # feet (hip pivot; leg + foot hang below)
    for side, nm in ((1, 'Foot_R'), (-1, 'Foot_L')):
        piv = (side * 0.12, 0.17, 0.0)
        leg = shift(frustum(0.028, 0.028, 0.13, 10), (side * 0.12, 0.105, 0.0))
        foot = shift(ellipsoid(0.085, 0.03, 0.13, 8, 14), (side * 0.12, 0.03, -0.06))
        part(lean, nm, piv, P_lean, [(leg, M_ORANGE), (foot, M_ORANGE)])
    return Model([root])

def kf(times, vals):
    return dict(t=times, v=vals)

def build_duck_anims(model):
    D = {}   # name -> (loop, tracks)
    def tr(y=0, x=0, z=0): return np.array([x, y, z], float)
    base = {n.name: (n.t.copy(), n.q.copy()) for n in model.nodes()}
    def T(name, dx=0, dy=0, dz=0):
        return base[name][0] + np.array([dx, dy, dz])
    def R(name, x=0, y=0, z=0):
        return q_mul(q_euler(x, y, z), base[name][1])
    anims = {}

    # ---- run (0.5 s loop)
    ts = [0, 0.125, 0.25, 0.375, 0.5]
    a = {}
    a['Foot_L'] = dict(t=ts, rot=[R('Foot_L', x=v) for v in (30, 0, -30, 0, 30)],
                       tr=[T('Foot_L', dy=v) for v in (0.03, 0.02, 0.03, 0.07, 0.03)])
    a['Foot_R'] = dict(t=ts, rot=[R('Foot_R', x=v) for v in (-30, 0, 30, 0, -30)],
                       tr=[T('Foot_R', dy=v) for v in (0.03, 0.07, 0.03, 0.02, 0.03)])
    a['Body'] = dict(t=ts, tr=[T('Body', dy=v) for v in (0, 0.035, 0, 0.035, 0)],
                     rot=[R('Body', x=v) for v in (-5, -7, -5, -7, -5)])
    a['Head'] = dict(t=ts, rot=[R('Head', x=v) for v in (3, 5, 3, 5, 3)])
    a['Wing_R'] = dict(t=ts, rot=[R('Wing_R', z=v, x=15) for v in (14, 26, 14, 26, 14)])
    a['Wing_L'] = dict(t=ts, rot=[R('Wing_L', z=v, x=15) for v in (-14, -26, -14, -26, -14)])
    a['Tail'] = dict(t=ts, rot=[R('Tail', x=v) for v in (0, 8, 0, 8, 0)])
    a['Band_Tail_R'] = dict(t=ts, rot=[R('Band_Tail_R', y=v, x=-8) for v in (0, 14, 0, -14, 0)])
    a['Band_Tail_L'] = dict(t=ts, rot=[R('Band_Tail_L', y=v, x=-8) for v in (0, -14, 0, 14, 0)])
    anims['run'] = (True, a)

    # ---- idle (1.6 s loop)
    ts = [0, 0.4, 0.8, 1.2, 1.6]
    a = {}
    a['Body'] = dict(t=ts, tr=[T('Body', dy=v) for v in (0, 0.008, 0.014, 0.008, 0)])
    a['Head'] = dict(t=ts, rot=[R('Head', y=v) for v in (0, 10, 0, -10, 0)])
    a['Wing_R'] = dict(t=ts, rot=[R('Wing_R', z=v) for v in (4, 6, 4, 6, 4)])
    a['Wing_L'] = dict(t=ts, rot=[R('Wing_L', z=v) for v in (-4, -6, -4, -6, -4)])
    a['Band_Tail_R'] = dict(t=ts, rot=[R('Band_Tail_R', y=v) for v in (0, 10, 0, -10, 0)])
    a['Band_Tail_L'] = dict(t=ts, rot=[R('Band_Tail_L', y=v) for v in (0, -10, 0, 10, 0)])
    anims['idle'] = (True, a)

    # ---- jump (0.7 s, one-shot; hold last pose)
    ts = [0, 0.08, 0.18, 0.35, 0.55, 0.7]
    a = {}
    a['Body'] = dict(t=ts, tr=[T('Body', dy=v) for v in (0, -0.07, 0, 0, 0, -0.04)],
                     rot=[R('Body', x=v) for v in (-5, 8, 12, 0, -12, -6)])
    a['Foot_L'] = dict(t=ts, rot=[R('Foot_L', x=v) for v in (30, 12, -35, -30, 25, 15)])
    a['Foot_R'] = dict(t=ts, rot=[R('Foot_R', x=v) for v in (-30, 12, -35, -30, 25, 15)])
    a['Wing_R'] = dict(t=ts, rot=[R('Wing_R', z=v) for v in (14, 5, 60, 72, 40, 25)])
    a['Wing_L'] = dict(t=ts, rot=[R('Wing_L', z=v) for v in (-14, -5, -60, -72, -40, -25)])
    a['Head'] = dict(t=ts, rot=[R('Head', x=v) for v in (3, -6, -10, -4, 6, 4)])
    a['Band_Tail_R'] = dict(t=ts, rot=[R('Band_Tail_R', x=v) for v in (0, 6, -20, -26, -10, 0)])
    a['Band_Tail_L'] = dict(t=ts, rot=[R('Band_Tail_L', x=v) for v in (0, 6, -20, -26, -10, 0)])
    anims['jump'] = (False, a)

    # ---- slide (0.7 s, hold the low pose in the middle)
    ts = [0, 0.1, 0.2, 0.5, 0.7]
    a = {}
    a['Body'] = dict(t=ts, tr=[T('Body', dy=v) for v in (0, -0.09, -0.15, -0.15, -0.03)],
                     rot=[R('Body', x=v) for v in (-5, -10, -10, -10, -5)])
    a['Head'] = dict(t=ts, tr=[T('Head', dy=dy, dz=dz) for dy, dz in ((0, 0), (-0.13, -0.06), (-0.25, -0.14), (-0.25, -0.14), (-0.04, -0.02))],
                     rot=[R('Head', x=v) for v in (3, -15, -28, -28, 0)])
    a['Foot_L'] = dict(t=ts, rot=[R('Foot_L', x=v) for v in (0, -30, -50, -50, 0)],
                     tr=[T('Foot_L', dy=v) for v in (0, 0.02, 0.05, 0.05, 0)])
    a['Foot_R'] = dict(t=ts, rot=[R('Foot_R', x=v) for v in (0, -30, -50, -50, 0)],
                     tr=[T('Foot_R', dy=v) for v in (0, 0.02, 0.05, 0.05, 0)])
    a['Wing_R'] = dict(t=ts, rot=[R('Wing_R', z=v, x=x) for v, x in ((14, 15), (8, 35), (4, 55), (4, 55), (14, 15))])
    a['Wing_L'] = dict(t=ts, rot=[R('Wing_L', z=v, x=x) for v, x in ((-14, 15), (-8, 35), (-4, 55), (-4, 55), (-14, 15))])
    a['Band_Tail_R'] = dict(t=ts, rot=[R('Band_Tail_R', x=v) for v in (0, -10, -20, -20, 0)])
    a['Band_Tail_L'] = dict(t=ts, rot=[R('Band_Tail_L', x=v) for v in (0, -10, -20, -20, 0)])
    anims['slide'] = (False, a)

    # ---- lean_left / lean_right (0.3 s, one-shot, overlay on LeanPivot only)
    for nm, sgn in (('lean_left', 1), ('lean_right', -1)):
        ts = [0, 0.1, 0.3]
        anims[nm] = (False, {'LeanPivot': dict(t=ts, rot=[R('LeanPivot', z=v * sgn) for v in (0, 14, 0)])})

    # ---- crash (0.9 s, one-shot, hold last pose = lying on its back)
    ts = [0, 0.15, 0.35, 0.6, 0.75, 0.9]
    a = {}
    a['Duck'] = dict(t=ts, rot=[R('Duck', x=v) for v in (0, -25, 40, 95, 82, 90)],
                     tr=[T('Duck', dy=v) for v in (0, 0.10, 0.80, 0.50, 0.58, 0.45)])
    a['Wing_R'] = dict(t=ts, rot=[R('Wing_R', z=v) for v in (14, 40, 75, 70, 70, 70)])
    a['Wing_L'] = dict(t=ts, rot=[R('Wing_L', z=v) for v in (-14, -40, -75, -70, -70, -70)])
    a['Foot_L'] = dict(t=ts, rot=[R('Foot_L', x=v) for v in (0, 10, -20, 20, 10, 15)])
    a['Foot_R'] = dict(t=ts, rot=[R('Foot_R', x=v) for v in (0, 10, -20, -10, 5, -15)])
    a['Head'] = dict(t=ts, rot=[R('Head', x=v, z=zz) for v, zz in ((3, 0), (-10, 4), (-20, -6), (10, 8), (4, -5), (6, 6))])
    anims['crash'] = (False, a)

    # ---- rest (single key, restores every animated node to bind pose)
    animated = set()
    for loop, a in anims.values(): animated |= set(a.keys())
    rest = {}
    for nm in sorted(animated):
        rest[nm] = dict(t=[0.0], tr=[base[nm][0]], rot=[base[nm][1]])
    anims['rest'] = (False, rest)
    return anims

# ---------------------------------------------------------------- PROPS
def build_train(length=11.0):
    root = Node('Train')
    # origin = front-bottom-center; body extends toward -Z
    W, H0, H1 = 1.4, 0.38, 2.7
    body = box(W, H1 - H0, length)
    body = shift(body, (0, (H0 + H1) / 2, -length / 2))
    roof = shift(box(W * 0.78, 0.16, length - 0.4), (0, H1 + 0.06, -length / 2))
    under = shift(box(W * 0.9, H0, length - 0.2), (0, H0 / 2, -length / 2))
    prims = [(body, M_TRAIN), (roof, M_TRAIN2), (under, M_TRAIN_DARK)]
    # yellow stripe wrapped on front + both sides
    sy0, sy1 = 0.78, 1.04
    prims.append((shift(box(W + 0.02, sy1 - sy0, 0.02), (0, (sy0 + sy1) / 2, 0.006)), M_YELLOW))
    for sx in (-1, 1):
        prims.append((shift(box(0.02, sy1 - sy0, length - 0.02), (sx * (W / 2 + 0.006), (sy0 + sy1) / 2, -length / 2)), M_YELLOW))
    # front windshield (two panes) + headlights
    for sx in (-1, 1):
        prims.append((shift(box(0.52, 0.78, 0.02), (sx * 0.30, 1.9, 0.006)), M_WINDOW))
        prims.append((shift(ellipsoid(0.07, 0.07, 0.03, 8, 12), (sx * 0.46, 0.58, 0.01)), M_HEAD))
    # side windows
    for sx in (-1, 1):
        for k in range(int((length - 1.6) // 1.9)):
            z = -1.2 - k * 1.9
            prims.append((shift(box(0.02, 0.62, 1.1), (sx * (W / 2 + 0.006), 1.9, z)), M_WINDOW))
    # wheels hint
    for sx in (-1, 1):
        for z in (-1.4, -length + 1.4):
            prims.append((tf(frustum(0.30, 0.30, 0.12, 14), t=(sx * (W / 2 - 0.02), 0.30, z), rot=(0, 0, 90)), M_TRAIN_DARK))
    Node('TrainBody', prims=prims, parent=root)
    return Model([root])

def stripes_on_board(w, h, z, stripe=0.16, gap=0.16):
    """Diagonal hazard stripes (list of prism meshes + material) on a w x h board centered at origin, front face at z."""
    out = []
    x0, x1, y0, y1 = -w / 2, w / 2, -h / 2, h / 2
    k = 0; start = -h
    pos = x0 - h
    idx = 0
    while pos < x1 + h:
        poly = [(pos, y0), (pos + stripe, y0), (pos + stripe + h, y1), (pos + h, y1)]
        clipped = clip_poly_rect(poly, x0, y0, x1, y1) if idx % 2 == 0 else []
        if len(clipped) >= 3:
            # ensure CCW
            area = sum(clipped[i][0] * clipped[(i + 1) % len(clipped)][1] - clipped[(i + 1) % len(clipped)][0] * clipped[i][1] for i in range(len(clipped)))
            if area < 0: clipped = clipped[::-1]
            out.append((prism(clipped, 0.012), M_HBLACK))
        pos += (stripe + gap) / 2; idx += 1
    return out

def build_barrier():
    root = Node('Barrier')
    prims = []
    for sx in (-1, 1):
        prims.append((tf(box(0.07, 0.5, 0.07), t=(sx * 0.6, 0.25, -0.05), rot=(18, 0, 0)), M_POST))
        prims.append((tf(box(0.07, 0.5, 0.07), t=(sx * 0.6, 0.25, 0.05), rot=(-18, 0, 0)), M_POST))
        prims.append((shift(ellipsoid(0.06, 0.07, 0.06, 8, 10), (sx * 0.6, 0.82, 0.0)), M_BEACON))
        prims.append((shift(box(0.04, 0.12, 0.04), (sx * 0.6, 0.74, 0.0)), M_POST))
    board_w, board_h = 1.56, 0.34
    prims.append((shift(box(board_w, board_h, 0.06), (0, 0.55, 0.0)), M_YELLOW))
    for m, mt in stripes_on_board(board_w, board_h, 0.03):
        prims.append((shift(m, (0, 0.55, 0.035)), mt))
    Node('BarrierBody', prims=prims, parent=root)
    return Model([root])

def build_duct():
    root = Node('Duct')
    prims = []
    for sx in (-1, 1):
        prims.append((shift(box(0.12, 1.74, 0.12), (sx * 0.86, 0.87, 0.0)), M_POST))
    prims.append((shift(box(1.84, 0.50, 0.50), (0, 1.07, 0.0)), M_METAL))
    prims.append((shift(box(1.90, 0.07, 0.56), (0, 1.355, 0.0)), M_RED))
    # hazard band along the lower edge (front face)
    band_w, band_h = 1.84, 0.09
    prims.append((shift(box(band_w, band_h, 0.012), (0, 0.865, 0.256)), M_YELLOW))
    for m, mt in stripes_on_board(band_w, band_h, 0.0, stripe=0.09, gap=0.09):
        prims.append((shift(m, (0, 0.865, 0.262)), mt))
    # sign plate with black down chevron
    prims.append((shift(box(0.62, 0.30, 0.012), (0, 1.14, 0.256)), M_YELLOW))
    chev = [(-0.22, 0.07), (-0.14, 0.07), (0.0, -0.05), (0.14, 0.07), (0.22, 0.07), (0.0, -0.14)]
    # chevron as two convex quads
    q1 = [(-0.22, 0.07), (-0.14, 0.07), (0.0, -0.05), (0.0, -0.14)]
    q2 = [(0.0, -0.14), (0.0, -0.05), (0.14, 0.07), (0.22, 0.07)]
    for q in (q1, q2):
        area = sum(q[i][0] * q[(i + 1) % 4][1] - q[(i + 1) % 4][0] * q[i][1] for i in range(4))
        if area < 0: q = q[::-1]
        prims.append((shift(prism(q, 0.012), (0, 1.14, 0.265)), M_HBLACK))
    Node('DuctBody', prims=prims, parent=root)
    return Model([root])

def build_coin():
    root = Node('Breadcrumb')
    spin = Node('CoinSpin', parent=root, t=(0, 0.0, 0))
    prims = []
    # upright coin, face toward +Z/-Z, centered at origin; game places root at y = 0.7
    prims.append((tf(frustum(0.26, 0.26, 0.07, 24), rot=(90, 0, 0)), M_GOLD))
    prims.append((tf(frustum(0.285, 0.285, 0.045, 24), rot=(90, 0, 0)), M_GOLD2))
    prims.append((tf(frustum(0.15, 0.15, 0.085, 20), rot=(90, 0, 0)), M_GOLD))
    spin.prims = prims
    return Model([root])

def build_blob():
    root = Node('BlobShadow')
    Node('BlobBody', prims=[(shift(tf(frustum(0.5, 0.5, 0.004, 28), s=(1.0, 1.0, 1.25)), (0, 0.012, 0)), M_BLOB)], parent=root)
    return Model([root])

def build_env(seed, length=24.0):
    rnd = random.Random(seed)
    root = Node('EnvChunk')
    prims = []
    zc = -length / 2   # chunk spans z in [-length, 0]
    prims.append((shift(box(5.8, 0.4, length), (0, -0.2, zc)), M_FLOOR))
    for x in (-0.8, 0.8):
        prims.append((shift(box(0.05, 0.012, length), (x, 0.004, zc)), M_LINE))
    k = int(length // 3)
    for i in range(k):
        prims.append((shift(box(4.8, 0.008, 0.05), (0, 0.003, -1.5 - i * 3.0)), M_SLEEPER))
    for sx in (-1, 1):
        prims.append((shift(box(0.32, 0.55, length), (sx * 3.05, 0.275, zc)), M_WALL))
        prims.append((shift(box(0.06, 0.02, length), (sx * 2.93, 0.565, zc)), M_STRIP))
        for i in range(4):
            z = -3.0 - i * 6.0
            prims.append((shift(box(0.4, 3.6, 0.4), (sx * 3.55, 1.8, z)), M_PILLAR))
            prims.append((shift(box(0.5, 0.12, 0.5), (sx * 3.3, 3.62, z)), M_LAMPW))
    prims.append((shift(box(70.0, 0.2, length), (0, -0.55, zc)), M_VOID))
    # skyline
    mats = [M_BLD1, M_BLD2, M_BLD3]
    for sx in (-1, 1):
        x = 6.5
        while x < 30:
            w = rnd.uniform(3.0, 6.0); d = rnd.uniform(4.0, 8.0); h = rnd.uniform(5.0, 24.0)
            z = rnd.uniform(-length + 3, -3)
            cx = sx * (x + w / 2)
            m = rnd.choice(mats)
            prims.append((shift(box(w, h, d), (cx, h / 2 - 0.5, z)), m))
            for _ in range(rnd.randint(4, 12)):
                wy = rnd.uniform(1.0, h - 1.0); wz = z + rnd.uniform(-d / 2 + 0.4, d / 2 - 0.4)
                prims.append((shift(box(0.04, 0.35, 0.5), (cx - sx * (w / 2 + 0.01), wy, wz)), rnd.choice([M_WIN1, M_WIN1, M_WIN2])))
            x += w + rnd.uniform(0.5, 3.0)
    Node('EnvBody', prims=prims, parent=root)
    return Model([root])

# ---------------------------------------------------------------- GLB writer
class GLBWriter:
    def __init__(self):
        self.bin = bytearray(); self.views = []; self.accs = []
        self.materials = []; self.mat_index = {}; self.meshes = []; self.nodes_json = []

    def _align(self):
        while len(self.bin) % 4: self.bin.append(0)

    def add_view(self, data_bytes, target=None):
        self._align(); off = len(self.bin); self.bin += data_bytes
        v = dict(buffer=0, byteOffset=off, byteLength=len(data_bytes))
        if target: v['target'] = target
        self.views.append(v); return len(self.views) - 1

    def add_acc(self, arr, ctype, typ, target=None, minmax=False):
        dt = {5126: np.float32, 5123: np.uint16, 5125: np.uint32}[ctype]
        a = np.ascontiguousarray(arr, dtype=dt)
        vi = self.add_view(a.tobytes(), target)
        acc = dict(bufferView=vi, componentType=ctype, count=int(len(a)), type=typ)
        if minmax:
            acc['min'] = [float(x) for x in a.min(axis=0)] if a.ndim > 1 else [float(a.min())]
            acc['max'] = [float(x) for x in a.max(axis=0)] if a.ndim > 1 else [float(a.max())]
        self.accs.append(acc); return len(self.accs) - 1

    def material(self, name):
        if name in self.mat_index: return self.mat_index[name]
        m = MATS[name]; col = srgb2lin(hex2rgb(m['color']))
        j = dict(name=name, pbrMetallicRoughness=dict(baseColorFactor=[*col, m['alpha']], metallicFactor=m['metallic'], roughnessFactor=m['rough']))
        if m['emissive']:
            e = srgb2lin(hex2rgb(m['emissive'])); k = min(1.0, m['ek'])
            j['emissiveFactor'] = [min(1.0, c * k) for c in e]
        if m['double']: j['doubleSided'] = True
        if m['alpha'] < 1.0: j['alphaMode'] = 'BLEND'
        self.materials.append(j); self.mat_index[name] = len(self.materials) - 1
        return self.mat_index[name]

    def mesh_for(self, prims, name):
        # group by material -> one primitive per material
        groups = {}
        for (P, N, I), mt in prims: groups.setdefault(mt, []).append((P, N, I))
        jprims = []
        for mt, ms in groups.items():
            P, N, I = merge(ms)
            idx = I.reshape(-1)
            if len(P) > 65535:
                ia = self.add_acc(idx, 5125, 'SCALAR', 34963)
            else:
                ia = self.add_acc(idx, 5123, 'SCALAR', 34963)
            jprims.append(dict(attributes=dict(POSITION=self.add_acc(P, 5126, 'VEC3', 34962, True),
                                               NORMAL=self.add_acc(N, 5126, 'VEC3', 34962)),
                               indices=ia, material=self.material(mt), mode=4))
        self.meshes.append(dict(name=name, primitives=jprims)); return len(self.meshes) - 1

    def write(self, model, anims, path):
        nodes_json = []; index = {}
        order = list(model.nodes())
        for n in order:
            index[n.name] = len(nodes_json)
            j = dict(name=n.name)
            nodes_json.append(j)
        for n in order:
            j = nodes_json[index[n.name]]
            if np.any(np.abs(n.t) > 1e-9): j['translation'] = [float(x) for x in n.t]
            if np.any(np.abs(n.q - np.array([0, 0, 0, 1])) > 1e-9): j['rotation'] = [float(x) for x in n.q]
            if np.any(np.abs(n.s - 1) > 1e-9): j['scale'] = [float(x) for x in n.s]
            if n.prims: j['mesh'] = self.mesh_for(n.prims, n.name + '_mesh')
            if n.children: j['children'] = [index[c.name] for c in n.children]
        janims = []
        for name, (loop, tracks) in (anims or {}).items():
            samplers = []; channels = []
            for nm, tk in tracks.items():
                times = np.array(tk['t'], np.float32)
                ti = self.add_acc(times, 5126, 'SCALAR', None, True)
                for path_, key, typ in (('translation', 'tr', 'VEC3'), ('rotation', 'rot', 'VEC4'), ('scale', 'sc', 'VEC3')):
                    if tk.get(key) is None: continue
                    vals = np.array(tk[key], np.float32)
                    if key == 'rot':   # keep hemisphere continuity
                        for i in range(1, len(vals)):
                            if np.dot(vals[i], vals[i - 1]) < 0: vals[i] = -vals[i]
                        vals = vals / np.linalg.norm(vals, axis=1, keepdims=True)
                    oi = self.add_acc(vals, 5126, typ)
                    samplers.append(dict(input=ti, output=oi, interpolation='LINEAR'))
                    channels.append(dict(sampler=len(samplers) - 1, target=dict(node=index[nm], path=path_)))
            janims.append(dict(name=name, samplers=samplers, channels=channels))
        gltf = dict(asset=dict(version='2.0', generator='quacky-asset-gen'), scene=0,
                    scenes=[dict(nodes=[index[r.name] for r in model.roots])],
                    nodes=nodes_json, meshes=self.meshes, materials=self.materials,
                    accessors=self.accs, bufferViews=self.views,
                    buffers=[dict(byteLength=0)])
        if janims: gltf['animations'] = janims
        self._align(); gltf['buffers'][0]['byteLength'] = len(self.bin)
        js = json.dumps(gltf, separators=(',', ':')).encode()
        while len(js) % 4: js += b' '
        total = 12 + 8 + len(js) + 8 + len(self.bin)
        with open(path, 'wb') as f:
            f.write(struct.pack('<III', 0x46546C67, 2, total))
            f.write(struct.pack('<II', len(js), 0x4E4F534A)); f.write(js)
            f.write(struct.pack('<II', len(self.bin), 0x004E4942)); f.write(bytes(self.bin))
        tris = sum(len(p[2]) for n in order for p in [pr[0] for pr in n.prims])
        return tris

def export(model, anims, path):
    return GLBWriter().write(model, anims, path)

# ---------------------------------------------------------------- spin animation for the coin
def coin_anims():
    ts = [0, 0.3, 0.6, 0.9, 1.2]
    # 90-degree steps about Y so slerp always takes the short way; last key = full turn (same rotation as the first)
    rots = [q_euler(y=v) for v in (0, 90, 180, 270)] + [np.array([0, 0, 0, -1.0])]
    return {'spin': (True, {'CoinSpin': dict(t=ts, rot=rots)})}

if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else 'out'
    os.makedirs(out, exist_ok=True)
    report = {}
    duck = build_duck(); da = build_duck_anims(duck)
    report['quacky_duck.glb'] = (export(duck, da, f'{out}/quacky_duck.glb'), da)
    report['train.glb'] = (export(build_train(), None, f'{out}/train.glb'), {})
    report['barrier.glb'] = (export(build_barrier(), None, f'{out}/barrier.glb'), {})
    report['duct.glb'] = (export(build_duct(), None, f'{out}/duct.glb'), {})
    report['blob_shadow.glb'] = (export(build_blob(), None, f'{out}/blob_shadow.glb'), {})
    report['breadcrumb.glb'] = (export(build_coin(), coin_anims(), f'{out}/breadcrumb.glb'), coin_anims())
    for i, seed in enumerate((11, 23, 37)):
        nm = f'env_chunk_{"abc"[i]}.glb'
        report[nm] = (export(build_env(seed), None, f'{out}/{nm}'), {})
    with open(f'{out}/ANIMATIONS.md', 'w') as f:
        f.write('# Animation clips (generated)\n\n| Model | Clip | Duration (s) | Loop | Animated nodes |\n|---|---|---|---|---|\n')
        for nm, (tris, an) in report.items():
            for cn, (loop, tracks) in an.items():
                dur = max(max(tk['t']) for tk in tracks.values())
                f.write(f"| {nm} | {cn} | {dur:.3f} | {'yes' if loop else 'no (hold last pose)'} | {', '.join(sorted(tracks.keys()))} |\n")
    for nm, (tris, an) in report.items():
        size = os.path.getsize(f'{out}/{nm}')
        print(f'{nm:22s} tris={tris:6d} bytes={size:8d} anims={list(an.keys())}')
