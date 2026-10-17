package com.newuniverse.nusmp.labyrinth.mega.crucible;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.theme.JsonTrees;
import com.newuniverse.nusmp.labyrinth.mega.theme.ThemeConfig;
import com.newuniverse.nusmp.labyrinth.mega.theme.ThemeDefs;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.List;
import java.util.Optional;

/**
 * Why: the SPIRIT catastrophe summons "a massive Cursed Spirit" (the owner's words), and what that is must be data the owner can extend: the roster
 * file {@code data/nusmp/mega/cursed_spirits.json}, read by this reload listener into {@link SpiritRoster} entries (the pure parser and the weighted,
 * seeded, skip-the-missing draw live there and are tested). The creature is chosen when the pick is made, from the sector's seed and the chooser, so
 * the same vault always summons the same kind; it is created like the Peak Altar creates its boss (by id, finalised, persistent, tagged) and then made
 * a spirit: renamed with its roster name, glowing, tagged {@value #TAG}, its health and attack scaled by the sector's threat on top of its roster
 * {@code healthScale} (attribute modifiers, so they show in tooltips and can be removed by id), stripped of the skills the boss rules forbid
 * ({@link SpiritRules}), and held still and unhurtable for the three seconds of its summoning circle so a party that wants to run has the time. It is
 * placed with the spawn type EVENT (it is a scripted event, not a natural spawn, so no spawn-blocking rule of another system applies). Nothing here
 * is a custom entity: a Cursed Spirit is a borrowed body with a new name and a curse on its health bar.
 * <p>
 * How the other systems must see it: it is a BOSS, not a servant. It carries only the scoreboard tag {@value #TAG}: never {@code nusmp_servant} (it
 * answers to nobody) and none of the tags other systems already use for their own bosses ({@code nusmp_peak_boss} fires the Peak Altar's quest event and the
 * economy's boss rules, {@code nusmp_floor_boss} feeds the Genesis Core drops and the old boss rules, {@code nusmp_wanderer} is the encounter director's
 * and would be counted and despawned by it). The ecology's turf-war eligibility reads a fixed list of boss tags ({@code TurfMarks.BOSS_TAGS}), so the lead
 * adds this tag to that list; until then the faction rules treat the body as the wild creature it was. The Labyrinth's health buffer still applies to a
 * hostile body (it is not excluded as a boss there), on top of the roster scale and the threat scaling.
 */
public final class CursedSpirits implements ResourceManagerReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    /** The persistent entity tag every spirit carries; the heal rule, the no-drops rule and the orphan sweep read it. */
    public static final String TAG = "nusmp_cursed_spirit";
    static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath("nusmp", "mega/cursed_spirits.json");
    private static final ResourceLocation HEALTH_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "cursed_spirit_health"),
            DAMAGE_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "cursed_spirit_damage");
    private static volatile List<SpiritRoster.Entry> ROSTER = List.of();

    public static List<SpiritRoster.Entry> roster() { return ROSTER; }

    /** Replaces the roster (the reload, and tests). */
    public static void replace(List<SpiritRoster.Entry> entries) { ROSTER = List.copyOf(entries); }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        List<SpiritRoster.Entry> entries = List.of();
        try {
            Optional<Resource> res = manager.getResource(FILE);
            if (res.isPresent()) {
                try (Reader reader = res.get().openAsReader()) {
                    SpiritRoster.Parsed p = SpiritRoster.parse(JsonTrees.read(reader));
                    for (String w : p.warnings()) LOG.warn("[nusmp] mega spirits: {}", w);
                    entries = p.entries();
                }
            } else {
                LOG.warn("[nusmp] mega spirits: {} not found: the SPIRIT catastrophe is off", FILE);
            }
        } catch (Exception e) {
            LOG.warn("[nusmp] mega spirits: {} could not be read: {}", FILE, e.toString());
        }
        replace(entries);
        LOG.info("[nusmp] mega spirits: {} roster entries", entries.size());
    }

    /** Does an entity type with this id exist in this game (an optional mod may be missing)? */
    static boolean exists(String id) { return EntityType.byString(id).isPresent(); }

    /** Can a spirit be summoned at all: at least one roster entry names an entity that exists. */
    public static boolean available() {
        for (SpiritRoster.Entry e : ROSTER) if (exists(e.entity())) return true;
        return false;
    }

    /**
     * Creates, scales and places the spirit of this sector's vault, asleep (no AI, invulnerable) for its summoning circle; null when no roster entity
     * exists, the type cannot be made, or the world refused the entity. The caller wakes it with {@link #wake}.
     */
    static Mob spawn(ServerLevel level, Sector s, BlockPos at, ServerPlayer chooser) {
        long seed = CrucibleRules.drawSeed(s.seed ^ 0x2545F4914F6CDD1DL, chooser == null ? null : chooser.getUUID());
        SpiritRoster.Entry entry = SpiritRoster.pick(ROSTER, seed, CursedSpirits::exists);
        return entry == null ? null : spawnEntry(level, CrucibleRules.dangerThreat(s.threat, ThemeDefs.get(s.theme).danger()), entry, at);
    }

    /** The spirit of one roster entry (the draw is {@link #spawn}'s business; the self-test builds every entry in turn to find one that cannot be made). */
    static Mob spawnEntry(ServerLevel level, int threat, SpiritRoster.Entry entry, BlockPos at) {
        Optional<EntityType<?>> type = EntityType.byString(entry.entity());
        if (type.isEmpty()) return null;
        Entity made = type.get().create(level);
        if (!(made instanceof Mob mob)) {
            if (made != null) made.discard();
            return null;
        }
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
        scale(mob, threat, entry.healthScale());                                      // after finalizeSpawn: some bodies roll their own stats there
        mob.setCustomName(Component.literal(entry.name()));
        mob.setCustomNameVisible(true);
        mob.setGlowingTag(true);
        mob.addTag(TAG);
        mob.setPersistenceRequired();
        mob.setNoAi(true);                                                            // the summoning circle: it stands still and cannot be hurt
        mob.setInvulnerable(true);
        SpiritRules.sweepSkills(mob);
        if (!level.addFreshEntity(mob)) return null;
        return mob;
    }

    /** Scales the spirit's maximum health and attack damage by the sector's threat (and the roster's healthScale); a body without the attribute is left alone. */
    static void scale(Mob mob, int threat, double healthScale) {
        double hm = CrucibleRules.spiritHealthMultiplier(threat, healthScale, ThemeConfig.get(ThemeConfig.SPIRIT_HEALTH_PER_THREAT));
        AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null && Math.abs(hm - 1.0) > 1.0E-6) {
            hp.addOrReplacePermanentModifier(new AttributeModifier(HEALTH_ID, hm - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        mob.setHealth(mob.getMaxHealth());
        double dm = CrucibleRules.spiritDamageMultiplier(threat, ThemeConfig.get(ThemeConfig.SPIRIT_DAMAGE_PER_THREAT));
        AttributeInstance atk = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null && dm > 1.0) atk.addOrReplacePermanentModifier(new AttributeModifier(DAMAGE_ID, dm - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /** Ends the summoning circle: the spirit can be hurt and thinks again, and its first target is the player given (the one who took the relic). */
    static void wake(Mob mob, ServerPlayer target) {
        mob.setInvulnerable(false);
        mob.setNoAi(false);
        if (target != null) mob.setTarget(target);
    }
}
