package com.newuniverse.nusmp.client.ui;

import com.newuniverse.nusmp.client.multiverse.MultiverseStatusScreen;
import com.newuniverse.nusmp.item.MagicWeaponItem;
import com.newuniverse.nusmp.item.NUItems;
import com.newuniverse.nusmp.item.RelicItem;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * 0.100: the registry and the stock pages of the Codex. To add a page to the book, write a {@link CodexPage} (or a {@link TextPage} /
 * {@link CatalogPage}, which cover most needs) and {@link #register} it; the tabs, the page turn, scrolling and clipping come with it.
 */
public final class CodexPages {
    private CodexPages() {}

    private static final List<CodexPage> PAGES = new ArrayList<>();

    /** Appends a page (pages only ever go at the end). */
    public static void register(CodexPage page) { PAGES.add(page); }

    public static List<CodexPage> all() {
        if (PAGES.isEmpty()) defaults();
        return PAGES;
    }

    private static void defaults() {
        register(new OverviewPage());
        register(new StatusPage());
        register(new CatalogPage("Weapons", "W", i -> i instanceof MagicWeaponItem));
        register(new CatalogPage("Relics", "R", i -> i instanceof RelicItem));
        register(new TextPage("Bestiary", "B", List.of(
                new String[]{"Zagred, the Word-Eater", "A boss of spoken words, barrier layers and a dream dimension. Drops the Heart of Words and one of: Last Word, Shroud of Margins, Circlet of Quickened Thought."},
                new String[]{"Marquis Remake, the Black Bulls' Bard", "Counter-picks your magic: New Order, Gearshift, Compress, Evil Eye and one god avatar at a time. Anti-magic staggers him. Drops the Black Bull Bard Relic (sneak-use: Bardic Inspiration), a Story Page, his Crest Cape token and, rarely, the Final Crown."},
                new String[]{"Sylph, the Wind Spirit Lord", "A wind spirit of the gale. Drops the Gale Feather (use: Gale Leap)."})));
        register(new TextPage("Spirit Lords", "L", List.of(
                new String[]{"Salamander", "A winged fire dragon. Flame Breath burns everything in a cone; its master resists fire."},
                new String[]{"Undine", "A woman of water. Tidal Lance slows foes, and she mends her master when they are hurt."},
                new String[]{"Sylph", "A green fairy on dragonfly wings. Gale Cut throws foes into the air; her master moves faster and lands softly."},
                new String[]{"Gnome", "A hunched stone giant. Earth Slam raises stone claws round a foe; his master takes less damage."},
                new String[]{"One mage each", "As in Black Clover, each elemental Spirit Lord answers to a single mage on the server. At 100 Trust and with a True Name, sneak-use the Spirit Charm to call yours."},
                new String[]{"Tensura spirits", "Anyone can call a Tensura-style spirit: a small floating orb that follows you. Use the Spirit Charm to pick its element, sneak-use to call it."})));
        register(new TextPage("Guide", "?", List.of(
                new String[]{"Turning pages", "Use the mouse wheel over the book, the arrow keys or A and D, or click a tab. Long pages scroll with the wheel; hold Shift to turn the page instead."},
                new String[]{"The figure", "Click the figure for a new pose, drag it to turn it."},
                new String[]{"Hotkey", "The Codex opens with the Multiverse Status key (Controls, Multiverse Status)."})));
    }

    // ---------------------------------------------------------------- overview
    /** The first spread: the player figure on the left, the magic this mod has given them on the right. */
    static final class OverviewPage implements CodexPage {
        private List<String[]> cache = List.of();
        private long cacheAt = -1;

        @Override public String title() { return "You"; }
        @Override public String glyph() { return "*"; }

        /** {id path, pretty name, kind} of every skill from this mod the player has. */
        List<String[]> skills(CodexContext ctx) {
            long now = ctx.mc.level == null ? 0 : ctx.mc.level.getGameTime();
            if (now - cacheAt < 20 && cacheAt >= 0) return cache;
            List<String[]> out = new ArrayList<>();
            try {
                var skills = SkillAPI.getSkillsFrom(ctx.player());
                if (skills != null) {
                    for (ManasSkillInstance i : new ArrayList<>(skills.getLearnedSkills())) {
                        ResourceLocation id = i.getSkill().getRegistryName();
                        if (id == null || !"nusmp".equals(id.getNamespace())) continue;
                        String kind = id.getPath().contains("grimoire") ? "Grimoire magic" : "Skill";
                        out.add(new String[]{id.getPath(), pretty(id.getPath()), kind});
                    }
                }
            } catch (Throwable ignored) {}
            out.sort((a, b) -> a[2].equals(b[2]) ? a[1].compareTo(b[1]) : a[2].compareTo(b[2]));
            cache = out;
            cacheAt = now;
            return out;
        }

        @Override
        public int contentHeight(CodexContext ctx, int width) { return 24 + skills(ctx).size() * 22 + 30; }

        @Override
        public void render(CodexContext ctx, GuiGraphics g, int x, int y, int w, int h, int scroll, int mx, int my, float time) {
            Font font = ctx.font();
            int half = w / 2 - 8;
            // left: the figure on a small stage
            int fx = x + half / 2, fy = y + h / 2 + 4, fh = Math.min(h - 40, 170);
            g.fill(x + 4, y + h - 28, x + half - 4, y + h - 26, UiDraw.withAlpha(UiDraw.RULE, 140));          // the floor line
            if (ctx.player() != null) UiDraw.player(g, ctx.player(), fx, fy, fh, ctx.figureYaw);
            ctx.figureX = x; ctx.figureY = y; ctx.figureW = half; ctx.figureH = h;
            String name = ctx.player() == null ? "" : ctx.player().getName().getString();
            g.drawCenteredString(font, name, fx, y + 2, UiDraw.INK);
            g.drawCenteredString(font, UiPoses.current().caption(), fx, y + h - 20, UiDraw.INK_DIM);
            // right: the magic (scrolls)
            int rx = x + w / 2 + 8, rw = w / 2 - 8;
            g.enableScissor(rx - 2, y, rx + rw + 2, y + h);
            int cy = y - scroll;
            cy = UiDraw.header(g, font, "Your magic", rx, cy, rw - 6);
            List<String[]> list = skills(ctx);
            if (list.isEmpty()) cy = UiDraw.wrapped(g, font, "Nothing from this mod yet. Take a grimoire at a Grimoire Tower to begin.", rx, cy, rw - 8, UiDraw.INK_DIM);
            String last = "";
            for (String[] s : list) {
                if (!s[2].equals(last)) { last = s[2]; cy += 2; g.drawString(font, s[2].toUpperCase(Locale.ROOT), rx, cy, UiDraw.INK_DIM, false); cy += font.lineHeight + 3; }
                g.fill(rx, cy + 1, rx + 3, cy + 15, UiDraw.ACCENT);
                g.drawString(font, font.plainSubstrByWidth(s[1], rw - 14), rx + 8, cy + 3, UiDraw.INK, false);
                cy += 20;
            }
            g.disableScissor();
        }

        @Override
        public boolean click(CodexContext ctx, double mx, double my, int button, int x, int y, int w, int h) {
            if (mx >= ctx.figureX && mx <= ctx.figureX + ctx.figureW && my >= ctx.figureY && my <= ctx.figureY + ctx.figureH) {
                UiPoses.roll();
                return true;
            }
            return false;
        }
    }

    static String pretty(String path) {
        StringBuilder b = new StringBuilder();
        for (String part : path.replace("grimoire_", "").split("_")) {
            if (part.isEmpty()) continue;
            if (b.length() > 0) b.append(' ');
            b.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return b.toString();
    }

    // ---------------------------------------------------------------- status (the old status screen's blocks)
    static final class StatusPage implements CodexPage {
        @Override public String title() { return "Status"; }
        @Override public String glyph() { return "S"; }

        private int colW(int w) { return w / 2 - 8; }

        @Override
        public int contentHeight(CodexContext ctx, int width) {
            int[] col = new int[2];
            int i = 0;
            for (String[] b : MultiverseStatusScreen.summary(ctx.status())) {
                int hgt = 16 + UiDraw.wrappedHeight(ctx.font(), b[1], colW(width) - 6) + 10;
                col[i % 2] += hgt;
                i++;
            }
            return Math.max(col[0], col[1]) + 6;
        }

        @Override
        public void render(CodexContext ctx, GuiGraphics g, int x, int y, int w, int h, int scroll, int mx, int my, float time) {
            Font font = ctx.font();
            int cw = colW(w);
            int[] cy = {y - scroll, y - scroll};
            int i = 0;
            for (String[] b : MultiverseStatusScreen.summary(ctx.status())) {
                int col = i % 2;
                int bx = col == 0 ? x : x + w / 2 + 8;
                int ny = UiDraw.header(g, font, b[0], bx, cy[col], cw - 6);
                ny = UiDraw.wrapped(g, font, b[1], bx + 2, ny, cw - 10, "warn".equals(b[2]) ? UiDraw.WARN : UiDraw.INK);
                cy[col] = ny + 8;
                i++;
            }
        }
    }

    // ---------------------------------------------------------------- generic text and item catalogues
    /** A page of titled paragraphs, laid out in two columns. */
    public static final class TextPage implements CodexPage {
        private final String title, glyph;
        private final List<String[]> entries;

        public TextPage(String title, String glyph, List<String[]> entries) { this.title = title; this.glyph = glyph; this.entries = entries; }

        @Override public String title() { return title; }
        @Override public String glyph() { return glyph; }

        @Override
        public int contentHeight(CodexContext ctx, int width) {
            int[] col = new int[2];
            for (int i = 0; i < entries.size(); i++) col[i % 2] += 18 + UiDraw.wrappedHeight(ctx.font(), entries.get(i)[1], width / 2 - 16) + 10;
            return Math.max(col[0], col[1]);
        }

        @Override
        public void render(CodexContext ctx, GuiGraphics g, int x, int y, int w, int h, int scroll, int mx, int my, float time) {
            int cw = w / 2 - 8;
            int[] cy = {y - scroll, y - scroll};
            for (int i = 0; i < entries.size(); i++) {
                int col = i % 2, bx = col == 0 ? x : x + w / 2 + 8;
                int ny = UiDraw.header(g, ctx.font(), entries.get(i)[0], bx, cy[col], cw - 6);
                cy[col] = UiDraw.wrapped(g, ctx.font(), entries.get(i)[1], bx + 2, ny, cw - 10, UiDraw.INK) + 8;
            }
        }
    }

    /** Every item of this mod that matches a filter: its icon, its name and the first lines of its tooltip. Grows with the mod. */
    public static final class CatalogPage implements CodexPage {
        private final String title, glyph;
        private final Predicate<Item> filter;
        private List<Entry> entries;
        private int builtWidth = -1;

        record Entry(ItemStack stack, String name, List<String> lines) {}

        public CatalogPage(String title, String glyph, Predicate<Item> filter) { this.title = title; this.glyph = glyph; this.filter = filter; }

        @Override public String title() { return title; }
        @Override public String glyph() { return glyph; }

        private void build(CodexContext ctx) {
            if (entries != null) return;
            entries = new ArrayList<>();
            for (var holder : NUItems.all()) {
                Item item = holder.get();
                if (!filter.test(item)) continue;
                ItemStack st = new ItemStack(item);
                List<String> lines = new ArrayList<>();
                try {
                    List<Component> tip = st.getTooltipLines(Item.TooltipContext.of(ctx.mc.level), ctx.mc.player, TooltipFlag.NORMAL);
                    for (int k = 1; k < tip.size() && lines.size() < 40; k++) {
                        String t = tip.get(k).getString();
                        if (!t.isBlank()) lines.add(t.stripTrailing());
                    }
                } catch (Throwable ignored) {}
                entries.add(new Entry(st, st.getHoverName().getString(), lines));
            }
        }

        private int entryHeight(CodexContext ctx, Entry e, int cw) {
            int hgt = 22;
            for (String l : e.lines()) hgt += UiDraw.wrappedHeight(ctx.font(), l.strip(), cw - 8 - (l.startsWith(" ") ? 8 : 0));
            return hgt + 8;
        }

        @Override
        public int contentHeight(CodexContext ctx, int width) {
            build(ctx);
            int[] col = new int[2];
            for (int i = 0; i < entries.size(); i++) col[i % 2] += entryHeight(ctx, entries.get(i), width / 2 - 8);
            return Math.max(col[0], col[1]);
        }

        @Override
        public void render(CodexContext ctx, GuiGraphics g, int x, int y, int w, int h, int scroll, int mx, int my, float time) {
            build(ctx);
            Font font = ctx.font();
            int cw = w / 2 - 8;
            int[] cy = {y - scroll, y - scroll};
            for (int i = 0; i < entries.size(); i++) {
                int col = i % 2, bx = col == 0 ? x : x + w / 2 + 8;
                Entry e = entries.get(i);
                int top = cy[col];
                if (top + entryHeight(ctx, e, cw) > y && top < y + h) {                            // only what is on the page is drawn
                    g.renderItem(e.stack(), bx, top);
                    g.drawString(font, font.plainSubstrByWidth(e.name(), cw - 26), bx + 20, top + 4, UiDraw.GOLD_INK, false);
                    g.hLine(bx, bx + cw - 6, top + 18, UiDraw.RULE);
                    int ly = top + 22;
                    for (String l : e.lines()) {
                        int in = l.startsWith(" ") ? 8 : 0;                    // the tooltip's indented lines stay indented, wrapped under themselves
                        ly = UiDraw.wrapped(g, font, l.strip(), bx + 2 + in, ly, cw - 12 - in, in > 0 ? UiDraw.INK_DIM : UiDraw.INK);
                    }
                }
                cy[col] += entryHeight(ctx, e, cw);
            }
        }
    }
}
