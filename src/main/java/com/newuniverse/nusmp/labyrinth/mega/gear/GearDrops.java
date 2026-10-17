package com.newuniverse.nusmp.labyrinth.mega.gear;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.boss.budget.BudgetedBoss;
import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.eco.TurfMarks;
import com.newuniverse.nusmp.labyrinth.seal.FloorWardenEntity;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Why: this is the third {@code LivingDropsEvent} listener of the Mega-Dungeon, beside the 1.36.0 economy (Magic Stones and monster meat) and the 1.54.0
 * faction drops, and it adds only the EQUIPMENT: the owner wants Normal, Special and Rare gear to drop without limit across the floors, "alongside" the
 * stones and the meat, so it is a separate listener at LOW priority that adds one item and touches nothing the other two decided. It applies on exactly
 * their terms: the Mega-Dungeon only, a hostile {@code Enemy} that is neither a boss nor anyone's servant, killed by a player (the damage source's player,
 * else the kill credit), so a monster that died to another monster or to a trap pays no gear and a Shadow Core minion or a tamed pet is no farm. AT MOST ONE
 * piece per kill: the roller lays the three grades' chances end to end on one roll. The exception is the Labyrinth Guardian of a sector (the entity with
 * the scoreboard tag {@code nusmp_sector_guardian}), the decennial boss: it is not an ordinary kill, so it gets no roll but a guarantee, exactly one Rare
 * piece (switch {@code bossGuaranteed}); every other boss drops no standard gear, because bosses have their own loot. The floor for the roll is
 * {@code MegaDungeon.floorAt} where the monster died; Looting is the killer's main-hand level, read the way the economy reads it. The whole decision is in
 * {@link #process}, which takes the random source as an argument so the self-test can replay it with a seed. Never throws out of an event: a failure is
 * logged once and that death drops no gear.
 */
public final class GearDrops {
    private GearDrops() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    /** The scoreboard tag the Labyrinth Guardian carries (set by the guard module): its death is the guaranteed Rare drop. */
    public static final String GUARDIAN_TAG = "nusmp_sector_guardian";

    private static final ResourceLocation WARDEN = ResourceLocation.tryParse(TurfMarks.WARDEN_ID);
    private static final ResourceKey<Enchantment> LOOTING = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace("looting"));

    public static void onDrops(LivingDropsEvent e) {
        try {
            process(e, null);
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] Mega-Dungeon gear drops failed once and are skipped for that death", ex);
        }
    }

    /**
     * Decides and adds the gear of one death.
     *
     * @param dice the random source, or null for the level's own
     * @return the number of pieces added to the drops: 0 or 1, never more
     */
    public static int process(LivingDropsEvent e, Dice dice) {
        if (!(e.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level) || !Labyrinth.inMega(level)) return 0;
        if (!GearConfig.get(GearConfig.ENABLED)) return 0;
        ServerPlayer killer = e.getSource().getEntity() instanceof ServerPlayer sp ? sp : (mob.getKillCredit() instanceof ServerPlayer lp ? lp : null);
        if (killer == null) return 0;
        Dice d = dice != null ? dice : level.getRandom()::nextDouble;
        GearTables tables = GearTablesReload.tables();
        GearSpec spec;
        if (mob.getTags().contains(GUARDIAN_TAG)) {
            if (!GearConfig.get(GearConfig.BOSS_GUARANTEED)) return 0;
            spec = GearRoller.make(d, GearGrade.RARE, null, tables).orElse(null);
        } else {
            if (!(mob instanceof Enemy) || isBoss(mob) || isServant(mob)) return 0;
            int floor = MegaDungeon.floorAt(level, mob.blockPosition());
            spec = GearRoller.roll(d, floor, looting(level, killer), tables, GearConfig.rates()).orElse(null);
        }
        if (spec == null) return 0;
        ItemStack stack = GearFactory.create(spec, level.registryAccess());
        if (stack.isEmpty()) return 0;
        e.getDrops().add(new ItemEntity(level, mob.getX(), mob.getY() + 0.3, mob.getZ(), stack));
        return 1;
    }

    /**
     * Bosses, by the economy's own tests: the DPS-budget interface, the Floor Warden class (which the Labyrinth Guardian extends), the boss and wanderer
     * tags, the shared {@code c:bosses} entity tag and the Floor Warden's id. (The economy keeps its version package-private, so this repeats it; the
     * strings come from the faction layer's single list, {@link TurfMarks}.)
     */
    static boolean isBoss(Mob m) {
        if (m instanceof BudgetedBoss || m instanceof FloorWardenEntity) return true;
        if (TurfMarks.anyOf(m.getTags(), TurfMarks.BOSS_TAGS)) return true;
        if (m.getType().is(Tags.EntityTypes.BOSSES)) return true;
        return WARDEN != null && WARDEN.equals(BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()));
    }

    /** A servant answers to a player (a tamed or owned creature, a Paladin, a shadow soldier, a relic's summon): its death is no loot. */
    static boolean isServant(Mob m) {
        if (m instanceof TamableAnimal t && t.isTame()) return true;
        if (m instanceof OwnableEntity o && o.getOwnerUUID() != null) return true;
        if (TurfMarks.anyOf(m.getTags(), TurfMarks.SERVANT_TAGS)) return true;
        CompoundTag data = m.getPersistentData();
        if (data.isEmpty()) return false;
        for (String key : TurfMarks.SERVANT_KEYS) if (data.contains(key)) return true;
        return false;
    }

    /** The Looting level of the killer's main-hand weapon (0 if it has none or the enchantment is missing). */
    private static int looting(ServerLevel level, Player killer) {
        Optional<? extends Holder<Enchantment>> looting = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(LOOTING);
        return looting.isEmpty() ? 0 : Math.max(0, killer.getMainHandItem().getEnchantments().getLevel(looting.get()));
    }
}
