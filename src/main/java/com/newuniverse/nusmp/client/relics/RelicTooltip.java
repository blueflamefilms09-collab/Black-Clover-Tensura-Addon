package com.newuniverse.nusmp.client.relics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.newuniverse.nusmp.client.relics.RelicTooltipLayout.Layout;
import com.newuniverse.nusmp.client.relics.RelicTooltipLayout.Para;
import com.newuniverse.nusmp.client.relics.RelicTooltipLayout.Row;
import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.Attune;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.Passive;
import com.newuniverse.nusmp.relics.RelicBearer;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicEngine;
import com.newuniverse.nusmp.relics.RelicSlot;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.UseRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The relic tooltip, built so it can never leave the screen: a component slipped in after the item's name that wraps its text to 45% of the screen
 * width (at most 280 px), shrinks the text to 75% if it is too tall, and past that cuts it into pages the mouse wheel turns ("Page 2/4").
 * It shows the grade with its stars in the grade's colour, the slot, the attunement state, a short summary, the abilities with their costs and how to
 * use them, and the passives; the full card text sits behind SHIFT. An unattuned relic that needs attunement shows only a faint line (its power sleeps).
 * The layout arithmetic is {@link RelicTooltipLayout}.
 */
public final class RelicTooltip {
    private RelicTooltip() {}

    /** The tooltip component of a relic stack; the client component that draws it is registered in {@link #factories}. */
    public record Data(ItemStack stack, RelicDef def, int maxWidth, int maxHeight) implements TooltipComponent {}

    static final int LINE = 10;
    private static final int BODY = 0, GRADE = 1, SLOT = 2, ATTUNED = 3, HINT_ATTUNE = 4, WARN = 5, ACT = 6, DETAIL = 7, PASSIVE = 8, HINT = 9, LONG = 10, DIM = 11;

    private static int page, pages = 1;
    private static String lastKey = "";
    private static long lastGather;

    // ---------------------------------------------------------------- registration
    /** Mod bus. */
    public static void factories(RegisterClientTooltipComponentFactoriesEvent e) { e.register(Data.class, Client::new); }

    /** Game bus: puts the component into the tooltip of every relic stack. */
    public static void onGather(RenderTooltipEvent.GatherComponents e) {
        ItemStack stack = e.getItemStack();
        RelicDef def = Relics.defOf(stack);
        if (def == null) return;
        int maxWidth = Mth.clamp((int) (e.getScreenWidth() * 0.45), 120, 280);
        e.setMaxWidth(maxWidth);
        List<Either<FormattedText, TooltipComponent>> els = e.getTooltipElements();
        if (!els.isEmpty() && !(stack.getItem() instanceof RelicBearer) && els.get(0).left().orElse(null) instanceof Component name) {
            els.set(0, Either.left(name.copy().withStyle(def.grade().colour())));              // an item the mod already had: its name in the grade's colour
        }
        int others = Math.max(0, els.size() - 1);
        int maxHeight = Math.max(60, e.getScreenHeight() - 28 - others * LINE);
        els.add(Math.min(1, els.size()), Either.right(new Data(stack, def, maxWidth, maxHeight)));
        String key = def.id() + "|" + Screen.hasShiftDown();
        if (!key.equals(lastKey)) {
            lastKey = key;
            page = 0;
        }
        lastGather = System.nanoTime();
    }

    /** Game bus: the wheel turns the pages of a paged relic tooltip instead of scrolling the screen underneath. */
    public static void onScroll(ScreenEvent.MouseScrolled.Pre e) {
        if (pages <= 1 || System.nanoTime() - lastGather > 150_000_000L) return;
        double dy = e.getScrollDeltaY();
        if (dy == 0) return;
        page = RelicTooltipLayout.clampPage(page + (dy < 0 ? 1 : -1), pages);
        e.setCanceled(true);
    }

    // ---------------------------------------------------------------- the component
    private static final class Client implements ClientTooltipComponent {
        private final Data d;
        private Layout layout;

        Client(Data d) { this.d = d; }

        private Layout layout() {
            if (layout == null) {
                Font font = Minecraft.getInstance().font;
                layout = RelicTooltipLayout.layout(content(d, Screen.hasShiftDown()), font::width, d.maxWidth(), d.maxHeight(), LINE, 0.75f);
            }
            return layout;
        }

        @Override public int getHeight() { return layout().height() + 2; }

