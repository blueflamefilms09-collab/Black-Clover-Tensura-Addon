"""The single entry point of the quest line spec: advancements (spec_adv) and the quest book (spec_quests), side by side.

Why: the owner's brief names one spec module as the source of truth. The data is split over small files by topic (magic table, advancement tree,
quest builders, one module per chapter group) because a single file of 700 advancements and 340 quests would be unreviewable; this module is
what the generators and the docs import, so there is still exactly one place that says what the line contains:

    ADVANCEMENTS.advs        every advancement (id, parent, title, icon, criteria)
    build_registry()         the quest groups, chapters, quests and reward tables
"""
from spec_adv import S as ADVANCEMENTS   # noqa: F401
from spec_quests import build_registry   # noqa: F401
