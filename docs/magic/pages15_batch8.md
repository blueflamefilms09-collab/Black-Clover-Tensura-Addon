# Pages 15, batch 8: Seal, Transform and Body

Owner goal: every grimoire has 15 spells. This batch owns these books (and no other): `book_sealing`, `book_skin`, `book_tongue`, `book_switching`, `book_recombination`, `book_transmutation`, `book_butoh`, `book_modification`, `book_hair`, `book_nail`, `book_scale`, `book_spike`, `book_chain`, `book_barrier`.

How to build a page: `docs/magic/pages15_recipe.md`. Files this batch edits: `src/main/java/com/newuniverse/nusmp/book/pages15/Batch8.java`, `tools/lang/pages15_batch8.json`,
`src/test/resources/armour_policy/pages15_batch8_new_pages.json` and this file.

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
