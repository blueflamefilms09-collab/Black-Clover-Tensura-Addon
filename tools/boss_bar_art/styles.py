"""The table of boss bar styles: one entry per style id (the same ids as BarStyleIds.ALL in the Java code; a JUnit test compares them).
Palette keys: dk outline/dark, md body, lt light body, hi highlight, gl glow (emissive), ac accent (gems, cast bar).
fill = (top, bottom) of the health fill. motifs = functions of motifs.py, drawn in order. emblem = a name of emblems.py."""

FX = ["shimmer", "drip", "embers", "wave", "sparks", "frost", "souls", "leaves", "static", "glitch", "veins", "runes", "shield"]

STYLES = []


def S(id, family, emblem, motifs, dk, md, lt, hi, gl, ac, fill_top, fill_bottom, fx="shimmer", entrance="slide", death="shatter",
      name="#FFFFFF", title=None, lair="#FFB347", lair_name=None, shield="#9FE8FF", seed=1):
    STYLES.append(dict(id=id, family=family, emblem=emblem, motifs=motifs, pal=dict(dk=dk, md=md, lt=lt, hi=hi, gl=gl, ac=ac),
                       fill=(fill_top, fill_bottom), fx=fx, entrance=entrance, death=death, name=name, title=title or lt, lair=lair,
                       shield=shield, seed=seed))


