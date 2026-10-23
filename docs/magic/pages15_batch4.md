# Pages 15, batch 4: Earth, Stone and Metal

Owner goal: every grimoire has 15 spells. This batch owns these books (and no other): `book_mercury`, `book_sand`, `book_steel`, `book_rock`, `book_red_ochre`, `book_stone`, `book_mineral`, `book_sandstone`, `book_earth`, `book_shakudo`, `book_crystal`, `book_glass`, `book_iron`, `book_copper`, `book_corundum`, `book_bronze`.

How to build a page: `docs/magic/pages15_recipe.md`. Files this batch edits: `src/main/java/com/newuniverse/nusmp/book/pages15/Batch4.java`, `tools/lang/pages15_batch4.json`,
`src/test/resources/armour_policy/pages15_batch4_new_pages.json` and this file.

## Pages

One table row per page, exactly this shape (the test `Pages15SourceScanTest` reads it): book, id, name, tier (starter / mid / zone / signature), origin (`original`, or
`canon: <source>` naming the English wiki page, the Japanese wiki page, or the chapter / episode; say which wiki each came from), then free columns (what it does, the shape and effect it reuses, worst case).

| Book | Id | Name | Tier | Origin | What it does | Reuses | Worst case |
|---|---|---|---|---|---|---|---|

## Research sources

(English wiki first, Japanese wiki as the fallback; list what could not be fetched.)

## Worst case of the batch (rule 11)

(Entities, packets per second, quads per frame, zones, targets: the figure per page is in `docs/magic/pages15_recipe.md`; state any page that goes beyond it and why it is still bounded.)

## New and replaced (rule 5)

(All pages of this batch are NEW; nothing is replaced. List any exception.)
