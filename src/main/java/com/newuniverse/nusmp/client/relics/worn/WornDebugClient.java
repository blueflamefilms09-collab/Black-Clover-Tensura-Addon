package com.newuniverse.nusmp.client.relics.worn;

import com.newuniverse.nusmp.client.relics.RelicClient;
import com.newuniverse.nusmp.client.relics.RelicTextures;
import com.newuniverse.nusmp.client.relics.worn.lib.WornLibDraw;
import com.newuniverse.nusmp.client.relics.worn.lib.WornLibrary;
import com.newuniverse.nusmp.item.CuriosProbe;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.WornSync;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the client half of {@code /multiverse relic debug}. The server can say what a wearer carries and which model file each relic resolves to, but only the client knows whether the
 * worn layer was REACHED for that wearer, what it decided frame by frame ({@link WornTrace}), whether its own look at the Curios inventory works, whether the model library could dress each
 * relic from the resources it really loaded, and whether the textures are registered. This prints all of that in the operator's chat. Client thread only.
 */
public final class WornDebugClient {
    private WornDebugClient() {}

    private static Component t(String key, String fallback, Object... args) { return Component.translatableWithFallback("message.nusmp.relic_debug." + key, fallback, args); }

    private static Component lit(String s) { return Component.literal(s); }

    /** Prints the report for {@code target}; {@code serverLines} are the server's "id|SLOT|WHERE" entries for the same wearer. */
    public static void report(UUID target, List<String> serverLines) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        List<Component> out = new ArrayList<>();
        Player who = mc.level.getPlayerByUUID(target);
        String name = who == null ? target.toString() : who.getName().getString();
        out.add(t("client_title", "Worn relics of %s (client view, this machine)", name));
        out.add(t("layers", "Worn layer added to %s player renderers; refused: %s", RelicClient.layersAdded(), RelicClient.layersRefused().isEmpty() ? "-" : RelicClient.layersRefused()));
        boolean atlas = mc.getResourceManager().getResource(WornLibDraw.ATLAS).isPresent(), shimmer = mc.getResourceManager().getResource(WornLibDraw.SHIMMER).isPresent();
        out.add(t("client_textures", "Atlas texture resolved: %s, shimmer: %s, white pixel registered: %s", yes(atlas), yes(shimmer), yes(RelicTextures.whiteRegistered())));
        out.add(t("sync", "Server lists received: %s (last %s ms ago)", WornSync.receivedCount(), WornSync.sinceLastReceived()));
        if (who == null) {
            out.add(t("not_loaded", "The wearer is not loaded on this client (too far, or in another dimension): nothing can be drawn for it here."));
            send(out);
            return;
        }
        WornTrace.Seen seen = WornTrace.of(target);
        long gameNow = mc.level.getGameTime();
        if (seen == null) {
            out.add(t("never", "LAYER NEVER REACHED for this wearer. Vanilla does not draw your own body in first person (press F5), the wearer may be out of view, or another renderer draws it."));
        } else {
            long age = gameNow - seen.tick;
            out.add(t(age <= 40 ? "reached" : "stale", age <= 40 ? "Layer reached %s ticks ago (%s frames, layer %s, view %s, slim arms %s)"
                            : "Layer last reached %s ticks ago (%s frames, layer %s, view %s, slim arms %s): the wearer is not being drawn right now", age, seen.frames, seen.layer, seen.view, yes(seen.slim)));
        }
        CuriosProbe.Report probe = CuriosProbe.probe(who);
        out.add(t("client_curios", "This client's own read of the Curios inventory: %s", probe.ok() ? t("ok", "every step passed") : t("broken", "A STEP FAILED, see below")));
        for (String l : probe.lines()) out.add(lit("  " + l));

        List<Relics.Located> found = Relics.locatedList(who);
        Set<String> clientHas = new HashSet<>();
        int n = 0;
        for (Relics.Located l : found) {
            RelicDef d = l.def();
            n++;
            clientHas.add(d.id() + "|" + d.slot().name());
            out.add(t("relic", "%s. %s: slot %s, found by this client in: %s", n, d.id(), d.slot().name(), l.where().name().toLowerCase()));
            if (d.slot().isHand()) {
                out.add(lit("     " + WornPlan.Why.HELD_IN_HAND.text));
                continue;
            }
            WornLibrary.Ensemble e = WornLibrary.of(d);
            out.add(t("client_model", "     model library: %s", e == null ? "NO MODEL LOADED (resources missing or the catalog has no entry): plain boxes or a floating charm" : e.pieces.size() + " pieces, " + e.baseVerts + " vertices, tier " + e.tier));
            WornPlan.Item item = seen == null || seen.plan == null ? null : find(seen.plan, d.id());
            out.add(t("client_plan", "     last decision: %s", item == null ? "-" : item.describe()));
        }
        if (found.isEmpty()) out.add(t("client_none", "This client found no worn relic on the wearer."));
        for (String s : serverLines) {                                                      // the server's list against this client's
            String[] p = s.split("\\|");
            if (p.length >= 3 && !clientHas.contains(p[0] + "|" + p[1])) out.add(t("missing", "The server sees %s (%s, %s) but this client did not find it: it is not drawn on this machine.", p[0], p[1], p[2].toLowerCase()));
        }
        send(out);
    }

    private static WornPlan.Item find(WornPlan.Plan plan, String id) {
        for (WornPlan.Item i : plan.all()) if (i.entry().id().equals(id)) return i;
        return null;
    }

    private static String yes(boolean b) { return b ? "yes" : "NO"; }

    private static void send(List<Component> lines) {
        Minecraft mc = Minecraft.getInstance();
        for (Component c : lines) mc.gui.getChat().addMessage(c);
    }
}
