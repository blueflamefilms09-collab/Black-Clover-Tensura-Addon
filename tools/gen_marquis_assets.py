from __future__ import annotations

import json
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/nusmp"
GEO = ROOT / "geo/entity"
TEXTURES = ROOT / "textures/entity"
SIZE = 128


def box(name: str, parent: str, origin: list[float], dimensions: list[float], uv: list[int], inflate: float = 0) -> dict:
    cube = {"origin": origin, "size": dimensions, "uv": uv}
    if inflate:
        cube["inflate"] = inflate
    return {"name": name, "parent": parent, "pivot": [0, 0, 0], "cubes": [cube]}


def model(identifier: str, bones: list[dict]) -> dict:
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": f"geometry.{identifier}",
                "texture_width": SIZE,
                "texture_height": SIZE,
                "visible_bounds_width": 3,
                "visible_bounds_height": 3,
                "visible_bounds_offset": [0, 1, 0],
            },
            "bones": bones,
        }],
    }


def paint_box(draw: ImageDraw.ImageDraw, u: int, v: int, w: int, h: int, d: int, color: tuple[int, int, int],
              front: tuple[int, int, int] | None = None) -> None:
    # Paint the standard unfolded cube UV net; the north face is the front of the model.
    draw.rectangle((u, v, u + 2 * d + 2 * w - 1, v + d + h - 1), fill=color)
    if front:
        draw.rectangle((u + d, v + d, u + d + w - 1, v + d + h - 1), fill=front)


def write_texture(path: Path, coat: bool) -> None:
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0) if coat else (70, 100, 145, 255))
    draw = ImageDraw.Draw(image)
    if not coat:
        shirt = (51, 119, 178, 255)
        for uv in ((32, 0),):
            paint_box(draw, *uv, 8, 12, 4, shirt, shirt)
        # White Black-Bull-inspired sweatshirt crest, centered on the chest.
        for x, y, width in ((38, 9, 2), (39, 8, 2), (40, 9, 2), (39, 10, 2), (38, 12, 4)):
            draw.rectangle((x, y, x + width - 1, y), fill=(237, 240, 242, 255))
        for uv in ((0, 0), (16, 0)):
            paint_box(draw, *uv, 4, 12, 4, (103, 110, 121, 255))
            draw.rectangle((uv[0] + 4, uv[1] + 14, uv[0] + 7, uv[1] + 15), fill=(236, 238, 240, 255))
        for uv in ((50, 32), (82, 32)):
            paint_box(draw, *uv, 4, 12, 4, shirt)
            draw.rectangle((uv[0] + 4, uv[1] + 14, uv[0] + 7, uv[1] + 15), fill=(211, 220, 230, 255))
        paint_box(draw, 44, 16, 8, 8, 8, (119, 78, 53, 255), (206, 161, 125, 255))
        # Small pixel eyes, hairline, and warm skin highlights.
        draw.rectangle((52, 25, 59, 28), fill=(101, 64, 42, 255))
        draw.point((54, 29), fill=(57, 107, 165, 255))
        draw.point((57, 29), fill=(57, 107, 165, 255))
        paint_box(draw, 96, 0, 6, 2, 6, (102, 67, 43, 255))
        for uv in ((0, 32), (12, 32), (24, 32), (36, 32)):
            paint_box(draw, *uv, 3, 3, 3, (102, 67, 43, 255))
    else:
        black, silver, violet = (20, 19, 29, 255), (183, 188, 205, 255), (111, 102, 190, 255)
        for uv, w, h, d in (((0, 64), 8, 12, 4), ((24, 64), 4, 12, 4), ((40, 64), 4, 12, 4)):
            paint_box(draw, *uv, w, h, d, black)
        # Silver seams and cuffs run along the lower fronts and shoulders.
        draw.rectangle((4, 74, 11, 75), fill=silver)
        draw.rectangle((28, 76, 31, 76), fill=silver)
        draw.rectangle((44, 76, 47, 76), fill=silver)
        # Compact bull-skull crest, visibly distinct from a generic cross.
        draw.rectangle((6, 68, 9, 70), fill=silver)
        draw.point((5, 67), fill=silver)
        draw.point((10, 67), fill=silver)
        draw.rectangle((6, 71, 9, 72), fill=violet)
        draw.rectangle((0, 88, 7, 90), fill=black)
        draw.rectangle((16, 88, 19, 90), fill=black)
        draw.rectangle((32, 88, 35, 90), fill=black)
    image.save(path)


