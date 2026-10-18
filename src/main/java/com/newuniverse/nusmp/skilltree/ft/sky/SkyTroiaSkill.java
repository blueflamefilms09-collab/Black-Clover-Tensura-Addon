package com.newuniverse.nusmp.skilltree.ft.sky;

import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.ft.kit.FtTreeSkill;
import com.newuniverse.nusmp.skilltree.ft.kit.FtWorld;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: Troia is the second healing spell of the story and not a heal at all: a ball of light that touches someone and restores their sense of balance, which in the story cures motion
 * sickness for a while. A game has a name for "restores balance and clears what is wrong with you": a cleanse. So this technique takes the harmful effects off the allies around the
 * caster, nausea first (the sickness the spell is named for, {@link SkyNumbers#cleansePriority}) and then the poisons and the rest, as many as the node level allows per creature; at the
 * higher levels it also puts out flames and thaws a chilled body. It deliberately restores no health, so it cannot be used as a second heal and stay inside the balance law's
 * healing cap, and it is refused (nothing spent, no cooldown) when nobody needs it, so a cleanse is never wasted on a healthy party. The harmful effects are found by their category in
 * the effect registry and removed through the creature's own effect list, so effects from other mods that are marked harmful are cleansed too without this class knowing them.
 */
public final class SkyTroiaSkill extends FtTreeSkill {
    public SkyTroiaSkill() { super(SkyRoute.ID, SkyRoute.TROIA, "item/milk_bucket", SkyNumbers::fallback); }

    @Override
    protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
        int strips = whole("strips", lv), cap = whole("targets", lv);
        double radius = num("radius", lv);
        boolean thaw = num("extinguish", lv) > 0;

        List<LivingEntity> needy = new ArrayList<>();
        for (LivingEntity e : SkyAllies.around(p, p.position(), radius)) {
            if (!harmful(e).isEmpty() || (thaw && (e.isOnFire() || e.getTicksFrozen() > 0))) needy.add(e);
        }
        needy = FtWorld.nearest(needy, p.position(), cap);
        if (needy.isEmpty()) {
            AbilityKit.say(p, "No one nearby needs cleansing.");
            return false;
        }
        if (!pay(p, lv)) return false;

        ServerLevel level = p.serverLevel();
        for (LivingEntity e : needy) {
            List<Holder<MobEffect>> bad = harmful(e);
            List<String> chosen = SkyNumbers.pickStrips(paths(bad), strips);
            for (Holder<MobEffect> h : bad) if (chosen.contains(path(h))) e.removeEffect(h);
            if (thaw) {
                e.clearFire();
                e.setTicksFrozen(0);
            }
            Vec3 c = FtWorld.center(e);
            AbilityKit.fx(level, VfxShape.SEAL_ORBIT, c, c, SkyRoute.WHITE, 30, 0.9f);
        }
        AbilityKit.sound(p, SoundEvents.BEACON_POWER_SELECT, 0.7f, 1.6f);
        AbilityKit.sound(p, SoundEvents.ALLAY_ITEM_GIVEN, 0.9f, 1.3f);
        return true;
    }

    /** The harmful effects a creature has right now, as registry holders. */
    static List<Holder<MobEffect>> harmful(LivingEntity e) {
        List<Holder<MobEffect>> out = new ArrayList<>();
        for (MobEffectInstance i : e.getActiveEffects()) if (i.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) out.add(i.getEffect());
        return out;
    }

    private static List<String> paths(List<Holder<MobEffect>> effects) {
        List<String> out = new ArrayList<>(effects.size());
        for (Holder<MobEffect> h : effects) out.add(path(h));
        return out;
    }

    private static String path(Holder<MobEffect> h) { return h.unwrapKey().map(k -> k.location().getPath()).orElse(""); }
}
