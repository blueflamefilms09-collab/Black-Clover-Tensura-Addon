package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.prop.ShakudoPropState;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 1.93.0: the self-tests of the rebuild of Shakudo, Scale, Spike, Nail and Hair Magic, run by the CI server smoke test in the real game
 * ({@code /multiverse mega selftest magic.<key>_rebuild}). Each checks, for its magic: the five generic pages still come first with the same ids in the
 * same order and the new pages are appended after them with unique ids inside the 24 page limit; the armour render layer and the three own effects exist
 * and the effect mapping of the generic pages reaches them; both constructs spawn as props, carry their variant in the synced word and end cleanly.
 */
public final class ShakudoSelfTests {
    private ShakudoSelfTests() {}

    private static final List<String> GENERIC = List.of("attribute_bolt", "attribute_cut", "attribute_field", "attribute_domain", "attribute_guard");

    /** Called once from ShakudoProps.init(). */
    public static void register() {
        SelfTests.register("magic.shakudo_rebuild", s -> check(s, MagicType.SHAKUDO, PropKind.SHAKUDO_1, PropKind.SHAKUDO_2,
                List.of("shakudo_lance", "shakudo_golem", "shakudo_bulwark", "shakudo_plate_armor", "shakudo_colossus")));
        SelfTests.register("magic.scale_rebuild", s -> check(s, MagicType.SCALE, PropKind.SCALE_1, PropKind.SCALE_2,
                List.of("scale_volley", "scale_serpent", "scale_shield", "scale_mail", "scale_tide")));
        SelfTests.register("magic.spike_rebuild", s -> check(s, MagicType.SPIKE, PropKind.SPIKE_1, PropKind.SPIKE_2,
                List.of("spike_eruption", "spike_wall", "spike_urchin", "spike_armor", "spike_impaling_field")));
        SelfTests.register("magic.nail_rebuild", s -> check(s, MagicType.NAIL, PropKind.NAIL_1, PropKind.NAIL_2,
                List.of("nail_barrage", "nail_driver", "nail_mail", "nail_pin", "nail_storm")));
        SelfTests.register("magic.hair_rebuild", s -> check(s, MagicType.HAIR, PropKind.HAIR_1, PropKind.HAIR_2,
                List.of("hair_lash", "hair_bind", "hair_cocoon", "hair_weave", "hair_mane")));
    }

    private static Result check(MinecraftServer server, MagicType m, PropKind k1, PropKind k2, List<String> added) {
        GrimoireBook book = com.newuniverse.nusmp.skill.NUSkills.bookFor(m);
        if (book == null) return Result.fail(m + ": no registered book");
        List<BookPage> family = book.familyPages();
        int n = GENERIC.size() + added.size();
        if (family.size() != n) return Result.fail(m + ": " + family.size() + " family pages, expected " + n);
        if (family.size() > 24) return Result.fail(m + ": more than 24 family pages");
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < n; i++) {
            String want = i < GENERIC.size() ? GENERIC.get(i) : added.get(i - GENERIC.size()), got = family.get(i).id();
            if (!want.equals(got)) return Result.fail(m + ": page " + i + " is " + got + ", expected " + want);
            if (!seen.add(got)) return Result.fail(m + ": duplicate page id " + got);
            if (family.get(i).cast() == null) return Result.fail(m + ": page " + got + " has no cast");
        }
        if (ShakudoKit.aura(m) == null || ShakudoKit.aura(m) == Aura.WIKI) return Result.fail(m + ": no own armour layer");
        for (int f = 1; f <= 3; f++) {
            VfxShape fx = ShakudoKit.fx(m, f);
            if (fx == VfxShape.WIKI_MAGIC_CAST || !fx.name().equals(m.name() + "_FX" + f)) return Result.fail(m + ": effect " + f + " missing");
        }
        if (ShakudoKit.shape(m, VfxShape.WIKI_MAGIC_FIELD) != ShakudoKit.fx(m, 2) || ShakudoKit.shape(m, VfxShape.MAGIC_CIRCLE) != VfxShape.MAGIC_CIRCLE)
            return Result.fail(m + ": the effect mapping is wrong");
        ServerLevel level = server.overworld();
        for (PropKind kind : new PropKind[]{k1, k2}) {
            MagicPropEntity e = MagicProps.spawn(level, kind, new Vec3(0.5, 100, 0.5), 0f, 1f, 40, ShakudoPropState.pack(3, 0, level.getGameTime()), null);
            if (e == null) return Result.fail(m + ": " + kind + " did not spawn");
            if (e.kind() != kind) return Result.fail(m + ": " + kind + " has the wrong kind");
            if (ShakudoPropState.variant(e.param()) != 3) return Result.fail(m + ": " + kind + " lost its variant");
            e.expire();
            if (!e.isRemoved()) return Result.fail(m + ": " + kind + " did not end");
        }
        return Result.ok(m + ": " + n + " pages in order (" + added.size() + " appended), own armour layer, three effects, two constructs");
    }
}
