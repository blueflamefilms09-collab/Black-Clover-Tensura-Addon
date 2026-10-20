package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.client.relics.worn.WornAssets;
import com.newuniverse.nusmp.client.relics.worn.WornPlan;
import com.newuniverse.nusmp.item.CuriosProbe;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The self-test {@code relic.worn_render2} (run by the CI server smoke test, see {@link SelfTests}), the second half of {@code relic.worn_render}: that one proves every worn relic has a
 * model file; this one proves the DECISION and the DATA PATH between the server and the screen, with no client class that touches the game:
 * <ol>
 *   <li>{@link WornPlan} gives every worn relic of the registry a drawn answer (a library model, plain boxes, an own renderer) in each context a player is seen in (wide and slim arms, own body
 *       and another's, models on and off), and never skips one for a reason other than the Curios eye icon or the armour option;</li>
 *   <li>the packets {@link WornSync} and {@link WornDebugPayload} survive their codecs with real stacks (the registry-aware buffer of this server);</li>
 *   <li>a named mob's pack, which a client can never read, is found by the server side scan ({@link Relics#scanExtra}, the list {@link WornSync} sends) and the plan draws it as a charm;</li>
 *   <li>the Curios probe runs without throwing and says whether Curios is there (it is not on the CI server: reported, not failed).</li>
 * </ol>
 */
public final class WornRender2SelfTests {
    private WornRender2SelfTests() {}

    public static void register() { SelfTests.register("relic.worn_render2", WornRender2SelfTests::run); }

    static SelfTests.Result run(MinecraftServer server) {
        List<RelicDef> all = Relics.all();
        if (all.isEmpty()) return SelfTests.Result.fail("no relic is registered");

        // 1. the plan, over every worn relic, in every context
        Map<WornPlan.Kind, Integer> kinds = new EnumMap<>(WornPlan.Kind.class);
        int worn = 0;
        RelicDef pack = null;
        for (boolean slim : new boolean[]{false, true}) {
            for (WornPlan.View view : new WornPlan.View[]{WornPlan.View.OTHER, WornPlan.View.SELF_THIRD}) {
                for (boolean models : new boolean[]{true, false}) {
                    List<WornPlan.Entry> entries = new ArrayList<>();
                    for (RelicDef d : all) {
                        if (!WornPlan.isWornSlot(d.slot().name())) continue;
                        boolean has = WornAssets.hasModel(d.id(), d.slot().name(), d.look().name());
                        WornPlan.Source src = d.slot().isArmor() ? WornPlan.Source.ARMOUR : WornPlan.Source.CURIOS;
                        entries.add(new WornPlan.Entry(d.id(), d.slot().name(), d.look().name(), src, Boolean.TRUE, WornAssets.CUSTOM_RENDERERS.contains(d.id()), has, d.grade().tier()));
                        if (pack == null && d.slot() == RelicSlot.CHARM) pack = d;
                    }
                    WornPlan.Plan plan = WornPlan.decide(new WornPlan.Ctx(slim, view, false, false, false, models, false, WornPlan.Fallback.BOXES), entries);
                    worn = entries.size();
                    if (worn == 0) return SelfTests.Result.fail("no relic is worn on the body?");
                    for (WornPlan.Item it : plan.all()) {
                        if (it.kind() == WornPlan.Kind.SKIP && it.why() != WornPlan.Why.VANILLA_ARMOUR)
                            return SelfTests.Result.fail(it.entry().id() + " is skipped (" + it.why() + ") in context slim=" + slim + " view=" + view + " models=" + models);
                        kinds.merge(it.kind(), 1, Integer::sum);
                    }
                    if (plan.drawnCount() + countVanilla(plan) != entries.size()) return SelfTests.Result.fail("the plan lost a relic: " + plan.drawnCount() + " of " + entries.size());
                }
            }
        }
        // a wearer that cannot be seen draws nothing, whatever it wears
        List<WornPlan.Entry> one = List.of(new WornPlan.Entry("x", "NECK", "AMULET", WornPlan.Source.CURIOS, null, false, true, 3));
        if (!WornPlan.decide(new WornPlan.Ctx(false, WornPlan.View.OTHER, false, true, false, true, false, WornPlan.Fallback.BOXES), one).isEmpty())
            return SelfTests.Result.fail("an invisible wearer still draws relics");
        if (!WornPlan.decide(new WornPlan.Ctx(false, WornPlan.View.OTHER, false, false, true, true, false, WornPlan.Fallback.BOXES), one).isEmpty())
            return SelfTests.Result.fail("a far wearer still draws relics");

        // 2. the packets
        ItemStack a = ItemStack.EMPTY, b = ItemStack.EMPTY;                                  // the first two relics that have an item of their own (an adopted one may not)
        for (RelicDef d : all) {
            ItemStack s = RelicCatalog.newStack(d);
            if (s.isEmpty()) continue;
            if (a.isEmpty()) a = s;
            else if (!WornSync.keyOf(s).equals(WornSync.keyOf(a))) { b = s; break; }
        }
        if (a.isEmpty() || b.isEmpty()) return SelfTests.Result.fail("fewer than two relics have an item to send");
        UUID id = UUID.nameUUIDFromBytes("worn_render2".getBytes());
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
        WornSync.STREAM_CODEC.encode(buf, new WornSync(id, List.of(a, b)));
        WornSync back = WornSync.STREAM_CODEC.decode(buf);
        if (!back.wearer().equals(id) || back.stacks().size() != 2 || !WornSync.keyOf(back.stacks().get(0)).equals(WornSync.keyOf(a)) || !WornSync.keyOf(back.stacks().get(1)).equals(WornSync.keyOf(b)))
            return SelfTests.Result.fail("the worn list changed on its way through the codec");
        buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
        WornDebugPayload.STREAM_CODEC.encode(buf, new WornDebugPayload(id, List.of("ring|CHARM|CURIOS", "cloak|BACK|PACK")));
        WornDebugPayload dback = WornDebugPayload.STREAM_CODEC.decode(buf);
        if (!dback.target().equals(id) || dback.serverLines().size() != 2) return SelfTests.Result.fail("the debug request changed on its way through the codec");
        if (WornMerge.missing(List.of(WornSync.keyOf(a)), List.of(WornSync.keyOf(a), WornSync.keyOf(b))).size() != 1) return SelfTests.Result.fail("the merge of the client's look and the server's list is wrong");

        // 3. a mob's pack is read by the server side scan and drawn by the plan
        String packNote = "no charm relic to test a pack with";
        Mob mob = EntityType.ZOMBIE.create(server.overworld());
        if (mob == null) return SelfTests.Result.fail("could not make a mob to carry a pack");
        if (pack != null) {
            ItemStack ring = RelicCatalog.newStack(pack);
            ListTag list = new ListTag();
            list.add(ring.save(mob.registryAccess()));
            mob.getPersistentData().put("nusmp_mob_relics", list);
            List<Relics.Located> extra = Relics.scanExtra(mob);
            if (extra.size() != 1 || extra.get(0).where() != Relics.Where.PACK || extra.get(0).def() != pack) return SelfTests.Result.fail("the pack of a mob was not found by the server scan (" + extra.size() + " found)");
            RelicDef d = extra.get(0).def();
            WornPlan.Plan plan = WornPlan.decide(new WornPlan.Ctx(false, WornPlan.View.OTHER, false, false, false, true, false, WornPlan.Fallback.CHARM),
                    List.of(new WornPlan.Entry(d.id(), d.slot().name(), d.look().name(), WornPlan.Source.PACK, null, false, WornAssets.hasModel(d.id(), d.slot().name(), d.look().name()), d.grade().tier())));
            if (plan.isEmpty()) return SelfTests.Result.fail("the pack relic " + d.id() + " is not drawn: " + plan.all().get(0).describe());
            packNote = "pack relic " + d.id() + " found and drawn as " + plan.all().get(0).kind();
        }

        // 4. the Curios probe
        CuriosProbe.Report probe = CuriosProbe.probe(mob);
        return SelfTests.Result.ok(worn + " worn relics planned in 8 contexts " + kinds + "; codecs ok; " + packNote + "; curios probe: " + (probe.lines().isEmpty() ? "-" : probe.lines().get(0)));
    }

    private static int countVanilla(WornPlan.Plan plan) {
        int n = 0;
        for (WornPlan.Item it : plan.all()) if (it.kind() == WornPlan.Kind.SKIP && it.why() == WornPlan.Why.VANILLA_ARMOUR) n++;
        return n;
    }
}
