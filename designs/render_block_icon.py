"""Render textured, axis-aligned Minecraft block models for the Wiki."""
import math

from PIL import Image


def render_icon(model, texture, *, yaw=45, pitch=30, size=256):
    """Use a per-pixel depth test so intersecting and stacked parts occlude correctly."""
    yaw, pitch = map(math.radians, (yaw, pitch))
    sy, cy = math.sin(yaw), math.cos(yaw)
    sp, cp = math.sin(pitch), math.cos(pitch)
    faces = []
    for element in model['elements']:
        x, y, z = element['from']
        X, Y, Z = element['to']
        # Model coordinates at the top-left, top-right and bottom-left of each face's UVs.
        corners = {
            'north': ((X, Y, z), (x, Y, z), (X, y, z)),
            'south': ((x, Y, Z), (X, Y, Z), (x, y, Z)),
            'east': ((X, Y, Z), (X, Y, z), (X, y, Z)),
            'west': ((x, Y, z), (x, Y, Z), (x, y, z)),
            'up': ((x, Y, z), (X, Y, z), (x, Y, Z)),
            'down': ((x, y, Z), (X, y, Z), (x, y, z)),
        }
        for side, face in element['faces'].items():
            points = [
                (-cy * a - sy * c, sp * (sy * a - cy * c) - cp * b, cp * (sy * a - cy * c) + sp * b)
                for a, b, c in corners[side]
            ]
            a, b, d = points
            bx, by = b[0] - a[0], b[1] - a[1]
            dx, dy = d[0] - a[0], d[1] - a[1]
            if bx * dy - by * dx <= 1e-8:
                continue
            name = face['texture']
            if name.startswith('#'):
                name = model['textures'][name[1:]]
            faces.append((a, b, d, texture(name), face['uv'], side))

    def bounds(a, b, d, axis):
        coords = [p[axis] for p in (a, b, d)]
        coords.append(coords[1] + coords[2] - coords[0])
        return min(coords), max(coords)

    extents = [[bounds(a, b, d, axis) for a, b, d, *_ in faces] for axis in (0, 1)]
    (left, right), (top, bottom) = [(min(a for a, _ in axis), max(b for _, b in axis)) for axis in extents]
    scale = (size - 2 * round(size / 24)) / max(right - left, bottom - top)
    offset_x = (size - (left + right) * scale) / 2
    offset_y = (size - (top + bottom) * scale) / 2

    image = Image.new('RGBA', (size, size))
    pixels = image.load()
    depths = [-math.inf] * (size * size)
    lighting = {'up': 1, 'down': .55, 'north': .9, 'south': .9, 'east': .76, 'west': .76}
    for a, b, d, atlas, uv, side in faces:
        a, b, d = [(x * scale + offset_x, y * scale + offset_y, depth) for x, y, depth in (a, b, d)]
        bx, by, bz = (v - base for v, base in zip(b, a))
        dx, dy, dz = (v - base for v, base in zip(d, a))
        det = bx * dy - by * dx
        x0, x1 = bounds(a, b, d, 0)
        y0, y1 = bounds(a, b, d, 1)
        u0, v0, u1, v1 = uv
        texels = atlas.load()
        light = lighting[side]
        for y in range(math.floor(y0), math.ceil(y1)):
            py = y + .5 - a[1]
            for x in range(math.floor(x0), math.ceil(x1)):
                px = x + .5 - a[0]
                u = (px * dy - py * dx) / det
                v = (py * bx - px * by) / det
                if not (0 <= u < 1 and 0 <= v < 1):
                    continue
                depth = a[2] + u * bz + v * dz
                index = y * size + x
                if depth <= depths[index]:
                    continue
                tx = min(atlas.width - 1, int((u0 + u * (u1 - u0)) * atlas.width / 16))
                ty = min(atlas.height - 1, int((v0 + v * (v1 - v0)) * atlas.height / 16))
                r, g, b, alpha = texels[tx, ty]
                if alpha:
                    depths[index] = depth
                    pixels[x, y] = (round(r * light), round(g * light), round(b * light), alpha)
    return image
