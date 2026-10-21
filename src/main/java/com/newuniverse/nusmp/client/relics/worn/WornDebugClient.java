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
        out.add(t("humanoids", "Worn layer added to %s other humanoid renderers (armour stands, zombies, skeletons, mobs of other mods)", RelicClient.humanoidLayers()));
        out.add(t("guard","Renderers that got the layer from the first-frame guard instead of the event: %s (%s)", com.newuniverse.nusmp.client.relics.RelicLayerGuard.healedCount(),
                com.newuniverse.nusmp.client.relics.RelicLayerGuard.lastHealed().isEmpty() ? "-" : com.newuniverse.nusmp.client.relics.RelicLayerGuard.lastHealed()));
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
            if (age <= 40) out.add(t("reached", "Layer reached %s ticks ago (%s frames, layer %s, view %s, slim arms %s)", age, seen.frames, seen.layer, seen.view, yes(seen.slim)));
            else out.add(t("stale", "Layer last reached %s ticks ago (%s frames, layer %s, view %s, slim arms %s): the wearer is not being drawn right now", age, seen.frames, seen.layer, seen.view, yes(seen.slim)));
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
            out.add(t("client_relic", "%s. %s: slot %s, found by this client in: %s", n, d.id(), d.slot().name(), l.where().name().toLowerCase()));
            if (d.slot().isHand()) {
                out.add(lit("     " + WornPlan.Why.HELD_IN_HAND.text));
                continue;
            }
            WornLibrary.Ensemble e = WornLibrary.of(d);
            out.add(t("client_model", "     model library: %s", e == null ? "NO MODEL LOADED (resources missing or the catalog has no entry): plain boxes or a floating charm" : e.pieces.size() + " pieces, " + e.baseVerts + " vertices, tier " + e.tier));
            WornPlan.Item item = seen == null || seen.plan == null ? null : find(seen.plan, d.id());
            out.add(t("client_plan", "     last decision: %s", item == null ? "-" : item.describe()));
            if (l.where() == Relics.Where.ARMOUR && d.slot().isArmor()) armourLines(out, target, d);
        }
        if (found.isEmpty()) out.add(t("client_none", "This client found no worn relic on the wearer."));
        for (String s : serverLines) {                                                      // the server's list against this client's
            String[] p = s.split("\\|");
            if (p.length >= 3 && !clientHas.contains(p[0] + "|" + p[1])) out.add(t("missing", "The server sees %s (%s, %s) but this client did not find it: it is not drawn on this machine.", p[0], p[1], p[2].toLowerCase()));
        }
        send(out);
    }

    /** 1.111.0: what the worn layer really did for an armour relic in its last frame: slot, which armour layer was chosen, whether a model was drawn, whether the painted texture is there, what the failsafe did. */
    private static void armourLines(List<Component> out, UUID target, RelicDef d) {
        ArmorTrace.Rec r = ArmorTrace.of(target, d.slot().name());
        out.add(t("armour_texture", "     armour slot %s: painted layer texture present: %s (%s registered; %s)", d.slot().name(), yes(RelicTextures.paintedOk(d.id())), RelicTextures.paintedCount(), RelicTextures.lastVerify()));
        if (r == null || !r.id.equals(d.id())) {
            out.add(t("armour_never", "     armour record: the worn layer has not finished a frame for this piece yet (or never ran for this wearer)"));
            return;
        }
        out.add(t("armour_layer", "     vanilla armour layer chosen: %s (plan %s / %s); asked for the painted layer last frame: %s", r.layer, r.kind, r.why.text, yes(r.paintedAsked)));
        out.add(t("armour_drawn", "     model drawn: %s (%s pieces); own renderer ran: %s", yes(r.modelPieces > 0), r.modelPieces, yes(r.customRan)));
        out.add(t("armour_result", "     result: %s; failsafe boxes drawn: %s", r.outcome, yes(r.failsafeDrawn)));
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