def write_assets() -> None:
    GEO.mkdir(parents=True, exist_ok=True)
    TEXTURES.mkdir(parents=True, exist_ok=True)
    base = [
        {"name": "root", "pivot": [0, 0, 0], "cubes": []},
        {"name": "legR", "parent": "root", "pivot": [-2, 12, 0], "cubes": [{"origin": [-4, 0, -2], "size": [4, 12, 4], "uv": [0, 0]}]},
        {"name": "legL", "parent": "root", "pivot": [2, 12, 0], "cubes": [{"origin": [0, 0, -2], "size": [4, 12, 4], "uv": [16, 0]}]},
        {"name": "torso", "parent": "root", "pivot": [0, 12, 0], "cubes": [{"origin": [-4, 12, -2], "size": [8, 12, 4], "uv": [32, 0]}]},
        {"name": "head", "parent": "torso", "pivot": [0, 24, 0], "cubes": [{"origin": [-4, 24, -4], "size": [8, 8, 8], "uv": [44, 16]}]},
        {"name": "hair", "parent": "head", "pivot": [0, 32, 0], "cubes": [
            {"origin": [-3, 31, -3], "size": [6, 2, 6], "uv": [96, 0]},
            {"origin": [-4, 29, -2], "size": [2, 4, 4], "uv": [0, 32]},
            {"origin": [2, 29, -2], "size": [2, 4, 4], "uv": [12, 32]},
            {"origin": [-2, 32, 1], "size": [4, 2, 4], "uv": [24, 32]},
        ]},
        {"name": "armR", "parent": "torso", "pivot": [-5, 22, 0], "cubes": [{"origin": [-8, 12, -2], "size": [4, 12, 4], "uv": [50, 32]}]},
        {"name": "armL", "parent": "torso", "pivot": [5, 22, 0], "cubes": [{"origin": [4, 12, -2], "size": [4, 12, 4], "uv": [82, 32]}]},
    ]
    coat = [
        {"name": "root", "pivot": [0, 0, 0], "cubes": []},
        {"name": "torso", "parent": "root", "pivot": [0, 12, 0], "cubes": [
            {"origin": [-4.3, 12, -2.3], "size": [8.6, 12, 4.6], "uv": [0, 64]},
            {"origin": [-4.5, 23, -2.5], "size": [9, 3, 5], "uv": [0, 80]},
            {"origin": [-4.2, 1, 1.8], "size": [8.4, 11, 1], "uv": [0, 88]},
        ]},
        {"name": "armR", "parent": "torso", "pivot": [-5, 22, 0], "cubes": [{"origin": [-8.2, 12, -2.2], "size": [4.4, 12, 4.4], "uv": [24, 64]}]},
        {"name": "armL", "parent": "torso", "pivot": [5, 22, 0], "cubes": [{"origin": [3.8, 12, -2.2], "size": [4.4, 12, 4.4], "uv": [40, 64]}]},
    ]
    for name, bones in (("marquis_skin", base), ("marquis_coat", coat)):
        (GEO / f"{name}.geo.json").write_text(json.dumps(model(name, bones), separators=(",", ":")) + "\n", encoding="utf-8")
    final_source = json.loads((GEO / "riven_remake_final.geo.json").read_text(encoding="utf-8"))
    source_bones = final_source["minecraft:geometry"][0]["bones"]
    selected = [dict(bone, cubes=[] if bone["name"] in {"root", "torso", "head"} else bone.get("cubes", []))
                for bone in source_bones if bone["name"] in {"root", "torso", "head", "wingR", "wingL", "crown"}]
    (GEO / "marquis_final.geo.json").write_text(json.dumps(model("marquis_final", selected), separators=(",", ":")) + "\n", encoding="utf-8")
    write_texture(TEXTURES / "marquis_skin.png", False)
    write_texture(TEXTURES / "marquis_coat.png", True)
    Image.open(TEXTURES / "riven_remake_final.png").save(TEXTURES / "marquis_final.png")
    icons = {
        "marquis_story_page": ((112, 92, 55), (213, 189, 123)),
        "marquis_crest_cape": ((48, 111, 169), (238, 241, 245)),
        "marquis_final_crown": ((25, 20, 42), (137, 119, 220)),
    }
    item_textures = ROOT / "textures/item"
    item_models = ROOT / "models/item"
    item_textures.mkdir(parents=True, exist_ok=True)
    item_models.mkdir(parents=True, exist_ok=True)
    for name, (base, accent) in icons.items():
        icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        art = ImageDraw.Draw(icon)
        if name == "marquis_story_page":
            art.polygon(((3, 1), (12, 2), (13, 14), (9, 13), (7, 15), (3, 13)), fill=base + (255,))
            art.rectangle((5, 4, 10, 5), fill=accent + (255,))
            art.line((5, 8, 10, 8), fill=accent + (255,))
            art.point((7, 10), fill=accent + (255,))
        elif name == "marquis_crest_cape":
            art.polygon(((2, 2), (13, 2), (11, 14), (8, 12), (5, 14)), fill=base + (255,))
            art.polygon(((6, 6), (8, 4), (10, 6), (8, 8)), fill=accent + (255,))
            art.rectangle((7, 8, 9, 10), fill=accent + (255,))
        else:
            art.polygon(((2, 12), (3, 5), (6, 7), (8, 2), (10, 7), (13, 4), (14, 12)), fill=base + (255,))
            art.line((3, 11, 8, 13, 13, 11), fill=accent + (255,), width=2)
            art.point((8, 7), fill=accent + (255,))
        icon.save(item_textures / f"{name}.png")
        (item_models / f"{name}.json").write_text(
            json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": f"nusmp:item/{name}"}}, separators=(",", ":")) + "\n",
            encoding="utf-8",
        )


if __name__ == "__main__":
    write_assets()
