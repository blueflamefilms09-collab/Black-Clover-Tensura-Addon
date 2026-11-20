package com.newuniverse.nusmp.client.hub;

import com.newuniverse.nusmp.client.compat.CompatCodexScreen;
import com.newuniverse.nusmp.client.elite.hub.HubClient;
import com.newuniverse.nusmp.client.multiverse.FourKingdomsScreen;
import com.newuniverse.nusmp.client.multiverse.MultiverseStatusClient;
import com.newuniverse.nusmp.client.multiverse.MultiverseStatusScreen;
import com.newuniverse.nusmp.client.ui.GuiSizeScreen;
import com.newuniverse.nusmp.client.ui.move.GuiMoveScreen;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.client.ui.theme.UiThemeOptions;
import com.newuniverse.nusmp.elite.hub.HubNet;
import com.newuniverse.nusmp.elite.hub.QuestText;
import io.github.manasmods.tensura.client.screen.AbilityCategoriesScreen;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: the data of the list pages of the hub (Status, Quests, Multiverse, Settings, and the lock page), read from what the client already holds and cooked into a {@link HubListBody} once per open or refresh (never
 * per frame). Status reads the status NBT the status screen already receives (through {@link MultiverseStatusScreen#summary}), the synced attributes of the player (EP, magicule, aura) and the player's own active
 * effects; Quests reads the quest tracker the server already sends and the last dungeon map; Multiverse and Settings are rows that open the mod's existing screens with the hub as their parent. Nothing here sends
 * a packet, decides a rule or keeps state between calls.
 */
final class HubCards {
    private HubCards() {}

    private static String tr(String key, Object... args) { return I18n.get("anime_multiverse.hub." + key, args); }

    private static String num(double v) { return String.format(Locale.ROOT, "%,d", (long) Math.max(0, v)); }

    private static double safe(java.util.function.ToDoubleFunction<LocalPlayer> f, LocalPlayer p) {
        try { return f.applyAsDouble(p); } catch (Throwable noTensuraData) { return 0.0; }
    }

    // ---------------------------------------------------------------- Status
    static HubListBody status(Font font, int w) {
        HubListBody.Builder b = new HubListBody.Builder(font, w);
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) {
            b.card(UiTheme.CYAN, tr("status.power"), List.of(tr("status.ep", num(safe(EnergyHelper::getMaxEP, p))),
                    tr("status.magicule", num(safe(EnergyHelper::getMaxMagicule, p))) + "   " + tr("status.aura", num(safe(EnergyHelper::getMaxAura, p)))), -1f, null);
        }
        for (String[] s : MultiverseStatusScreen.summary(MultiverseStatusClient.status())) {
            List<String> lines = new ArrayList<>();
            for (String l : s[1].split("\n")) lines.add(l.strip());
            b.card("warn".equals(s[2]) ? UiTheme.RED : UiTheme.GOLD, s[0], lines, -1f, null);
        }
        List<String> fx = new ArrayList<>();
        if (p != null) {
            int n = 0;
            for (MobEffectInstance e : p.getActiveEffects()) {
                if (n++ >= 10) {
                    fx.add(tr("status.more", p.getActiveEffects().size() - 10));
                    break;
                }
                String name = e.getEffect().value().getDisplayName().getString() + (e.getAmplifier() > 0 ? " " + (e.getAmplifier() + 1) : "");
                fx.add(name + "   " + (e.isInfiniteDuration() ? tr("status.infinite") : clock(e.getDuration())));
            }
        }
        if (fx.isEmpty()) fx.add(tr("status.noeffects"));
        b.card(UiTheme.VIOLET, tr("status.effects"), fx, -1f, null);
        return b.build();
    }

    private static String clock(int ticks) {
        int s = Math.max(0, ticks / 20);
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }

    // ---------------------------------------------------------------- Quests
    static HubListBody quests(Font font, int w, HubListBody.Host host) {
        HubListBody.Builder b = new HubListBody.Builder(font, w);
        for (HubNet.HudEntry e : HubClient.hudEntries()) {
            boolean ready = e.state() == 1;
            float frac = ready ? 1f : e.target() <= 0 ? -1f : Math.min(1f, e.progress() / (float) e.target());
            b.card(ready ? UiTheme.GREEN : UiTheme.CYAN, e.title(), List.of(ready ? tr("quests.ready") : QuestText.progressLine(e.progress(), e.target(), e.label())), frac, null);
        }
        HubNet.MapSync map = HubClient.lastMap();
        b.row(UiTheme.GREEN, tr("quests.map"), map == null ? tr("quests.map.none") : "", map != null, () -> host.openWatched(HubClient.mapScreen(map)));
        b.card(UiTheme.TEXT_DIM, tr("quests.board"), List.of(tr("quests.board.line1"), tr("quests.board.line2")), -1f, null);
        return b.build();
    }

    // ---------------------------------------------------------------- Multiverse
    static HubListBody multiverse(Font font, int w, HubListBody.Host host) {
        HubListBody.Builder b = new HubListBody.Builder(font, w);
        b.row(0xFFFF9F6A, tr("mv.four"), tr("mv.four.note"), true, () -> host.open(new FourKingdomsScreen(host.screen())));
        b.row(0xFFFF9F6A, tr("mv.beast"), tr("mv.beast.note"), true, () -> host.open(new MultiverseStatusScreen(host.screen(), true)));
        b.row(0xFFFF9F6A, tr("mv.compat"), tr("mv.compat.note"), true, () -> host.open(new CompatCodexScreen(host.screen())));
        b.row(0xFFFF9F6A, tr("mv.magic"), tr("mv.magic.note"), true, () -> host.openWatched(new AbilityCategoriesScreen(AbilityCategoriesScreen.AbilityType.MAGIC)));
        b.row(0xFFFF9F6A, tr("mv.status"), tr("mv.status.note"), true, () -> host.open(new MultiverseStatusScreen(host.screen())));
        return b.build();
    }

    // ---------------------------------------------------------------- Settings
    static HubListBody settings(Font font, int w, HubListBody.Host host) {
        HubListBody.Builder b = new HubListBody.Builder(font, w);
        boolean fx = UiThemeOptions.hubFx(), motion = UiThemeOptions.motion();
        b.toggle(0xFFC0C8D8, tr("settings.fx"), tr(fx ? "settings.on" : "settings.off"), fx, () -> { UiThemeOptions.setFlag(UiThemeOptions.HUB_FX, !UiThemeOptions.hubFx()); host.refresh(); });
        b.toggle(0xFFC0C8D8, tr("settings.motion"), tr(motion ? "settings.on" : "settings.off"), motion, () -> { UiThemeOptions.setFlag(UiThemeOptions.MOTION, !UiThemeOptions.motion()); host.refresh(); });
        b.row(0xFFC0C8D8, tr("settings.intro"), UiThemeOptions.hubIntroSeen() ? "" : tr("settings.intro.queued"), true, () -> { UiThemeOptions.setFlag(UiThemeOptions.HUB_INTRO_SEEN, false); host.refresh(); });
        b.row(0xFFC0C8D8, tr("settings.size"), "", true, () -> host.open(new GuiSizeScreen(host.screen(), null)));
        b.row(0xFFC0C8D8, tr("settings.move"), "", true, () -> host.open(new GuiMoveScreen(host.screen())));
        return b.build();
    }

    // ---------------------------------------------------------------- Locked
    static HubListBody locked(Font font, int w, HubNodes.Tab tab) { return new HubListBody.Builder(font, w).lock(tr("locked." + tab.key)); }
}
