package com.newuniverse.nusmp.mana.zone;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The two pages the Mana Zone adds to every grimoire (appended last, so no mode number moves): they need an open zone, and so they need the late-game gate.
 * <ul>
 *   <li><b>Omni Volley</b>: offence from ANY point inside the zone: for each of the foes nearest your crosshair, bolts of mana start behind them and all
 *       round them (not from you) and strike together. A boss takes half.</li>
 *   <li><b>Pressure Space</b>: condenses the mana round you into a highly pressurised protective space for you and your allies (a few seconds, a few of them
 *       at once): blows inside it are cut, and it is only half as good against a boss.</li>
 * </ul>
 * The ids contain "zone" so the Avatar's cast list (AvatarSkillList) never casts them (a test checks it).
 */
public final class ManaZoneActions {
    private ManaZoneActions() {}

    public static final String VOLLEY_ID = "zone_omni_volley", SPACE_ID = "zone_pressure_space";

    /** The pages as the grimoire appends them (cost percent, floor, cooldown ticks). */
    public static BookPage volleyPage() { return new BookPage(VOLLEY_ID, "Omni Volley", "Omni Volley", 6, 400, 160, ManaZoneActions::omniVolley); }

    public static BookPage spacePage() { return new BookPage(SPACE_ID, "Pressure Space", "Pressure Space", 8, 500, 400, ManaZoneActions::pressureSpace); }

    public static boolean omniVolley(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ManaZoneManager.Zone z = ManaZoneManager.zoneOf(p);
        if (z == null) {
            GrimoireBook.fail(p, "Open a Mana Zone first: the volley is launched from the points of the zone.");
            return false;
        }
        ManaZoneRules.Zone zr = ManaZoneData.zone();
        ServerLevel sl = p.serverLevel();
        double r = ManaZoneManager.radius(z, sl.getGameTime());
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        List<LivingEntity> foes = new ArrayList<>(sl.getEntitiesOfClass(LivingEntity.class, new AABB(z.cx - r, z.cy - r, z.cz - r, z.cx + r, z.cy + r, z.cz + r),
                e -> Foes.isFoe(p, e) && ManaZoneMath.inside(e.getX() - z.cx, e.getY() - z.cy, e.getZ() - z.cz, r)));
        if (foes.isEmpty()) {
            GrimoireBook.fail(p, "No foe inside your Mana Zone.");
            return false;
        }
        foes.sort((a, c) -> Double.compare(aim(c, eye, look), aim(a, eye, look)));          // nearest the crosshair first
        int bolts = Math.max(1, zr.volleyBoltsPerTarget()), hit = 0;
        double[] o = new double[3];
        for (LivingEntity t : foes) {
            if (hit >= zr.volleyMaxTargets()) break;
            Vec3 c = t.getBoundingBox().getCenter();
            float raw = (float) (zr.volleyDamage() * (BalanceLaw.isBoss(t) ? zr.volleyBossDamageMult() : 1.0));
            for (int k = 0; k < bolts; k++) {
                ManaZoneMath.omniOrigin(k, bolts + 1, t.getX(), t.getY(), t.getZ(), p.getX(), p.getZ(), 3.0, o);
                b.vfx(p, VfxShape.LIGHT_BLADE, new Vec3(o[0], o[1], o[2]), c, 8, 0.7f);
                b.hurt(i, p, t, mode, raw);
            }
            b.impact(p, c, 0.5f);
            hit++;
        }
        sl.playSound(null, p.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.3f, 1.6f);
        return true;
    }

    /** How well a target lines up with the crosshair: the cosine of the angle (higher is nearer the aim), with a small bonus for being near. */
    private static double aim(LivingEntity e, Vec3 eye, Vec3 look) {
        Vec3 d = e.getBoundingBox().getCenter().subtract(eye);
        double len = Math.max(1e-3, d.length());
        return d.dot(look) / len - len * 0.002;
    }

    public static boolean pressureSpace(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ManaZoneRules.Zone zr = ManaZoneData.zone();
        Vec3 c = p.position();
        if (!ManaZoneManager.addBubble(p, c.x, c.y, c.z)) {
            GrimoireBook.fail(p, "Open a Mana Zone first: the pressure is made of the zone's own mana.");
            return false;
        }
        ServerLevel sl = p.serverLevel();
        VfxSpawn.send(sl, VfxShape.MANA_ZONE_BUBBLE, c, Vec3.ZERO, ManaZoneManager.tintOf(p), zr.bubbleTicks(), (float) zr.bubbleRadius());
        sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2f, 0.9f);
        return true;
    }
}
