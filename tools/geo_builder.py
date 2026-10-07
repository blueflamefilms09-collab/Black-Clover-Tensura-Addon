"""GeoLite model builder: writes the .geo.json / .animation.json / texture files that client/geo (GeoModels, GeoDraw, GeoSpec) loads.

    from geo_builder import Model, Anim
    m = Model("bronze", "statue", tex_w=128, tex_h=128)
    m.bone("root", pivot=(0, 0, 0))
    m.bone("torso", parent="root", pivot=(0, 12, 0))
    m.cube("torso", origin=(-4, 12, -2), size=(8, 12, 4), uv=(16, 16))            # box uv like a vanilla model
    m.cube("torso", origin=(-5, 11, -3), size=(10, 2, 6), uv=(0, 0), inflate=0.2, rotation=(0, 0, 15), pivot=(0, 12, 0))
    a = Anim(); a.clip("idle", length=2.0, loop=True)
    a.rot("idle", "torso", {0.0: (0, 0, 0), 1.0: (0, 4, 0), 2.0: (0, 0, 0)})
    m.save(a)                 # writes geo / animations / textures/entity paths (texture only if you call m.texture(...))

File frame (same as Blockbench): pixels (1/16 block), y UP, origin at the feet, the model's front on -z, +x is the model's left.
Animation rotation values are DEGREES added to the bone's own rotation, positions in pixels, scale multiplies.
Textures: use Model.paint() to get a PIL image of tex_w x tex_h (+ a glow image), draw on it, then m.save_texture(img, glow).
Run python as `python3 -B`. Files are written under src/main/resources/assets/nusmp/.
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp")


class Model:
    def __init__(self, key, name, tex_w=64, tex_h=64):
        self.key, self.name, self.tex_w, self.tex_h = key, name, tex_w, tex_h
        self.bones = {}
        self.order = []

    @property
    def id(self):
        return f"{self.key}_{self.name}"

    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        b = {"name": name, "pivot": list(pivot), "cubes": []}
        if parent:
            b["parent"] = parent
        if rotation:
            b["rotation"] = list(rotation)
        self.bones[name] = b
        self.order.append(name)
        return b

    def cube(self, bone, origin, size, uv=(0, 0), inflate=0.0, rotation=None, pivot=None, mirror=False):
        c = {"origin": list(origin), "size": list(size), "uv": list(uv) if not isinstance(uv, dict) else uv}
        if inflate:
            c["inflate"] = inflate
        if rotation:
            c["rotation"] = list(rotation)
            c["pivot"] = list(pivot if pivot is not None else self.bones[bone]["pivot"])
        if mirror:
            c["mirror"] = True
        self.bones[bone]["cubes"].append(c)
        return c

    def geo(self):
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": "geometry." + self.id, "texture_width": self.tex_w, "texture_height": self.tex_h,
                            "visible_bounds_width": 4, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1, 0]},
            "bones": [self.bones[n] for n in self.order]}]}

    def cube_count(self):
        return sum(len(b["cubes"]) for b in self.bones.values())

    def paint(self):
        from PIL import Image
        return Image.new("RGBA", (self.tex_w, self.tex_h), (0, 0, 0, 0)), Image.new("RGBA", (self.tex_w, self.tex_h), (0, 0, 0, 0))

    def save(self, anim=None):
        os.makedirs(os.path.join(ROOT, "geo", "entity"), exist_ok=True)
        with open(os.path.join(ROOT, "geo", "entity", self.id + ".geo.json"), "w") as f:
            json.dump(self.geo(), f, indent=1)
        if anim is not None:
            os.makedirs(os.path.join(ROOT, "animations", "entity"), exist_ok=True)
            with open(os.path.join(ROOT, "animations", "entity", self.id + ".animation.json"), "w") as f:
                json.dump(anim.data(), f, indent=1)

    def save_texture(self, img, glow=None):
        os.makedirs(os.path.join(ROOT, "textures", "entity"), exist_ok=True)
        img.save(os.path.join(ROOT, "textures", "entity", self.id + ".png"))
        if glow is not None:
            glow.save(os.path.join(ROOT, "textures", "entity", self.id + "_glow.png"))


class Anim:
    def __init__(self):
        self.clips = {}

    def clip(self, name, length=1.0, loop=True):
        self.clips[name] = {"loop": bool(loop), "animation_length": length, "bones": {}}
        return self.clips[name]

    def _track(self, clip, bone, kind, frames):
        b = self.clips[clip]["bones"].setdefault(bone, {})
        b[kind] = {f"{t:.2f}": list(v) for t, v in sorted(frames.items())}

    def rot(self, clip, bone, frames): self._track(clip, bone, "rotation", frames)
    def pos(self, clip, bone, frames): self._track(clip, bone, "position", frames)
    def scale(self, clip, bone, frames): self._track(clip, bone, "scale", frames)

    def data(self):
        return {"format_version": "1.8.0", "animations": self.clips}
