package com.newuniverse.nusmp.multiverse;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Wizard King's own skill (Unique; only the holder of the crown has it, see {@link WizardKing}). Five abilities, cycle with the
 * mode key: Sovereign Decree (a king's pressure on foes, a blessing on allies), Crown of Mana (a full Mana Zone for 60 s), Sovereign
 * Strike (a piercing bolt 40 blocks long), Royal Guard (eight seconds shielded from every spell), Return to the Capital (home at once).
 * Passive: all damage -10%.
 */
public class WizardKingSkill extends Skill {
    private static final String[] NAMES = {"Sovereign Decree", "Crown of Mana", "Sovereign Strike", "Royal Guard", "Return to the Capital"};
    private static final String[] IDS = {"sovereign_decree", "crown_of_mana", "sovereign_strike", "royal_guard", "return_to_the_capital"};
    private static final int[] COOLDOWN_SECONDS = {20, 90, 15, 60, 300};
    private static final int GOLD = 0xFFFFD86B;

    public WizardKingSkill() { super(SkillType.UNIQUE); }

    @Override public ResourceLocation getSkillIcon() { return ResourceLocation.fromNamespaceAndPath("nusmp", "textures/skill/grimoire/storm.png"); }
    @Override public int getModes(ManasSkillInstance i) { return NAMES.length; }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean reverse) { return Math.floorMod(mode + (reverse ? -1 : 1), NAMES.length); }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return IDS[Math.floorMod(mode, IDS.length)]; }
    @Override public Component getModeName(ManasSkillInstance i, int mode) {
        int m = Math.floorMod(mode, NAMES.length);
        int cd = i.getCoolDown(m);
        return Component.literal(cd > 0 ? NAMES[m] + " (" + cd + "s)" : NAMES[m]);
    }
    @Override public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) { return 0; }
    @Override public boolean canTick(ManasSkillInstance i, LivingEntity e) { return false; }

    @Override
    public void onPressed(ManasSkillInstance i, LivingEntity e, int key, int mode) {
        if (!(e instanceof ServerPlayer p) || !WizardKing.isKing(p)) return;
        mode = Math.floorMod(mode, NAMES.length);
        if (i.onCoolDown(mode)) {
            p.displayClientMessage(Component.literal(NAMES[mode] + " is recharging (" + i.getCoolDown(mode) + "s).").withStyle(ChatFormatting.RED), true);
            return;
        }
        boolean done = switch (mode) {
            case 0 -> decree(p);
            case 1 -> crownOfMana(p);
            case 2 -> strike(p);
            case 3 -> guard(p);
            default -> returnHome(p);
        };
        if (!done) return;
        i.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(COOLDOWN_SECONDS[mode] * 20), mode);
        i.markDirty();
    }

    @Override public boolean onHeld(ManasSkillInstance i, LivingEntity e, int heldTicks, int mode) { return true; }
    @Override public void onRelease(ManasSkillInstance i, LivingEntity e, int heldTicks, int keyNumber, int mode) { }

    /** The Wizard King shrugs a tenth off everything, and a spell wholly off while the Royal Guard stands. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (owner.level().getGameTime() < owner.getPersistentData().getLong("nusmp_royal_guard") && AntiMagic.isMagic(source)) { amount.set(0f); return true; }
        amount.set(amount.get() * 0.9f);
        return true;
    }

    // ---------------------------------------------------------------- the five abilities
    private static boolean decree(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        double r = 16;
        for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), x -> x.isAlive() && !x.isSpectator() && x.distanceToSqr(p) <= r * r)) {
            if (t == p || t.isAlliedTo(p) || t instanceof ServerPlayer) {
                t.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
                t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            } else {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2));
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
            }
        }
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, p.position().add(0, 1, 0), p.position().add(0, 2, 0), GOLD, 30, 2.6f);
        sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.6f, 0.6f);
        p.displayClientMessage(Component.literal("Sovereign Decree: kneel.").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    private static boolean crownOfMana(ServerPlayer p) {
        ManaLaw.forceZone(p, 1200);
        return true;
    }

    private static boolean strike(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getViewVector(1f).scale(40));
        for (LivingEntity t : GrimoireBook.along(p, eye, end, 1.8)) {
            if (t == p || t.isAlliedTo(p)) continue;
            t.hurt(p.damageSources().indirectMagic(p, p), BalanceLaw.damage(p, t, 30f, 1.0));
        }
        VfxSpawn.send(sl, VfxShape.LIGHT_BLADE, eye.add(0, -0.3, 0), end, GOLD, 12, 2.2f);
        sl.playSound(null, p.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2f, 1.6f);
        return true;
    }

    private static boolean guard(ServerPlayer p) {
        p.getPersistentData().putLong("nusmp_royal_guard", p.level().getGameTime() + 160);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 4));
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.MAGIC_CIRCLE, p, p.position().add(0, 0.08, 0), GOLD, 160, 1.6f);
        p.displayClientMessage(Component.literal("Royal Guard: no spell can touch you for 8 s.").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    private static boolean returnHome(ServerPlayer p) {
        ServerLevel target = p.getServer().overworld();
        BlockPos pos = p.getRespawnPosition();
        if (pos == null) pos = target.getSharedSpawnPos();
        VfxSpawn.send(p.serverLevel(), VfxShape.MAGIC_CIRCLE_EXPLOSION, p.position(), p.position().add(0, 1, 0), GOLD, 20, 1.4f);
        p.teleportTo(target, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, p.getYRot(), p.getXRot());
        VfxSpawn.send(target, VfxShape.MAGIC_CIRCLE_EXPLOSION, p.position(), p.position().add(0, 1, 0), GOLD, 20, 1.4f);
        return true;
    }
}
