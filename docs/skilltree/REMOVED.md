# The Skill Tree was removed (1.96.0)

The Skill Tree system (routes, Skill Points, the tree screen with its nodes and backgrounds, the Arts bar, the Passives panel, the nested panel in Tensura's Ability Selection screen, the route-unlocking books,
the SP Books, the `/multiverse tree` commands and its keybinds, packets and config, and the self-tests `tree.*`) and every Fairy Tail magic of the skill book system (29 books and their 30 technique skills) were
removed at the owner's request: a Skill Book now teaches a real Tensura Unique skill directly. The line data (`data/nusmp/skill_trees`), the line engine (arts and passives), the loot, the rift books and the Black
Market stayed and were rewired; the full description of what exists now, what the migration does and what could not be checked is in [`docs/skillbooks_unique.md`](../skillbooks_unique.md). The old design documents
of the tree (design, core, gui, items, shop, lines, the four Fairy Tail route files, the Tensura menu panel) are in the git history before 1.96.0.
