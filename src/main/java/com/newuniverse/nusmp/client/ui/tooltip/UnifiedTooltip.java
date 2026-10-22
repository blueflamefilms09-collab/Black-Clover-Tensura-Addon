package com.newuniverse.nusmp.client.ui.tooltip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.newuniverse.nusmp.client.relics.RelicTooltipLayout;
import com.newuniverse.nusmp.client.relics.RelicTooltipLayout.Layout;
import com.newuniverse.nusmp.client.relics.RelicTooltipLayout.Para;
import com.newuniverse.nusmp.client.relics.RelicTooltipLayout.Row;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.tooltip.UnifiedTooltipItems;
import com.newuniverse.nusmp.tooltip.UnifiedTooltipRules;
import com.newuniverse.nusmp.tooltip.UnifiedTooltipRules.Style;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

/**
 * Why: the owner wants every item of the mod to carry the tooltip the relics have. A relic's tooltip is: its name in the grade's colour, a banner row (the grade's stars and
 * label) with a hairline under it, a type row, the text, and (in the anime look) a dark glass frame whose border takes the grade's colour. This class gives that same layout to
 * every other item of the mod: the name in the grade's colour, the SAME banner row and hairline, a "Type:" row in the slot row's colour, the item's own lines kept exactly as
 * they were (the lore keeps its colours; the attribute block takes the relic palette: green for a bonus, red for a malus, slate for the "When in Main Hand" heading), a hairline
 * and a dim footer, and the same frame. Relics are skipped (their own tooltip is the model); spawn eggs are skipped; items of other mods are untouched. The decisions are
 * {@link UnifiedTooltipRules} and {@link UnifiedTooltipItems}; this class only draws. Client thread only, registered from {@code UiThemeClient}. Any fault leaves the vanilla
 * tooltip as it was.
 */
public final class UnifiedTooltip {
    private UnifiedTooltip() {}

    /** Which piece of the layout a component draws. */
    public static final int HEAD = 0, FOOT = 1;
    /** EX-Singularity (appended): a prismatic text row (the "Akashic Relic - Grade" line), and the head whose grade row is prismatic. */
    public static final int PRISM_LINE = 2, HEAD_PRISM = 3;

    private static final int LINE = 10, TAG_BANNER = 1, TAG_TYPE = 2, TAG_FOOT = 3, TAG_PRISM = 4;

    /** The component slipped into the tooltip. Plain data: two lines of text and the grade's colour. */
    public record Data(int part, String first, String second, int rgb, int maxWidth) implements TooltipComponent {}

    // ---------------------------------------------------------------- registration
    /** Called from {@code UiThemeClient.init}. */
    public static void init(IEventBus modBus) {
        modBus.addListener(UnifiedTooltip::factories);
        NeoForge.EVENT_BUS.addListener(UnifiedTooltip::onGather);
        NeoForge.EVENT_BUS.addListener(UnifiedTooltip::onColor);
    }

    static void factories(RegisterClientTooltipComponentFactoriesEvent e) { e.register(Data.class, Client::new); }

    private static boolean warned;

