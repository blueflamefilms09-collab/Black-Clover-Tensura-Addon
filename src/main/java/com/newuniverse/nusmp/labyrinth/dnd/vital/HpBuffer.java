package com.newuniverse.nusmp.labyrinth.dnd.vital;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.boss.budget.BudgetedBoss;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.dnd.DndConfig;
import com.newuniverse.nusmp.labyrinth.seal.SealRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Why: the D&amp;D half of the owner's "Lethality vs HP buffers" brief: the Labyrinth's hostile mobs carry a big hit-point pool, so a fight is
 * attrition and not a one-shot. It is a permanent attribute modifier on MAX_HEALTH, operation ADD_MULTIPLIED_TOTAL with value
 * {@code HP_SCALE - 1}, id {@code nusmp:labyrinth_hp_buffer}. A modifier (and not a {@code setBaseValue}) is the NeoForge-friendly way:
 * it is saved with the entity, shows in tooltips and /attribute, composes with other mods' modifiers instead of overwriting the mob's own
 * base health, and it can be removed by id if the owner turns the buffer off. ADD_MULTIPLIED_TOTAL multiplies the final sum, so a modded mob
 * that already has +health modifiers is scaled as a whole. The hook is {@link EntityJoinLevelEvent} (server side only) and not
 * {@code FinalizeSpawnEvent}: a Sealed Archway, a spawn egg, a command, a wildcard-pool mod and a chunk reload all end in a join, but only
 * natural spawns pass through finalize. It runs once per mob: the entity tag {@value #TAG} is written the first time (and the modifier id is
 * checked too, so a mob that lost its tags still is not buffered twice).
 * <p>
 * Who is excluded, and why: bosses (the Floor Warden has its own 200 HP, {@code configured through LabyrinthConfig}; the Zogratis and Zagred
 * bosses implement {@link BudgetedBoss}, which caps THEIR damage taken per second: multiplying their health as well would double-count the
 * balance), anything in the {@code c:bosses} entity tag (Wither, Ender Dragon, other mods' bosses), anything tagged {@code nusmp_peak_boss}
 * (the Peak Altar's roster), and anything whose BASE health is already boss-sized (the check uses the base value before the modifier,
 * the same 300 as {@code BalanceLaw.isBoss}; after the buffer a 150-HP mob would be mistaken for a boss by every system that tests max health).
 * Only {@link Enemy} mobs are buffered: villagers, pets and the trial-chamber allies stay as they are.
 */
public final class HpBuffer {
    private HpBuffer() {}

    /** Entity tag marking "this mob has been looked at by the buffer"; also read by the Vital Strike as "this mob has a buffer". */
    public static final String TAG = "nusmp_hpbuf";
    /** Entity tag the Peak Altar puts on its boss (kept in step with {@code PeakAltarBlockEntity.BOSS_TAG}). */
    public static final String PEAK_BOSS_TAG = "nusmp_peak_boss";
    public static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "labyrinth_hp_buffer");
    /** A mob whose unbuffered health is at or above this is boss-sized (same number as the balance law). */
    public static final double BOSS_BASE_HEALTH = 300.0;

    /** Game-bus listener: buffers a hostile non-boss mob the first time it joins the Labyrinth. */
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !Labyrinth.in(e.getLevel())) return;
        if (!(e.getEntity() instanceof Mob mob)) return;
        if (!DndConfig.get(DndConfig.HP_BUFFER)) return;
        apply(mob, DndConfig.get(DndConfig.HP_SCALE), e.loadedFromDisk());
    }

    /** Buffer {@code mob} if it qualifies and has none yet. Returns true when it was buffered now. */
    public static boolean apply(Mob mob, double scale, boolean loadedFromDisk) {
        if (!(mob instanceof Enemy) || isBoss(mob) || isBuffered(mob)) return false;
        AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp == null) return false;
        mob.addTag(TAG);
        if (!(scale > 1.0)) return false;                                    // scale 1.0 = nothing to add; the tag still stops a second look
        float before = mob.getMaxHealth();
        hp.addOrReplacePermanentModifier(new AttributeModifier(MODIFIER_ID, scale - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        float after = mob.getMaxHealth();
        if (loadedFromDisk && before > 0f) mob.setHealth(Math.min(after, mob.getHealth() * after / before));   // an old mob keeps its wounds in proportion
        else mob.setHealth(after);                                           // a new mob starts at full health
        return true;
    }

    /** Does this mob carry the buffer? The modifier is the truth (the tag only says it was looked at; a scale of 1.0 adds no modifier). */
    public static boolean isBuffered(LivingEntity e) {
        AttributeInstance hp = e.getAttribute(Attributes.MAX_HEALTH);
        return hp != null && hp.hasModifier(MODIFIER_ID);
    }

    /** Boss-like: has its own balance, so it gets neither the buffer nor the Vital Strike. */
    public static boolean isBoss(LivingEntity e) {
        if (e instanceof BudgetedBoss) return true;
        if (e.getType().is(Tags.EntityTypes.BOSSES)) return true;
        if (e.getType() == SealRegistry.FLOOR_WARDEN.get()) return true;
        if (e.getTags().contains(PEAK_BOSS_TAG)) return true;
        AttributeInstance hp = e.getAttribute(Attributes.MAX_HEALTH);
        if (hp == null) return false;
        double base = hp.getBaseValue();
        return base >= BOSS_BASE_HEALTH;
    }
}
