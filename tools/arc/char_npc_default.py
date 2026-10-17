"""Why: the fallback body of an NPC whose own model is missing (a roster entry added before its model, an old world): a plain villager-sized person in neutral
clothes, so the player sees a character standing there instead of nothing. It is not a roster character (no ArcCharacter has this id); the NPC renderer draws it when
arc_<id> cannot be found."""
SPEC = {
    "id": "npc_default",
    "name": "Villager",
    "role": "npc",
    "build": "average",
    "height": 1.9,
    "skin": "light",
    "face": {"expression": "calm", "eye_color": "#5a6a58"},
    "hair": {"style": "short", "color": "#4a3828"},
    "outfit": {"top": {"kind": "tunic", "color": "#7a6a4a", "trim": "#c8b48c"}, "legs": {"kind": "trousers", "color": "#4a4438"}, "boots": {"kind": "boots", "color": "#3a2a20", "height": "ankle"},
               "belt": {"kind": "plain", "color": "#3c281c"}},
}
