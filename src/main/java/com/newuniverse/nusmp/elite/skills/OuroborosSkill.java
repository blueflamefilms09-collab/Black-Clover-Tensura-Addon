package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.elite.skills.rules.SnapshotRing;
import com.newuniverse.nusmp.elite.skills.rules.UltimateRules;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ouroboros (Ultimate, designed for this mod from the serpent that eats its tail; the original addon's source was not available): the cycle that undoes itself. The skill remembers where you were
 * and how healthy, one moment a second, for the last twenty seconds. Mode 1, Rewind: return to where you stood ten seconds ago (fifteen mastered), with at least half of your health, cleansed
 * and no longer burning. Mode 2, Cycle: reset the cooldown of EVERY other skill you own and refill your magicules to half (75% mastered), once in ten minutes; each Cycle is recorded as a
 * skill reset ({@link ResetLedger}), which the {@code reset_count} evolution trigger reads. PASSIVE: when a killing blow lands (not the void, not /kill) and the cycle is ready, Rewind happens
 * by itself and the death is refused (once per five minutes; config {@code ouroborosAutoRewind}). The Time Magic effects are not used. Numbers: {@link UltimateRules.Ouroboros}. Ouroboros is the evolution
 * of Ancient Soul in data/nusmp/elite/skills/evolutions.json.
 */
public class OuroborosSkill extends EliteSkill {
    static final SkillText TEXT = SkillText.of("ouroboros", "Ouroboros",
            "Ultimate. The cycle undoes itself: Rewind returns you to where and how you were ten seconds ago, Cycle resets every other cooldown of yours, and a killing blow can be undone by itself once in a while.");

    static final String AUTO_UNTIL = "AutoUntil";
    private static final Map<UUID, SnapshotRing> RINGS = new HashMap<>();

    public OuroborosSkill() { super(SkillType.ULTIMATE, TEXT, true); }

    @Override
    protected List<EliteMode> modes() {
        return List.of(EliteMode.of("ouroboros.rewind", "Rewind", UltimateRules.Ouroboros.REWIND_COST, UltimateRules.Ouroboros.REWIND_COOLDOWN),
                EliteMode.of("ouroboros.cycle", "Cycle", UltimateRules.Ouroboros.CYCLE_COST, UltimateRules.Ouroboros.CYCLE_COOLDOWN));
    }

    @Override protected int color() { return 0xFFB06AFF; }

    // ---------------------------------------------------------------- the memory
    /** Called every tick for an owner: remembers one moment a second. */
    static void sample(ServerPlayer p) {
        if (!p.isAlive()) return;
        SnapshotRing ring = RINGS.computeIfAbsent(p.getUUID(), k -> new SnapshotRing(UltimateRules.Ouroboros.RING_CAPACITY));
        ring.add(new SnapshotRing.Sample(p.level().getGameTime(), p.getHealth(), p.getX(), p.getY(), p.getZ(), GaiaWorld.dim(p.level())), UltimateRules.Ouroboros.SAMPLE_GAP);
    }

    static void forget(UUID id) { RINGS.remove(id); }

    static void clearAll() { RINGS.clear(); }

    static int remembered(UUID id) { SnapshotRing r = RINGS.get(id); return r == null ? 0 : r.size(); }

    // ---------------------------------------------------------------- the powers
    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, int mode) {
        boolean ownMastered = masteredBy(instance, player);
        if (mode == 0) {
            if (!rewind(player, ownMastered, false)) {
                EliteBridge.fail(player, SkillMsg.t("ouroboros.nothing", "The cycle has no memory of you yet."));
                return false;
            }
            return true;
        }
        int cleared = 0;
        for (ManasSkillInstance other : SkillAPI.getSkillsFrom(player).getLearnedSkills()) {
            if (other.getSkillId().equals(instance.getSkillId())) continue;
            for (int m = 0; m < Math.max(1, other.getModes()); m++) {
                if (other.getCoolDown(m) > 0) { other.setCoolDown(0, m); cleared++; }
            }
            other.markDirty();
        }
        double max = EliteUtil.maxMagicule(player), target = max * UltimateRules.Ouroboros.cycleMagiculeShare(ownMastered), have = EliteUtil.magicule(player);
        if (have < target) EliteUtil.addMagicule(player, target - have);
        int resets = ResetLedger.record(player, "ouroboros_cycle");
        ServerLevel level = player.serverLevel();
        EliteBridge.shape(level, VfxShape.SPACE_RINGGATE, player.position(), player.position().add(0, 1, 0), 0xFFB06AFF, 40, 1.2f);
        EliteBridge.burst(level, player.position().add(0, 1, 0), false, 16, 0.8);
        EliteBridge.burst(level, player.position().add(0, 1, 0), true, 16, 0.8);
        EliteBridge.sound(level, player.position(), SoundEvents.BEACON_DEACTIVATE, 1.0f, 1.4f);
        EliteBridge.actionbar(player, SkillMsg.t("ouroboros.cycle", "The cycle turns: %s cooldowns undone (reset number %s).", cleared, resets).withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** Puts the player back where and how they were. {@code auto} is the death-refusing version. Returns false if there is no memory to go back to. */
    static boolean rewind(ServerPlayer p, boolean ownMastered, boolean auto) {
        SnapshotRing ring = RINGS.get(p.getUUID());
        if (ring == null) return false;
        long now = p.level().getGameTime();
        SnapshotRing.Sample s = ring.at(now, UltimateRules.Ouroboros.rewindTicks(ownMastered));
        if (s == null) return false;
        double health = UltimateRules.Ouroboros.restoredHealth(s.health(), p.getMaxHealth());
        if (auto || health > p.getHealth()) p.setHealth((float) Math.min(p.getMaxHealth(), Math.max(auto ? 1.0 : p.getHealth(), health)));
        EliteBridge.cleanse(p);
        p.clearFire();
        p.fallDistance = 0.0f;
        ServerLevel level = p.serverLevel();
        EliteBridge.shape(level, VfxShape.SPACE_RINGGATE, p.position(), p.position().add(0, 1, 0), 0xFFB06AFF, 24, 1.0f);     // where you were
        if (s.dim().equals(GaiaWorld.dim(level)) && p.distanceToSqr(s.x(), s.y(), s.z()) > 1.0) p.teleportTo(s.x(), s.y(), s.z());
        EliteBridge.shape(level, VfxShape.SPACE_RINGGATE, p.position(), p.position().add(0, 1, 0), 0xFFB06AFF, 24, 1.0f);     // where you are again
        EliteBridge.burst(level, p.position().add(0, 1, 0), false, 12, 0.6);
        EliteBridge.sound(level, p.position(), SoundEvents.PORTAL_TRAVEL, 0.3f, 1.8f);
        ring.clear();                                                           // the moments after the one you returned to never happened
        EliteBridge.actionbar(p, SkillMsg.t("ouroboros.rewound", "The cycle returns you to where you were.").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        return true;
    }

    // ---------------------------------------------------------------- the passive
    @Override
    public boolean onDeath(ManasSkillInstance instance, LivingEntity owner, DamageSource source) {
        if (!(owner instanceof ServerPlayer player) || !EliteSkillsConfig.enabled() || !EliteSkillsConfig.get(EliteSkillsConfig.OUROBOROS_AUTO_REWIND)) return true;
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;                                  // the void and /kill are not undone
        long now = player.level().getGameTime();
        var tag = instance.getOrCreateTag();
        if (now < tag.getLong(AUTO_UNTIL)) return true;
        if (!rewind(player, masteredBy(instance, player), true)) return true;
        tag.putLong(AUTO_UNTIL, now + UltimateRules.Ouroboros.AUTO_COOLDOWN);
        instance.markDirty();
        return false;                                                                                         // the death is refused
    }
}