    // ---------------------------------------------------------------- building the tooltip
    static void onGather(RenderTooltipEvent.GatherComponents e) {
        try {
            ItemStack stack = e.getItemStack();
            if (!UnifiedTooltipItems.applies(stack)) return;
            Style style = styleOf(stack);
            int maxWidth = UnifiedTooltipRules.maxWidth(e.getScreenWidth());
            e.setMaxWidth(maxWidth);
            List<Either<FormattedText, TooltipComponent>> els = e.getTooltipElements();
            els.removeIf(x -> x.right().orElse(null) instanceof Data);
            boolean akashic = style.akashic();                                                       // EX-Singularity: the name, the grade row and the grade line are prismatic
            if (!els.isEmpty() && els.get(0).left().orElse(null) instanceof Component name)
                els.set(0, Either.left(akashic ? PrismText.name(name.getString(), System.currentTimeMillis()) : recolourName(name, style.rgb())));
            for (int i = 1; i < els.size(); i++) {
                FormattedText text = els.get(i).left().orElse(null);
                if (!(text instanceof Component line)) continue;
                String key = keyOf(line);
                if (akashic && "shishou.grade".equals(key)) {                                      // the yellow "Akashic Relic - Grade: EX-Singularity" line becomes the prismatic one
                    els.set(i, Either.right(new Data(PRISM_LINE, line.getString(), "", style.rgb(), maxWidth)));
                    continue;
                }
                int c = UnifiedTooltipRules.statColour(key);
                if (c != 0) els.set(i, Either.left(line.copy().withColor(c & 0xFFFFFF)));
            }
            els.add(Math.min(1, els.size()), Either.right(new Data(akashic ? HEAD_PRISM : HEAD, style.banner(), style.typeLine(), style.rgb(), maxWidth)));
            Component franchise = com.newuniverse.nusmp.franchise.FranchiseEngravings.tooltipLine(stack);                // 1.111.0: the franchise line under the engraving list
            if (franchise != null) els.add(Either.left(franchise));
            els.add(Either.right(new Data(FOOT, style.footer(), "", style.rgb(), maxWidth)));
        } catch (RuntimeException | LinkageError failed) {                                            // never drop the tooltip: the vanilla lines stay
            if (!warned) {
                warned = true;
                com.mojang.logging.LogUtils.getLogger().error("[nusmp] the unified tooltip could not be built; showing the plain lines", failed);
            }
            try {
                e.getTooltipElements().removeIf(x -> x.right().orElse(null) instanceof Data);
            } catch (RuntimeException ignored) {
                // the vanilla lines are all that is left
            }
        }
    }

    /** The style of a stack with the category label in the player's language. */
    static Style styleOf(ItemStack stack) {
        UnifiedTooltipRules.Category c = UnifiedTooltipItems.categoryOf(stack.getItem());
        Language lang = Language.getInstance();
        String label = lang.has(c.langKey()) ? lang.getOrDefault(c.langKey()) : null;
        return UnifiedTooltipItems.styleOf(stack, label);
    }

    /** The name in the grade's colour, unless a part of it already has a colour of its own (a Djinn's name keeps its Djinn colour). */
    private static Component recolourName(Component name, int rgb) {
        boolean own = name.getSiblings().stream().anyMatch(s -> s.getStyle().getColor() != null);
        if (own) return name;
        return name.copy().withColor(rgb & 0xFFFFFF);
    }

    /** The lang key of a line (its own or its first part's), or null. */
    private static String keyOf(Component c) {
        if (c.getContents() instanceof TranslatableContents t) return t.getKey();
        for (Component s : c.getSiblings()) if (s.getContents() instanceof TranslatableContents t) return t.getKey();
        return null;
    }

    /** In the anime look: the relic tooltip's dark glass, the border in the grade's colour fading toward violet. */
    static void onColor(RenderTooltipEvent.Color e) {
        try {
            if (!UiTheme.anime() || !UnifiedTooltipItems.applies(e.getItemStack())) return;
            Style style = styleOf(e.getItemStack());
            if (style.akashic()) {                                                                  // EX-Singularity: a deep violet glass, the border a two-hue prism that circles
                long now = System.currentTimeMillis();
                e.setBackgroundStart(0xF2160C2E);
                e.setBackgroundEnd(0xF2080414);
                e.setBorderStart(0xFF000000 | com.newuniverse.nusmp.relics.ExSingularity.borderRgb(now, false));
                e.setBorderEnd(0xFF000000 | com.newuniverse.nusmp.relics.ExSingularity.borderRgb(now, true));
                return;
            }
            int c = 0xFF000000 | style.rgb();
            e.setBackgroundStart(0xF2101A30);
            e.setBackgroundEnd(0xF2070B16);
            e.setBorderStart(c);
            e.setBorderEnd(UiMath.mix(c, 0xFF3A2A8A, 0.6f));
        } catch (RuntimeException | LinkageError failed) {
            // the vanilla colours stay
        }
    }

