# Four Kingdoms menu (0.46)

## Where it lives

The menu takes over the grey **"?"** slot in Tensura's Magic tab (the hexagon of category icons on the left).

- **Which slot:** the CI inspection of Tensura's jar found that the slot is drawn by `io.github.manasmods.tensura.client.screen.AbilityCategoriesScreen`, using the shared texture constant `ScreenHelper.COMING_SOON_ICON`.
- **The hook (`mixin/client/FourKingdomsSlotMixin`):** every `ResourceLocation` blit in `GuiGraphics` ends in `innerBlit` (with or without a colour), so the mixin injects at the head of both.
  - When that exact texture is drawn while `AbilityCategoriesScreen` is open, `client/multiverse/FourKingdomsSlot` draws the logo in its place, records the slot in screen pixels, and cancels the original quad.
  - Every other draw passes through untouched.
- **Events (NeoForge `ScreenEvent`):**
  - Clicking the slot opens `FourKingdomsScreen`, and the click is consumed so Tensura doesn't also get it.
  - Hovering highlights the slot and shows a "Four Kingdoms" tooltip.
- **If Tensura changes:** the hook is optional (`require = 0`) and the icon constant is read by reflection. If either is missing, the screen stays exactly as Tensura draws it. The older "Multiverse" fallback button is still there.

## The menu (`client/multiverse/FourKingdomsScreen`)

- **Header:**
  - the Four Kingdoms Tensura logo;
  - the title;
  - your grimoire: its magic, cover, pages unlocked and mastery.
- **Status:** the "Status" button opens the full Multiverse status screen.
- **Grimoire:** your unlocked grimoire pages as an open book, one page each, with:
  - the page's name;
  - its page number, magicule cost and cooldown;
  - what it does (the page's lang description, with the leading "Name -" removed).
- **Data:** the server sends the list (`MultiverseSync` → `Grimoire.PageList`: id, name, lang key, cost, cooldown, page number). The client only displays it.
- **Turning pages:** the `<` / `>` buttons, clicking a page, the arrow keys or A/D, or the scroll wheel.
- **The page-flip animation (450 ms, smoothstep):**
  1. The leaf lifts at the spine.
  2. It foreshortens as it turns (horizontal scale `cos θ` from the spine), shading as it turns away from the light.
  3. Past 90° it shows its back, which is the next spread's left page, laying down on the other side.
  4. Meanwhile the page underneath is revealed with a fading shadow.

  It plays the book-page-turn sound, and turning back runs the same flip in reverse.

## The logo

- **Path:** `assets/nusmp/textures/gui/four_kingdoms_logo.png`. The slot icon and the menu header both draw it as a 256×256 square image.
- **What ships now:** the image attached in chat couldn't be saved into the workspace, so the shipped file is a stand-in emblem drawn after it (`tools/gen_menu_textures.py`):
  - a stone ring with runes;
  - a four-leaf clover of heart leaves (Clover green, Diamond blue, Heart green, Spade red);
  - the slime at the foot.
- **To use the real logo:** crop it to a square, scale it to 256×256 and save it over that file.
