package com.newuniverse.nusmp.saiyan;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skill.DbSkills;
import com.newuniverse.nusmp.skill.NUSkills;
import com.newuniverse.nusmp.skill.ProdigySkills;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.slf4j.Logger;

/**
 * Why: the public door of the Saiyan Prodigy (docs/saiyan_prodigy.md). The skill (package {@code skill}) and the old Dragon Ball skills ask it three kinds of question: "does the hub stand in for the old keys for this
 * player" ({@link #replaces}), "do this Saiyan op for me" ({@link #act}: the same server entry the old keys use, so every old rule stays) and "grant, migrate, evolve" ({@link #grant}, {@link #onLogin},
 * {@link #onSaiyan}, {@link #onEvolved}). It also gives the Saiyan-blooded their small permanent gifts ({@link #second}). Everything is decided on the server and per player; the one static is the logger.
 * <p>
 * Switch: server setting {@code prodigy.prodigyHub}. Off = nothing here acts (the old keys and the old Dragon Ball skills behave as before). The gifts have their own switch, {@code perks.enabled}.
 * Cost: a flag read per call; one skill lookup at login and when the race is taken; two attribute reads a second per Saiyan (only when they differ from what is set).
 */
public final class SaiyanProdigyHub {
    private SaiyanProdigyHub() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** Player data (per player UUID, saved with the player): the Prodigy was granted once. A player who later forgets it is not given it again by the migration. */
    public static final String GIVEN = "nusmp_prodigy_given";
    private static final ResourceLocation PERK_HEALTH = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "prodigy_health"), PERK_ATTACK = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "prodigy_attack");

    public static boolean enabled() { return SaiyanProdigyConfig.hub(); }

    /** True when the hub stands in for the old keys and the old Dragon Ball skills for this entity: the hub is on and the entity is a Saiyan. */
    public static boolean replaces(LivingEntity e) { return enabled() && e instanceof ServerPlayer p && SaiyanStore.isSaiyan(p); }

    // ---------------------------------------------------------------------------------------------------- the old entry, asked from the skill
    /** Asks the Saiyan system for an op exactly as the old key packet does (a real Saiyan, rate limited, the old rules). A press of a non-Saiyan is dropped there. Server thread. */
    public static void act(ServerPlayer p, int op) {
        if (!enabled() || op < SaiyanNet.OP_BLAST || op > SaiyanNet2.LAST) return;
        SaiyanNet.onAction(p, op);
    }

    /** The release of a hold (Kamehameha, Spirit Bomb, Ki Wave): sent even if the hub was switched off meanwhile, so a charge can never be stuck (the Saiyan system never rate limits a release). */
    public static void release(ServerPlayer p, int op) {
        if (op != SaiyanNet.OP_KAME_RELEASE && op != SaiyanNet.OP_SPIRIT_RELEASE && op != SaiyanNet2.OP_WAVE_RELEASE) return;
        SaiyanNet.onAction(p, op);
    }

    /** True while the hold of a mode is still going (the Kamehameha or the Spirit Bomb charge, the Ki Wave channel). */
    public static boolean holding(ServerPlayer p, int releaseOp) {
        SaiyanManager.Run r = SaiyanManager.peekRun(p);
        if (r == null) return false;
        if (releaseOp == SaiyanNet.OP_KAME_RELEASE) return r.charging;
        if (releaseOp == SaiyanNet.OP_SPIRIT_RELEASE) return r.tech.gathering;
        if (releaseOp == SaiyanNet2.OP_WAVE_RELEASE) return r.tech4.waveStart >= 0;
        return false;
    }

    public static boolean awakened(ServerPlayer p) { SaiyanState s = SaiyanStore.peek(p); return s != null && s.saiyan && s.awakened; }

    /** The Goku line: the awakened Saiyan who holds the Ultimate that unlocks the Goku techniques (the old lock of Kamehameha, Instant Transmission, Spirit Bomb ...). */
    public static boolean goku(ServerPlayer p) { SaiyanState s = SaiyanStore.peek(p); return s != null && s.saiyan && s.awakened && SaiyanUltimates.GOKU.equals(s.ultimate); }

    public static double battlePower(ServerPlayer p) { SaiyanState s = SaiyanStore.peek(p); return s == null ? 0.0 : s.battlePower; }

    // ---------------------------------------------------------------------------------------------------- granting and migration
    /** Gives the Saiyan Prodigy if the player has not got it. True if it was given now. Never takes or changes anything else. Server thread. */
    public static boolean grant(ServerPlayer p) {
        try {
            Skills skills = SkillAPI.getSkillsFrom(p);
            if (skills == null) return false;
            ManasSkill prodigy = ProdigySkills.PRODIGY.get();
            if (skills.getSkill(prodigy.getRegistryName()).isPresent()) { p.getPersistentData().putBoolean(GIVEN, true); return false; }
            boolean ok = skills.learnSkill(prodigy.createDefaultInstance(), Component.translatable("anime_multiverse.saiyan.prodigy.granted").withStyle(ChatFormatting.GOLD));
            if (ok) p.getPersistentData().putBoolean(GIVEN, true);
            return ok;
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("Saiyan Prodigy: could not grant it to {} ({})", p.getGameProfile().getName(), e.toString());
            return false;
        }
    }

    /** True when the player owns the Saiyan Prodigy. */
    public static boolean ownsProdigy(ServerPlayer p) {
        try {
            Skills skills = SkillAPI.getSkillsFrom(p);
            return skills != null && skills.getSkill(ProdigySkills.PRODIGY.getId()).isPresent();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    /** True when the player owns any old Dragon Ball skill (the 30 ids) or the Ki skill: what the migration looks for. 31 map lookups. */
    public static boolean ownsOldSkills(ServerPlayer p) {
        try {
            Skills skills = SkillAPI.getSkillsFrom(p);
            if (skills == null) return false;
            for (DeferredHolder<ManasSkill, ?> h : DbSkills.allHolders()) if (skills.getSkill(h.getId()).isPresent()) return true;
            return skills.getSkill(NUSkills.KI.getId()).isPresent();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    /**
     * Login (a flag check once the player has it): a Saiyan gets the Prodigy once ({@code grantToSaiyans}); a player who owns an old Dragon Ball skill gets it once
     * ({@code prodigyMigrate}). The old skills stay owned. Idempotent: the stamp {@link #GIVEN} is on the player, so a player who forgot it is not given it again.
     */
    public static void onLogin(ServerPlayer p) {
        if (!enabled()) return;
        if (p.getPersistentData().getBoolean(GIVEN)) return;
        boolean saiyan = SaiyanProdigyConfig.grantToSaiyans() && SaiyanStore.isSaiyan(p);
        boolean migrate = SaiyanProdigyConfig.migrate() && ownsOldSkills(p);
        if (saiyan || migrate) grant(p);
    }

    /** The player just became a Saiyan (the flag went on): the Prodigy comes with the blood. */
    public static void onSaiyan(ServerPlayer p) {
        if (!enabled() || !SaiyanProdigyConfig.grantToSaiyans()) return;
        if (!p.getPersistentData().getBoolean(GIVEN)) grant(p);
    }

    /** The Saiyan-blooded evolution just finished: the Ki awakens at once ({@code evolutionAwakens}), so the new Saiyan can use the Prodigy now. */
    public static void onEvolved(ServerPlayer p) {
        if (!enabled() || !SaiyanProdigyConfig.evolutionAwakens()) return;
        SaiyanState s = SaiyanStore.peek(p);
        if (s == null || !s.saiyan || s.awakened) return;
        SaiyanManager.awaken(p, s, SaiyanManager.runOf(p));
    }

    /** What a forwarding old key or skill tells the player. */
    public static void forwarded(ServerPlayer p) { p.displayClientMessage(Component.translatable("anime_multiverse.saiyan.prodigy.forwarded"), true); }

    // ---------------------------------------------------------------------------------------------------- the small gifts
    /** Once a second for a Saiyan (called from the Saiyan second): the permanent gifts. No hunger, no food effect of any kind. */
    static void second(ServerPlayer p, SaiyanState s) {
        if (!enabled() || !SaiyanProdigyConfig.perks()) { clearPerks(p); return; }
        set(p, Attributes.MAX_HEALTH, PERK_HEALTH, SaiyanProdigyConfig.perkHealth(), AttributeModifier.Operation.ADD_VALUE);
        set(p, Attributes.ATTACK_DAMAGE, PERK_ATTACK, SaiyanProdigyConfig.perkAttack(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (s.awakened) SaiyanKi.regen(p, SaiyanProdigyConfig.perkKiRegen());
    }

    static void clearPerks(ServerPlayer p) {
        unset(p, Attributes.MAX_HEALTH, PERK_HEALTH);
        unset(p, Attributes.ATTACK_DAMAGE, PERK_ATTACK);
    }

    private static void set(ServerPlayer p, Holder<Attribute> attr, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance a = p.getAttribute(attr);
        if (a == null) return;
        if (!(amount > 0.0)) { if (a.getModifier(id) != null) a.removeModifier(id); return; }
        AttributeModifier cur = a.getModifier(id);
        if (cur != null && Math.abs(cur.amount() - amount) < 1.0E-6) return;
        a.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
    }

    private static void unset(ServerPlayer p, Holder<Attribute> attr, ResourceLocation id) {
        AttributeInstance a = p.getAttribute(attr);
        if (a != null && a.getModifier(id) != null) a.removeModifier(id);
    }
}
