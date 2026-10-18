"""Wave 36: the relics of the dimensional rifts and the waypoint (1.72.0): the Return Crystal, the Gate Key, the Rift Compass, the Pocket Key and the Waypoint Anchor.
Every ability is a `rift:<call>` of RiftActs (relics/wave/RiftActs.java); the rules, cooldowns and messages live in labyrinth/mega/rift."""
from relic_rows_lib import R

ROWS = [
    R("Return Crystal", "CHARM", "ORB", "SPECIAL", ("#9fd8ff", "#e8f4ff", "#c8ecff"), "gem",
      "A clear crystal that remembers where you stood. Hold it and it carries you out of the dungeon to Floor 0, and keeps the place so you can find your way back to the very block.",
      "a:luck:+1",
      [("Return to Floor 0", 8, 0, "rift:crystal", "The crystal rings, and the floor lets you go.")]),
    R("Gate Key", "CHARM", "TOME", "RARE", ("#d8b860", "#4a3a20", "#ffe8a0"), "key",
      "A heavy brass key cut for no lock you can find. Away from the dungeon it opens the way back to the place you left, exactly; held while sneaking it opens a Return Portal on the spot.",
      "a:luck:+1",
      [("Return to the Waypoint", 8, 0, "rift:key", "The key turns, and the dungeon opens."), ("Open a Return Portal", 20, 0, "rift:portal", "A tear opens in the air: it leads back.", 0, True)]),
    R("Rift Compass", "CHARM", "ORB", "RARE", ("#5a6a8a", "#c8d8f0", "#a0c8ff"), "compass",
      "A compass whose needle ignores north and swings toward the nearest tear in the dungeon's air.",
      "e:night_vision:0",
      [("Find the Tear", 10, 0, "rift:sense", "The needle shivers and settles.")]),
    R("Pocket Key", "CHARM", "AMULET", "LEGEND", ("#8a4ac8", "#2a1a3a", "#d8a0ff"), "key",
      "A small violet key that unlocks nothing and everything. Turn it in the air of a floor and a hidden realm's door opens in front of you, once a day.",
      "a:luck:+2",
      [("Open a Hidden Realm", 60, 0, "rift:pocket", "The air parts like a curtain.", 1)], att=True),
    R("Waypoint Anchor", "NECK", "PENDANT", "SPECIAL", ("#4a8a8a", "#d8e8e8", "#90ffe8"), "pendant",
      "A small iron anchor on a chain. Squeeze it and the place you are standing becomes the one the Return Crystal and the Gate Key bring you back to.",
      "a:fall:+2",
      [("Mark this Place", 30, 0, "rift:mark", "The anchor goes still.")]),
]
