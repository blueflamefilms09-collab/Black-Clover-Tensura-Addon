package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.93.0 Spike Magic: the pages appended after the five generic ones. Spike is the earth-side magic of spikes thrust out of the ground and thrown from the
 * body; the repo's wiki extract has no page for it, so the book is the nearest honest version (see docs/attributes/spike_rebuild.md).
 * Pages (appended modes 5 to 9):
 * <ul>
 *   <li>{@code spike_eruption} Spike Eruption (starter): a line of six spike pillars erupts in front of the caster, one after the other, 1.8 blocks apart;</li>
 *   <li>{@code spike_wall} Spike Wall (mid): seven pillars side by side across the caster's way for 8 s; whatever stays on them is pricked;</li>
 *   <li>{@code spike_urchin} Spike Urchin (mid): a spiked ball homes in on the target (or the crosshair) and bursts in a 3 block ring;</li>
 *   <li>{@code spike_armor} Spike Armor (mid): the temporary armour, 20 percent of every blow softened, attackers cut for half of what they dealt (capped), 20 s;</li>
 *   <li>{@code spike_impaling_field} Impaling Field (signature, 90 s): for 5 s, every half second a pillar erupts under each of the three nearest foes in 7 blocks.</li>
 * </ul>
 */
public final class SpikeSpells {
    private SpikeSpells() {}

    public static final int PILLAR_TICKS = 40, WALL_TICKS = 160, URCHIN_TICKS = 80, ARMOUR_TICKS = 400, FIELD_TICKS = 100;

    public static List<BookPage> pages() {
        return List.of(
                BookPage.starter("spike_eruption", "Spike Eruption", SpikeSpells::eruption),
                BookPage.mid("spike_wall", "Spike Wall", SpikeSpells::wall),
                BookPage.mid("spike_urchin", "Spike Urchin", SpikeSpells::urchin),
                BookPage.mid("spike_armor", "Spike Armor", (b, i, p, m) -> ShakudoKit.guard(b, p, MagicType.SPIKE, ARMOUR_TICKS, 0.20f, 3f + 0.4f * ShakudoKit.mighty(p), 0)),
                BookPage.signature("spike_impaling_field", "Impaling Field", SpikeSpells::field).withCooldown(1800));
    }

    private static MagicPropEntity pillar(ServerPlayer p, Vec3 at, float scale, int life) {
        return ShakudoKit.spawn(p, PropKind.SPIKE_1, at, p.getYRot(), scale, life, 0, null);
    }

    /** Six pillars in a line, a tick pair apart, each at the height of the caster's feet. */
    static boolean eruption(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 look = ShakudoKit.flatLook(p), base = p.position();
        float s = ShakudoKit.scaleFor(p, 0.9f);
        ServerLevel level = p.serverLevel();
        b.castCircle(p, 0.8f);
        for (int n = 0; n < 6; n++) {
            final Vec3 at = base.add(look.scale(1.5 + 1.8 * n));
            SpellRuntime.later(level, 2 * n, () -> { if (level.isLoaded(net.minecraft.core.BlockPos.containing(at))) pillar(p, at, s, PILLAR_TICKS); });
        }
        b.vfx(p, VfxShape.SPIKE_FX1, p.getEyePosition().add(0, -0.4, 0), base.add(look.scale(11)).add(0, 0.3, 0), 16, 0.9f);
        return true;
    }

    /** Seven pillars across the caster's way, 3 blocks out, that stay for 8 s. */
    static boolean wall(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 look = ShakudoKit.flatLook(p), side = new Vec3(-look.z, 0, look.x), centre = p.position().add(look.scale(3.2));
        float s = ShakudoKit.scaleFor(p, 1.0f);
        int made = 0;
        for (int n = -3; n <= 3; n++) if (pillar(p, centre.add(side.scale(n * 1.2)), s, ShakudoKit.span(WALL_TICKS, p)) != null) made++;
        if (made == 0) { GrimoireBook.fail(p, "The earth would not answer."); return false; }
        b.castCircle(p, 1.0f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SPIKE_FX2, centre, centre.add(0, 1, 0), b.color, 30, 2.4f);
        return true;
    }

    /** The urchin leaves the caster's hand at 0.5 blocks a tick and homes in on the foe under the crosshair. */
    static boolean urchin(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity foe = GrimoireBook.target(p, 24);
        Vec3 start = p.getEyePosition().add(p.getViewVector(1f).scale(0.8)).add(0, -0.4, 0);
        MagicPropEntity u = ShakudoKit.spawn(p, PropKind.SPIKE_2, start, p.getYRot(), ShakudoKit.scaleFor(p, 0.9f), URCHIN_TICKS, 0, foe);
        if (u == null) { GrimoireBook.fail(p, "The earth would not answer."); return false; }
        u.setDeltaMovement(p.getViewVector(1f).scale(0.55));
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.SPIKE_FX1, start, start.add(p.getViewVector(1f).scale(8)), 14, 0.6f);
        return true;
    }

    /** The Impaling Field: for 5 s, every 10 ticks a pillar under each of the three nearest foes (and one in front of the caster if there are none). */
    static boolean field(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        float s = ShakudoKit.scaleFor(p, 1.1f);
        b.castCircle(p, 1.6f);
        b.vfx(p, VfxShape.SPIKE_FX2, c, c.add(0, 1, 0), FIELD_TICKS, 7.0f);
        SpellRuntime.zone(level, FIELD_TICKS, 10, age -> {
            if (!p.isAlive()) return;
            List<LivingEntity> foes = new ArrayList<>(GrimoireBook.around(p, p.position(), 7.0));
            foes.sort((x, y) -> Double.compare(x.distanceToSqr(p), y.distanceToSqr(p)));
            int n = 0;
            for (LivingEntity t : foes) {
                if (n++ >= 3) break;
                pillar(p, t.position(), s, PILLAR_TICKS);
            }
            if (foes.isEmpty()) pillar(p, p.position().add(ShakudoKit.flatLook(p).scale(2.5 + (age % 3))), s, PILLAR_TICKS);
        });
        return true;
    }
}