# ---- the themes of the Decennial halls / boss families
S("holy", "theme", "wing", ["laurel", "wings"], "#3b2b06", "#8a6a1c", "#e8c25a", "#fff6d0", "#ffe9a0", "#fff2c0", "#fff3b0", "#d9a21b", "shimmer", "slide", name="#FFF6D6", title="#E8C25A")
S("evil", "theme", "thorn_crown", ["thorns", "drips"], "#0c0a0d", "#2a2428", "#55484e", "#8a7a80", "#ff2a2a", "#ff5a4a", "#ff5a4a", "#7a0c10", "drip", "rise", name="#FFE0DC", title="#C86A64")
S("undead", "theme", "skull", ["bones"], "#1a1d14", "#8d8a73", "#d8d4b8", "#f5f2da", "#7dff9b", "#4ddc7c", "#9affb0", "#1c8f4a", "souls", "fade", "dissolve", name="#EAF8E4", title="#9AD8A8")
S("fire", "theme", "flame", ["flames"], "#2b0b04", "#7a2a0c", "#d8641a", "#ffd27a", "#ff9a3a", "#ffc25a", "#ffd24a", "#d42a08", "embers", "flash", name="#FFF0D0", title="#FFB060")
S("ice", "theme", "snowflake", ["icicles", "crystals"], "#0b1f33", "#4b7fa8", "#a6d6f0", "#eefaff", "#bff0ff", "#dff8ff", "#d8f6ff", "#3b8fd0", "frost", "fade", name="#F0FBFF", title="#A6D6F0", shield="#FFFFFF")
S("storm", "theme", "bolt", ["bolts"], "#14123a", "#3d3f8a", "#7a86e0", "#e8ecff", "#ffe84a", "#fff48a", "#fff48a", "#4a5ad8", "sparks", "flash", name="#FFFBD8", title="#B8C0FF")
S("void", "theme", "void_eye", ["void_tendrils", "stars"], "#060410", "#2a1850", "#5a3a9a", "#b890ff", "#b06aff", "#d0a8ff", "#c08bff", "#2a0e5a", "static", "fade", "dissolve", name="#F0E4FF", title="#A88AE0")
S("forest", "theme", "leaf", ["vines"], "#0d2410", "#2e5a22", "#6aa83a", "#d6f0a0", "#9aff6a", "#c8ff8a", "#b6f06a", "#2f7a24", "leaves", "rise", name="#F0FFE0", title="#9AD070")
S("water", "theme", "wave", ["waves", "shells"], "#04203a", "#1a5f8f", "#4fb0d8", "#d0f6ff", "#6af0ff", "#a0f8ff", "#7af0ff", "#0a5fb0", "wave", "slide", name="#EAFBFF", title="#7ACCE8")
S("blood", "theme", "blood_drop", ["fangs", "drips"], "#1a0206", "#5a0a14", "#a01828", "#ff7a80", "#ff3040", "#ff6a70", "#ff4a58", "#6a0010", "drip", "rise", name="#FFE4E6", title="#E0707C")
S("clockwork", "theme", "gear", ["gears"], "#1c1408", "#6b4e1c", "#c9973a", "#ffe6a0", "#ffcf5a", "#ffe08a", "#ffd982", "#9a6a1a", "sparks", "slide", name="#FFF4D4", title="#D8B060")
S("mirror", "theme", "mirror", ["shards"], "#1a1d2a", "#7b86a8", "#cdd8f0", "#ffffff", "#e8f0ff", "#ffffff", "#f0f6ff", "#7a90c8", "glitch", "flash", "shatter", name="#FFFFFF", title="#CDD8F0")
S("underworld", "theme", "root", ["roots", "chains"], "#0a0608", "#3a1c26", "#7a3a48", "#c06a78", "#ff4a68", "#ff7a90", "#ff6a8a", "#4a0a22", "veins", "rise", name="#FFE0E8", title="#C87888")
S("qliphoth_gate", "theme", "gate", ["gate"], "#08040c", "#4a2060", "#8a48b0", "#e0a0ff", "#c060ff", "#e0a0ff", "#d890ff", "#3a0e5a", "runes", "flash", name="#F4E4FF", title="#B878E0")
S("devil", "theme", "horns", ["horns"], "#1a0505", "#6a1010", "#c02a1c", "#ff9a6a", "#ff5a1a", "#ff8a4a", "#ff7a3a", "#8a0e08", "embers", "flash", name="#FFE8D8", title="#E87050")
S("supreme_devil", "theme", "crown_horns", ["horns", "crown"], "#0a0204", "#4a0a10", "#c8a030", "#fff0a0", "#ff3a20", "#ffd24a", "#ff4a30", "#5a0008", "embers", "flash", name="#FFF0C8", title="#E0B040", lair="#FF6A3A")
S("dragon", "theme", "dragon", ["scales"], "#101a10", "#2f6a4a", "#62b07a", "#ffe08a", "#ffcf4a", "#ffe08a", "#ffcf4a", "#c0501a", "embers", "slide", name="#FFF4D0", title="#8ACA98")
S("angel", "theme", "feather", ["wings", "halo_small"], "#2a3a5a", "#9ab0d8", "#e8f0ff", "#ffffff", "#cfe8ff", "#ffffff", "#f4faff", "#8ab4ec", "shimmer", "slide", name="#FFFFFF", title="#C8DCFF", seed=3)
S("archon", "theme", "halo", ["halo", "rays"], "#1a2a4a", "#4a78c0", "#f0d870", "#fffbe0", "#fff0a0", "#ffffff", "#fff6c0", "#e0a020", "shimmer", "flash", name="#FFFBE8", title="#F0D870")
S("lich", "theme", "lich", ["bones", "phylactery"], "#0e0a16", "#4a3a6a", "#9a88c8", "#d8ccff", "#6aff9a", "#8affb8", "#9affc0", "#3a1a7a", "souls", "fade", "dissolve", name="#EAFFF2", title="#A898D8")
S("fallen_king", "theme", "broken_crown", ["broken_crown", "tatters"], "#12100a", "#5a4a2a", "#a08840", "#e8d890", "#c0a040", "#e0c860", "#d8c070", "#4a3a14", "static", "fade", "dissolve", name="#F4ECD0", title="#A89868")
S("pocket_tomb", "theme", "ankh", ["hieroglyph"], "#2a1a04", "#a67c1c", "#f0c850", "#fff0b0", "#ffd860", "#6ad0ff", "#ffe27a", "#b07814", "shimmer", "rise", name="#FFF6D8", title="#F0C850", shield="#6AD0FF")
S("plague", "theme", "plague", ["spores", "tendrils"], "#12160a", "#5a6a1c", "#a8b83a", "#e8f080", "#c8ff3a", "#e0ff6a", "#d8f060", "#4a6a10", "veins", "fade", "dissolve", name="#F6FFD8", title="#B8C860")
# ---- the arcs and the named fights
S("zogratis", "arc", "triad", ["triad"], "#0e0618", "#4a1a60", "#a02a70", "#ff90c0", "#ff3a9a", "#ff70b8", "#ff6ab0", "#5a0a50", "embers", "flash", name="#FFE4F2", title="#E070A8")
S("midnight_sun", "arc", "eclipse", ["eclipse"], "#06081a", "#1a2260", "#e8a830", "#fff0a0", "#ffcf3a", "#fff0a0", "#ffd860", "#1a2a90", "shimmer", "flash", name="#FFF4D0", title="#E8A830")
S("seven_sins", "arc", "sins", ["sins"], "#1a0c10", "#7a2a3a", "#e8b8a0", "#fff0e0", "#ff8a5a", "#ffb088", "#ffb08a", "#9a2a30", "embers", "slide", name="#FFF0E4", title="#E8B8A0")
S("spade_kingdom", "arc", "spade", ["spades"], "#0a0a12", "#3a3a5a", "#8a8ab0", "#e0e0ff", "#a080ff", "#c0a8ff", "#b8a0ff", "#2a1a6a", "static", "fade", name="#F0F0FF", title="#9A9AD0")
S("magic_knights", "arc", "knight", ["knight_wings"], "#101a2a", "#4a6a9a", "#a8c0e0", "#ffffff", "#70b0ff", "#a0d0ff", "#9ac8ff", "#2a5ac0", "shimmer", "slide", name="#F0F8FF", title="#A8C0E0")
S("danmachi", "arc", "labyrinth", ["meander"], "#1a1410", "#5a4a3a", "#9a846a", "#f0d8b0", "#ff9a3a", "#ffc070", "#ffb050", "#8a3a10", "embers", "rise", name="#FFF0DC", title="#C8A880")
S("lucius", "arc", "crown", ["laurel", "crown", "wings"], "#140828", "#5a2a9a", "#e8b923", "#fff0a0", "#ffd24a", "#fff0a0", "#ffe27a", "#4a1a8a", "shimmer", "flash", name="#FFF4C8", title="#E8B923", seed=5)
S("god_of_order", "boss", "scales", ["laurel", "halo_small", "scales_motif"], "#2a2618", "#b8a870", "#f4efdc", "#ffffff", "#fff6c8", "#ffffff", "#fffbe8", "#d8c070", "shimmer", "flash", name="#FFFFFF", title="#F4EFDC", seed=7)
S("goliath", "boss", "anvil", ["rivets", "embers_top"], "#14161a", "#5a626e", "#9aa4b2", "#dde4ee", "#ff8a3a", "#ffb060", "#c8d0dc", "#5a6270", "embers", "slide", name="#F0F4FA", title="#9AA4B2")
S("charybdis", "boss", "whirlpool", ["waves", "whirl"], "#031428", "#0d4f8f", "#1e7fc0", "#c8ffff", "#7ff0ff", "#c8ffff", "#6ae0ff", "#0a3f8f", "wave", "rise", name="#E8FFFF", title="#5AB8E0")
S("warden", "boss", "key", ["rivets", "runes_motif"], "#100c18", "#483a58", "#8a78a0", "#d0c0e8", "#a070ff", "#c8a8ff", "#b898f0", "#3a2a60", "runes", "slide", name="#F4ECFF", title="#A898C8")
S("spirit_king", "boss", "spirit", ["orbs", "wings"], "#10182a", "#6a82b8", "#d8e6ff", "#ffffff", "#a0d0ff", "#ffffff", "#e8f4ff", "#7a9ae0", "souls", "fade", "dissolve", name="#FFFFFF", title="#C8DCFF", seed=9)
S("zagred", "boss", "rune", ["runes_motif", "quills"], "#0a0414", "#3a1a6a", "#8a5ad0", "#e0c8ff", "#b070ff", "#d8b0ff", "#c898ff", "#2a0e5a", "runes", "flash", name="#F4E8FF", title="#A880E0")

IDS = [s["id"] for s in STYLES]
assert len(set(IDS)) == len(IDS)
