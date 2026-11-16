package com.newuniverse.nusmp.attune;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * Why: meditation can not be played by its author, so the CI server smoke test proves what a stand-in can: {@code attune.rules} (the element table is symmetric, the owner's example "fire Beast Will
 * and ice Djinn" rejects, resonance is capped, suppression keeps the stronger, the backlash never kills, the tag round-trips and is clamped), {@code attune.player} (a stand-in with no session, a moved
 * stand-in, a stand-in asking for a power it does not hold and one asking for too many are all refused and nothing is saved; an empty attunement is taken; a player with nothing out gets factor 1;
 * logout drops the session). Server safe: no client class.
 */
public final class AttuneSelfTests {
    private AttuneSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("attune.rules", s -> rules());
        SelfTests.register("attune.player", AttuneSelfTests::player);
    }

    private static Result rules() {
        for (AttuneRules.Element a : AttuneRules.Element.values())
            for (AttuneRules.Element b : AttuneRules.Element.values()) {
                int x = AttuneRules.bit(a), y = AttuneRules.bit(b);
                if (AttuneRules.pair(x, y) != AttuneRules.pair(y, x)) return Result.fail("the table is not symmetric at " + a + "/" + b);
                if (AttuneRules.opposed(a, b) && AttuneRules.allied(a, b)) return Result.fail(a + "/" + b + " is both opposed and allied");
            }
        int fire = AttuneRules.elementsOf("Fire"), ice = AttuneRules.elementsOf("frost"), light = AttuneRules.elementsOf("LIGHT"), bolt = AttuneRules.elementsOf("LIGHTNING");
        if (AttuneRules.pair(fire, ice) != AttuneRules.Pair.REJECT) return Result.fail("fire and ice do not reject");
        if (AttuneRules.pair(light, AttuneRules.elementsOf("Shadow")) != AttuneRules.Pair.REJECT) return Result.fail("light and dark do not reject");
        if (bolt != AttuneRules.bit(AttuneRules.Element.LIGHTNING)) return Result.fail("lightning reads as light too");
        if (AttuneRules.pair(0, fire) != AttuneRules.Pair.NEUTRAL) return Result.fail("a neutral power clashes");
        AttuneRules.Power beast = new AttuneRules.Power("beast:flame_serpent_will", AttuneRules.BEAST, fire, 2), djinn = new AttuneRules.Power("djinn:x", AttuneRules.DJINN, ice, 3);
        AttuneRules.Refusal r = AttuneRules.check(List.of(beast, djinn), Set.of(beast.id(), djinn.id()), 3);
        if (r == null || !r.reason().equals("reject")) return Result.fail("a fire Beast Will and an ice Djinn were attuned together");
        if (!AttuneRules.suppressed(List.of(beast, djinn)).equals(Set.of(0))) return Result.fail("suppression did not keep the stronger");
        if (AttuneRules.resonance(10, 0.3) > 1.0 + AttuneRules.MAX_RESONANCE + 1e-9) return Result.fail("resonance is not capped");
        if (AttuneRules.backlash(1.5f) > 0.5f || AttuneRules.backlash(100f) > AttuneRules.BACKLASH_MAX) return Result.fail("the backlash can kill");
        AttuneStore.State st = new AttuneStore.State();
        st.slots.add("beast:lion_will");
        st.slots.add("spirit:sylph");
        st.lastChange = 77;
        net.minecraft.nbt.CompoundTag t = st.toTag();
        AttuneStore.State back = AttuneStore.State.fromTag(t);
        if (!back.slots.equals(st.slots) || back.lastChange != 77) return Result.fail("the tag does not round-trip");
        net.minecraft.nbt.ListTag bad = new net.minecraft.nbt.ListTag();
        for (String s : new String[]{"beast:a", "BAD ID", "djinn:b", "devil:c", "spirit:d", "beast:e"}) bad.add(net.minecraft.nbt.StringTag.valueOf(s));
        t.put("slots", bad);
        if (AttuneStore.State.fromTag(t).slots.size() > AttuneRules.MAX_SLOTS) return Result.fail("a damaged save holds too many slots");
        return Result.ok("table symmetric, fire/ice and light/dark reject, suppression keeps the stronger, resonance capped, backlash never lethal, the save is clamped");
    }

    private static Result player(MinecraftServer server) {
        if (!AttuneConfig.enabled()) return Result.ok("skipped: attune.enabled is false");
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 3, 0);
        ServerPlayer a = SelfTests.standIn(level, "attune_a", at.x, at.y, at.z);
        try {
            AttuneService.forget(a.getUUID());
            if (AttuneService.confirm(a, List.of()) != AttuneService.Verdict.NO_SESSION) return Result.fail("a confirm without a session was taken");
            if (AttuneService.factor(a) != 1.0) return Result.fail("a player with nothing out gets factor " + AttuneService.factor(a));
            AttuneService.forget(a.getUUID());
            if (!AttuneService.open(a)) return Result.fail("the stand-in could not sit down");
            AttuneService.forget(a.getUUID());                                    // drop the ask gap and the session ...
            AttuneService.open(a);                                               // ... and sit down again: a fresh session
            a.setPos(at.x + 5, at.y, at.z);
            AttuneService.Verdict moved = confirmLater(a, List.of());
            if (moved != AttuneService.Verdict.MOVED) return Result.fail("a confirm after walking away was " + moved);
            a.setPos(at.x, at.y, at.z);
            AttuneService.forget(a.getUUID());
            AttuneService.open(a);
            AttuneService.Verdict notOwned = confirmLater(a, List.of("beast:dragon_will"));
            if (notOwned != AttuneService.Verdict.BAD) return Result.fail("a power the stand-in does not hold was " + notOwned);
            AttuneService.Verdict tooMany = confirmLater(a, List.of("beast:a", "djinn:b", "devil:c", "spirit:d", "beast:e"));
            if (tooMany != AttuneService.Verdict.BAD) return Result.fail("five ids were " + tooMany);
            AttuneStore.State s = AttuneStore.peek(a);
            if (s != null && !s.slots.isEmpty()) return Result.fail("a refused confirm saved something");
            AttuneService.Verdict empty = confirmLater(a, List.of());
            if (empty != AttuneService.Verdict.DONE) return Result.fail("an empty attunement was " + empty);
            AttuneService.resetAsk(a.getUUID());
            if (!AttuneService.open(a) || !AttuneService.hasSession(a.getUUID())) return Result.fail("no session after /meditate");
            AttuneService.forget(a.getUUID());
            if (AttuneService.hasSession(a.getUUID())) return Result.fail("logout left a session behind");
            return Result.ok("no session, moved, not owned and too many are refused and save nothing; an empty attunement is taken; factor 1 with nothing out; logout drops the session");
        } finally {
            AttuneService.forget(a.getUUID());
            AttuneService.clear(a);
        }
    }

    /** A confirm as if it came after the ask gap (the stand-in's packets arrive in one tick, so the gap is reset for the test). */
    private static AttuneService.Verdict confirmLater(ServerPlayer p, List<String> ids) {
        AttuneService.resetAsk(p.getUUID());
        return AttuneService.confirm(p, ids);
    }
}