        @Override public int getWidth(Font font) { return Math.max(48, layout().width()); }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics g) {
            Layout l = layout();
            RelicTooltip.pages = l.pageCount();
            RelicTooltip.page = RelicTooltipLayout.clampPage(RelicTooltip.page, l.pageCount());
            List<Row> rows = l.pages().get(RelicTooltip.page);
            PoseStack pose = g.pose();
            pose.pushPose();
            pose.translate(x, y + 1, 0);
            pose.scale(l.scale(), l.scale(), 1f);
            int yy = 0;
            for (Row r : rows) {
                if (!r.blank()) g.drawString(font, r.text(), r.indent(), yy, color(d.def(), r.tag()), true);
                yy += LINE;
            }
            if (l.pageCount() > 1) {
                String foot = RelicTooltipLayout.footer(RelicTooltip.page, l.pageCount()) + "  (mouse wheel)";
                g.drawString(font, foot, 0, l.pageRows() * LINE, 0xFF9A9AA8, true);
            }
            pose.popPose();
        }
    }

    private static int color(RelicDef def, int tag) {
        return switch (tag) {
            case GRADE -> 0xFF000000 | (def.grade() == Grade.GENESIS ? rainbow() : def.grade().rgb());
            case SLOT -> 0xFF8899AA;
            case ATTUNED -> 0xFF55FFFF;
            case HINT_ATTUNE -> 0xFFCC99FF;
            case WARN -> 0xFFFF6666;
            case ACT -> 0xFFFFD36B;
            case DETAIL -> 0xFFA0A6B2;
            case PASSIVE -> 0xFF7CE08A;
            case HINT -> 0xFF8A8A96;
            case LONG -> 0xFFDCDCE4;
            case DIM -> 0xFF777788;
            default -> 0xFFC8C8D0;
        };
    }

    private static int rainbow() {
        long t = System.currentTimeMillis() % 4000L;
        return java.awt.Color.HSBtoRGB(t / 4000f, 0.35f, 1f) & 0xFFFFFF;
    }

    // ---------------------------------------------------------------- what it says
    private static String text(RelicDef def, String suffix, String fallback) {
        String key = "item.nusmp." + def.id() + suffix;
        Language lang = Language.getInstance();
        return lang.has(key) ? lang.getOrDefault(key) : fallback;
    }

    static List<Para> content(Data d, boolean shift) {
        RelicDef def = d.def();
        ItemStack stack = d.stack();
        Minecraft mc = Minecraft.getInstance();
        List<Para> out = new ArrayList<>();
        Grade g = def.grade();
        out.add(new Para(g.stars() + "  " + g.label() + (def.isVessel() ? "  (can hold a Djinn)" : ""), GRADE));
        out.add(new Para("Slot: " + def.slot().label() + (def.slot().isCurio() ? " (Curios)" : ""), SLOT));
        boolean mine = mc.player != null && Attune.isMine(stack, mc.player);
        if (def.needsAttunement()) {
            UUID owner = Attune.owner(stack);
            if (mine) out.add(new Para("Attuned to you", ATTUNED));
            else if (owner != null) out.add(new Para("Attuned to " + nameOf(owner) + ": it does not answer you", WARN));
            else out.add(new Para("Requires attunement: sneak and hold use for 5 seconds", HINT_ATTUNE));
        }
        String summary = text(def, ".desc", def.desc());
        if (def.needsAttunement() && !mine) {                                              // a faint tooltip: its power sleeps
            out.add(new Para("", BODY));
            if (!summary.isEmpty()) out.add(new Para(summary, DIM));
            out.add(new Para("Its power sleeps until it is attuned.", DIM));
            return out;
        }
        out.add(new Para("", BODY));
        if (shift) {
            String full = text(def, ".card", summary);
            if (!full.isEmpty()) out.add(new Para(full, LONG));
        } else if (!summary.isEmpty()) {
            out.add(new Para(summary, BODY));
        }
        if (g.epBonus() > 0) out.add(new Para("While worn: existence points +" + trim(g.epBonus() * 100) + "%", PASSIVE));
        acts(def, stack, mc, out);
        passives(def, out);
        String full = text(def, ".card", "");
        if (!shift && !full.isEmpty() && !full.equals(summary)) {
            out.add(new Para("", BODY));
            out.add(new Para("Hold SHIFT for the full text", HINT));
        }
        return out;
    }

    private static void acts(RelicDef def, ItemStack stack, Minecraft mc, List<Para> out) {
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        int i = 0;
        for (Act a : def.acts()) {
            if (i == 0) out.add(new Para("", BODY));
            out.add(new Para("▸ " + a.displayName(), ACT));
            StringBuilder b = new StringBuilder();
            b.append(how(def, i)).append(". ");
            if (a.manaPercent() > 0) b.append("Mana ").append(a.manaPercent()).append("%. ");
            if (a.epPercentCost() > 0) b.append("Existence ").append(trim(a.epPercentCost())).append("%. ");
            if (a.cooldownTicks() > 0) b.append("Cooldown ").append(UseRules.span(a.cooldownTicks())).append(". ");
            if (a.usesPerDay() > 0) b.append(a.usesPerDay()).append(" a day (").append(Math.min(a.usesPerDay(), RelicEngine.usesLeft(stack, a, now))).append(" left). ");
            if (a.holdTicks() > 0) b.append("Hold ").append(UseRules.span(a.holdTicks())).append(". ");
            if (a.sneakRequired()) b.append("Sneak. ");
            if (a.toggles()) b.append("Toggle. ");
            int left = RelicClientState.remaining(def.id(), a.key(), now);
            if (left > 0) b.append("Ready in ").append(UseRules.span(left)).append('.');
            out.add(new Para(b.toString().trim(), DETAIL, 8));
            i++;
        }
    }

    /** How the act at this index is used. */
    static String how(RelicDef def, int index) {
        if (def.slot() == RelicSlot.CONSUMABLE) return "Use it";
        if (def.slot().isHand() && def.isRanged() && index == Math.min(1, def.acts().size() - 1)) return "Sneak + use";      // a drawn weapon: use means draw, so its one click is sneak + use
        if (def.slot().isHand() && !def.isAdopted() && !def.isRanged() && index == 0) return "Right-click";
        if (def.slot().isHand() && !def.isAdopted() && !def.isRanged() && index == 1) return "Sneak + right-click";
        if (def.slot().isArmor() || def.slot().isCurio() || def.slot().isHand()) return "Key " + RelicActKeys.USE.getTranslatedKeyMessage().getString();
        return "Use it";
    }

    private static void passives(RelicDef def, List<Para> out) {
        for (Passive p : def.passives()) {
            String line = passive(p);
            if (line != null) out.add(new Para(line, PASSIVE));
        }
    }

    private static String passive(Passive p) {
        ResourceLocation id = ResourceLocation.tryParse(p.key());
        switch (p.kind()) {
            case ATTR: {
                String name = id == null ? p.key() : BuiltInRegistries.ATTRIBUTE.getOptional(id).map(a -> I18n.get(a.getDescriptionId())).orElse(humanize(p.key()));
                boolean pct = p.operation() != AttributeModifier.Operation.ADD_VALUE;
                double v = pct ? p.amount() * 100 : p.amount();
                return (v >= 0 ? "+" : "") + trim(v) + (pct ? "% " : " ") + name;
            }
            case EFFECT: {
                String name = id == null ? p.key() : BuiltInRegistries.MOB_EFFECT.getOptional(id).map(e -> e.getDisplayName().getString()).orElse(humanize(p.key()));
                return name + " " + roman(p.amp() + 1);
            }
            case RESIST:
                return "Resists " + humanize(p.key()) + " " + Math.round(p.amount() * 100) + "%";
            case SKILL:
                return "Lends the skill " + humanize(p.key());
            default:
                return null;
        }
    }

    // ---------------------------------------------------------------- small helpers
    private static String nameOf(UUID id) {
        ClientPacketListener c = Minecraft.getInstance().getConnection();
        var info = c == null ? null : c.getPlayerInfo(id);
        return info == null ? "another player" : info.getProfile().getName();
    }

    /** "tensura:magic_sense" or "minecraft:is_fire" as "Magic Sense" / "Is Fire". */
    static String humanize(String id) {
        String s = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        s = s.replace("generic.", "").replace('_', ' ').replace('.', ' ').replace('/', ' ');
        StringBuilder b = new StringBuilder();
        for (String w : s.split(" ")) {
            if (w.isEmpty()) continue;
            if (b.length() > 0) b.append(' ');
            b.append(w.substring(0, 1).toUpperCase(Locale.ROOT)).append(w.substring(1));
        }
        return b.toString();
    }

    static String trim(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) return Long.toString(Math.round(v));
        return String.format(Locale.ROOT, "%.1f", v);
    }

    static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n >= 0 && n < r.length ? r[n] : Integer.toString(n);
    }
}
