# Store and draw weapons (Grimoire)

Every grimoire (except the Forbidden one) ends with two pages, **Store Weapon** and **Draw Weapon** (`book/GrimoireBook.java`, logic in `anim/WeaponStore.java`).

- **Store Weapon**: the weapon in your main hand is saved into the book (`ItemStack.save`, all its enchantments and data kept) and the sword draw animation plays backwards, the blade sinking into the pages.
- **Draw Weapon**: the last stored weapon comes back out with the normal draw animation. If your hand is full it drops to your inventory.
- **Yami's Grimoire** (`CanonBook.YAMI`, Dark Magic) now gives the same sword draw as Anti-Magic and Sword Magic, with its own dark palette, and hands over Yami's blade (the Miasma Infused Katana).
- Both pages are free and instant (no mana, no cooldown). They are appended last, so no existing page index moves.

Not verified in game: only parse-checked and compiled by CI.
