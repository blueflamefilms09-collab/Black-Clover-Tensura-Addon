package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.elite.skills.rules.MagicRules;
import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.skill.SkillText;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Gravity Well (Aspectual magic of the gravity family, designed for this mod; the original addon's source was not available): release the key and a well of pulled space opens where you look
 * (up to 24 blocks). For six seconds (eight when mastered) it pulls every foe in its radius toward the centre, harder the nearer they are, crushes them for a little damage each half
 * second and slows them; it ends in a Collapse that strikes everything still inside. The second mode, Collapse, ends your own well early for a tenth of the price. The pull is a hard
 * control, so it obeys the shared 12-second control lock of the balance law (when the lock is not free the well still crushes and slows but pulls only lightly), and bosses are pulled
 * less. The well never touches allies, pets, summons or you. Numbers: {@link MagicRules.GravityWell}. The Gravity Grimoire's own "Gravity Well" page is a different, older spell and is untouched.
 */
public class GravityWellMagic extends EliteAspectual {
    static final SkillText TEXT = SkillText.of("gravity_well", "Gravity Well",
            "Aspectual magic (gravity). Open a well where you look: it pulls foes in, crushes and slows them, then collapses. The second mode collapses your well early.");

    private static final String END = "WellEnd", X = "WellX", Y = "WellY", Z = "WellZ", RADIUS = "WellR", PULL = "WellPull";
    private static final int COLOR = 0xFF8A6CFF;

    public GravityWellMagic() { super(AspectualType.GRAVITY, TEXT); }

    @Override
    protected List<EliteMode> modes() {
        return List.of(EliteMode.of("gravity_well.open", "Gravity Well", MagicRules.GravityWell.COST, MagicRules.GravityWell.COOLDOWN),
                EliteMode.of("gravity_well.collapse", "Collapse", MagicRules.GravityWell.collapseCost(), 40));
    }

    @Override protected int color() { return COLOR; }

    @Override
    protected boolean cast(ManasSkillInstance instance, ServerPlayer player, int mode, int heldTicks) {
        return mode == 1 ? collapseEarly(instance, player) : open(instance, player);
    }

    private boolean open(ManasSkillInstance instance, ServerPlayer player) {
        boolean mastered = masteredBy(instance, player);
        ServerLevel level = player.serverLevel();
        Vec3 centre = EliteBridge.lookPoint(player, MagicRules.GravityWell.CAST_RANGE).add(0, 0.3, 0);
        double radius = MagicRules.GravityWell.radius(mastered);
        int pulses = MagicRules.GravityWell.pulses(mastered);
        long end = level.getGameTime() + MagicRules.GravityWell.durationTicks(mastered) + 1;
        boolean pull = BalanceLaw.beginControl(player);
        var tag = instance.getOrCreateTag();
        tag.putLong(END, end);
        tag.putDouble(X, centre.x);
        tag.putDouble(Y, centre.y);
        tag.putDouble(Z, centre.z);
        tag.putDouble(RADIUS, radius);
        tag.putBoolean(PULL, pull);
        instance.markDirty();
        EliteBridge.shape(level, VfxShape.GRAVITY_SINGULARITY, centre, centre, COLOR, 24, (float) (radius / 4.0));
        EliteBridge.sound(level, centre, SoundEvents.BEACON_ACTIVATE, 1.2f, 0.5f);
        final var id = getRegistryName();
        EliteBridge.startZone(player, MagicRules.GravityWell.PULSE_INTERVAL, pulses, (owner, lvl, index) -> {
            ManasSkillInstance live = SkillAPI.getSkillsFrom(owner).getSkill(id).orElse(null);
            if (live == null || live.getOrCreateTag().getLong(END) != end) return;          // collapsed early, or replaced by a newer well
            pulse(live, owner, lvl, centre, radius, mastered, pull, index);
            if (index == pulses - 1) collapse(live, owner, lvl, centre, radius, mastered);
        });
        EliteBridge.actionbar(player, SkillMsg.t("gravity_well.open", "A gravity well opens.").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    private void pulse(ManasSkillInstance instance, ServerPlayer owner, ServerLevel level, Vec3 centre, double radius, boolean mastered, boolean pull, int index) {
        double mastery = masteryFrac(instance);
        for (LivingEntity t : EliteBridge.foes(owner, centre, radius)) {
            double d = t.position().add(0, t.getBbHeight() / 2, 0).distanceTo(centre);
            double strength = MagicRules.GravityWell.pull(d, radius, BalanceLaw.isBoss(t));
            Vec3 toward = centre.subtract(t.position().add(0, t.getBbHeight() / 2, 0));
            if (strength > 0 && toward.lengthSqr() > 1.0e-4) {
                Vec3 push = toward.normalize().scale(pull ? strength : strength * 0.3);
                // airborne foes are dragged down as well: it is a well, not a trampoline
                t.setDeltaMovement(t.getDeltaMovement().scale(0.6).add(push.x, push.y - (t.onGround() ? 0.0 : 0.08), push.z));
                t.hurtMarked = true;
            }
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MagicRules.GravityWell.slowTicks(), 1, true, false));
            EliteBridge.hit(owner, t, MagicRules.GravityWell.pulseDamage(d, radius, mastered) * EliteSkillsConfig.damageScale(), mastery, true);
        }
        EliteBridge.shape(level, VfxShape.GRAVITY_WELL, centre, centre, COLOR, 14, (float) (radius / 5.0));
        EliteBridge.burst(level, centre, true, 6, radius * 0.25);
    }

    private void collapse(ManasSkillInstance instance, ServerPlayer owner, ServerLevel level, Vec3 centre, double radius, boolean mastered) {
        double mastery = masteryFrac(instance);
        for (LivingEntity t : EliteBridge.foes(owner, centre, radius)) {
            double d = t.position().add(0, t.getBbHeight() / 2, 0).distanceTo(centre);
            EliteBridge.hit(owner, t, MagicRules.GravityWell.collapseDamage(d, radius, mastered) * EliteSkillsConfig.damageScale(), mastery, true);
        }
        EliteBridge.shape(level, VfxShape.GRAVITY_IMPACT, centre, centre, COLOR, 20, (float) (radius / 3.0));
        EliteBridge.sound(level, centre, SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 0.6f);
        instance.getOrCreateTag().putLong(END, 0L);
        instance.markDirty();
    }

    private boolean collapseEarly(ManasSkillInstance instance, ServerPlayer player) {
        var tag = instance.getOrCreateTag();
        ServerLevel level = player.serverLevel();
        if (tag.getLong(END) <= level.getGameTime()) {
            EliteBridge.fail(player, SkillMsg.t("gravity_well.none", "You have no gravity well open."));
            return false;
        }
        collapse(instance, player, level, new Vec3(tag.getDouble(X), tag.getDouble(Y), tag.getDouble(Z)), tag.getDouble(RADIUS), masteredBy(instance, player));
        return true;
    }
}
