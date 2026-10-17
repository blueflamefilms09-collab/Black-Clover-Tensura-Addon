package com.newuniverse.nusmp.avatar.ritual;

import com.newuniverse.nusmp.avatar.AvatarChatterEngine;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.ChatEvent;
import com.newuniverse.nusmp.avatar.aptitude.Aptitude;
import com.newuniverse.nusmp.avatar.aptitude.AptitudeCatalog;
import com.newuniverse.nusmp.avatar.aptitude.AptitudeLook;
import com.newuniverse.nusmp.avatar.aptitude.ForgePlan;
import com.newuniverse.nusmp.avatar.aptitude.ForgeSkill;
import com.newuniverse.nusmp.avatar.goal.AvatarTargeting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the owner's birth spectacle ends with the Avatar stepping out of the circle, "instantly executing its special ability and speaking its first chat
 * line". This is that last moment. The ability is the Avatar's own flourish, themed by what the creator gave it (the tint of its first aptitude, else of its
 * chosen skill, else its aura): an aura of spirits wraps it ({@link RitualFx#novaAura}) and every hostile creature within five blocks that the Avatar is
 * allowed to hurt is thrown back (no damage: the birth must never be a weapon against whatever is standing near the forge), then it speaks through the
 * chatter engine as a skill-cast shout, with the name of what it "cast": the generated perk when it awakened, else its chosen skill, else its aptitude, else
 * "Awakening". The Avatar's real combat abilities are the AI's business ({@code avatar.cast}); this never casts a spell, so it cannot misfire or cost mana.
 */
public final class BirthSpectacle {
    private BirthSpectacle() {}

    /** The ARGB colour of this Avatar's birth: its first aptitude's tint, else its chosen skill's, else its own aura colour. */
    public static int colour(AvatarEntity a, ForgePlan plan) {
        if (plan != null) {
            for (String id : plan.aptitudes()) {
                Aptitude apt = AptitudeCatalog.byId(id);
                if (apt != null) return 0xFF000000 | AptitudeLook.of(apt).tint();
            }
            for (String id : new String[]{plan.ultimate(), plan.unique()}) {
                ForgeSkill s = id.isEmpty() ? null : ForgeSkillResolver.lookup(id);
                if (s != null) return 0xFF000000 | s.tint();
            }
        }
        return 0xFF000000 | (a.appearance().auraRgb() & 0xFFFFFF);
    }

    /** The name the Avatar shouts: the perk it awakened with, its chosen skill, its first aptitude, or "Awakening". */
    public static String abilityName(AvatarEntity a, ForgePlan plan) {
        if (a.perk() != null) return a.perk().name();
        if (plan != null) {
            for (String id : new String[]{plan.ultimate(), plan.unique()}) {
                ForgeSkill s = id.isEmpty() ? null : ForgeSkillResolver.lookup(id);
                if (s != null) return s.name();
            }
            for (String id : plan.aptitudes()) {
                Aptitude apt = AptitudeCatalog.byId(id);
                if (apt != null) return apt.name() + " Aptitude";
            }
        }
        return "Awakening";
    }

    /** The Avatar's first ability: the aura, the shove, the shout. Safe to call for an Avatar that has already been removed (it does nothing). */
    public static void nova(AvatarEntity a, ForgePlan plan) {
        if (a == null || !a.isAlive() || !(a.level() instanceof ServerLevel level)) return;
        RitualFx.novaAura(level, a, colour(a, plan));
        for (Mob m : level.getEntitiesOfClass(Mob.class, a.getBoundingBox().inflate(5.0), e -> e instanceof Enemy && e != a && AvatarTargeting.canHarm(a, e))) {
            Vec3 away = m.position().subtract(a.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : away.normalize();
            m.push(away.x * 0.9, 0.35, away.z * 0.9);
            m.hurtMarked = true;
        }
        AvatarChatterEngine.say(a, ChatEvent.SKILL_CAST, abilityName(a, plan));
    }
}