    // ---------------------------------------------------------------- the component
    private static final class Client implements ClientTooltipComponent {
        private final Data d;
        private Layout layout;
        private PrismText.Glyphs[] prism;                                                     // EX-Singularity rows' glyph tables, built once with the layout

        Client(Data d) { this.d = d; }

        private Layout layout(Font font) {
            if (layout == null) {
                List<Para> paras = d.part() == HEAD || d.part() == HEAD_PRISM
                        ? List.of(new Para(d.first(), TAG_BANNER), new Para(d.second(), TAG_TYPE))
                        : d.part() == PRISM_LINE ? List.of(new Para(d.first(), TAG_PRISM)) : List.of(new Para(d.first(), TAG_FOOT));
                layout = RelicTooltipLayout.layout(paras, font::width, d.maxWidth(), 1000, LINE, 1f);
            }
            return layout;
        }

        @Override public int getHeight() { return layout == null ? (d.part() == HEAD || d.part() == HEAD_PRISM ? 2 : 1) * LINE + 3 : layout.pages().get(0).size() * LINE + 3; }

        @Override public int getWidth(Font font) { return Math.max(48, layout(font).width()); }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics g) {
            Layout l = layout(font);
            PoseStack pose = g.pose();
            pose.pushPose();
            try {
                boolean anime = UiTheme.anime();
                int w = Math.max(48, l.width());
                if (d.part() == PRISM_LINE) {                                                            // the prismatic grade line
                    List<Row> rows = l.pages().get(0);
                    if (prism == null) {
                        prism = new PrismText.Glyphs[rows.size()];
                        for (int i = 0; i < prism.length; i++) prism[i] = PrismText.of(font, rows.get(i).text());
                    }
                    long now = System.currentTimeMillis();
                    int yy = y + 1;
                    for (int i = 0; i < rows.size(); i++) {
                        PrismText.draw(g, font, prism[i], x + rows.get(i).indent(), yy, now, true);
                        yy += LINE;
                    }
                } else if (d.part() == FOOT) {
                    if (anime) g.fill(x, y + 1, x + w, y + 2, UiMath.withAlpha(0xFF000000 | d.rgb(), 0.30f));          // a hairline above the footer
                    int yy = y + 3;
                    for (Row r : l.pages().get(0)) {
                        g.drawString(font, r.text(), x + r.indent(), yy, UnifiedTooltipRules.COLOUR_FOOTER, true);
                        yy += LINE;
                    }
                } else {
                    int yy = y + 1;
                    List<Row> rows = l.pages().get(0);
                    if (d.part() == HEAD_PRISM && prism == null) {
                        prism = new PrismText.Glyphs[rows.size()];
                        for (int i = 0; i < prism.length; i++) prism[i] = PrismText.of(font, rows.get(i).text());
                    }
                    long now = d.part() == HEAD_PRISM ? System.currentTimeMillis() : 0L;
                    for (int i = 0; i < rows.size(); i++) {
                        Row r = rows.get(i);
                        boolean banner = r.tag() == TAG_BANNER;
                        if (banner && d.part() == HEAD_PRISM) PrismText.draw(g, font, prism[i], x + r.indent(), yy, now, true);                  // EX-Singularity: every glyph its own hue, a glint running along
                        else g.drawString(font, r.text(), x + r.indent(), yy, banner ? 0xFF000000 | d.rgb() : UnifiedTooltipRules.COLOUR_TYPE, true);
                        if (banner && anime) g.fill(x, yy + LINE - 1, x + w, yy + LINE, UiMath.withAlpha(0xFF000000 | (d.part() == HEAD_PRISM ? com.newuniverse.nusmp.relics.ExSingularity.rowRgb(now, 0f) : d.rgb()), 0.45f));      // the hairline under the grade row
                        yy += LINE;
                    }
                }
            } catch (RuntimeException failed) {
                // a row that cannot be drawn is skipped; the tooltip stays
            } finally {
                pose.popPose();
            }
        }
    }
}
