package com.newuniverse.nusmp.multiverse;

import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.ForbiddenMagic;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpiritBond;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * Grimoire pages open through MASTERY only (never kills). A page unlocks when all three hold:
 * <ol>
 *   <li>mastery: page k of n needs mastery >= k/n of the bar (x the configured scale);</li>
 *   <li>gates: the page's minimum Magic Knight rank (zone pages Intermediate, signature pages Senior by default, per-page overrides
 *       in the config) and, if set, an allowed race;</li>
 *   <li>a free slot: 6 at first, growing to 12 with mastery, +4 with a spirit or devil contract, never above 16.</li>
 * </ol>
 * Pages are checked in order every 2 seconds; a page that is gated is skipped and the next one may still open.
 * Mastery comes from successful casts (as before), altar training, Spirit Channeling time and gold stars from missions.
 */
public final class MasteryPages {
    private MasteryPages() {}

    /** Page slots for this grimoire right now. */
    public static int slots(ServerPlayer p, ManasSkillInstance inst, GrimoireBook book) {
        int floor = MultiverseConfig.get(MultiverseConfig.PAGE_SLOTS_FLOOR), ceil = Math.max(floor, MultiverseConfig.get(MultiverseConfig.PAGE_SLOTS_CEILING));
        int s = floor + (int) Math.floor(book.masteryFrac(inst) * (ceil - floor));
        if (hasContract(p, inst)) s += MultiverseConfig.get(MultiverseConfig.PAGE_SLOTS_CONTRACT_BONUS);
        return Math.min(s, MultiverseConfig.get(MultiverseConfig.PAGE_SLOTS_CAP));
    }

    /** Spirit contract (bonded spirit or Spirit Lord) or devil contract (five-leaf cover or a Forbidden Magic pact). */
    public static boolean hasContract(ServerPlayer p, ManasSkillInstance inst) {
        if (SpiritBond.bonded(p) || SpiritLordSkill.instance(p).isPresent()) return true;
        if (GrimoirePages.coverOf(inst).isForbidden()) return true;
        return ForbiddenMagic.devilOf(p) != null;
    }

    /** Mastery needed (0..1) for family page m of n. The starter page (0) is always open. */
    public static double threshold(int m, int n) {
        if (m <= 0) return 0;
        return Math.min(1.0, (double) m / Math.max(1, n) * MultiverseConfig.get(MultiverseConfig.PAGE_MASTERY_SCALE));
    }

    // ---------------------------------------------------------------- gates
    public record Gate(int minStep, Set<Race> races) {
        public boolean passes(ServerPlayer p) {
            return MultiverseProfile.step(p) >= minStep && (races.isEmpty() || races.contains(MultiverseProfile.race(p)));
        }

        public String describe() {
            String r = minStep <= 0 ? "" : KnightRank.describe(KnightRank.rankOfStep(minStep), KnightRank.classOfStep(minStep));
            if (!races.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Race race : races) sb.append(sb.length() == 0 ? "" : " / ").append(race.displayName);
                r = r.isEmpty() ? sb.toString() : r + ", " + sb;
            }
            return r;
        }
    }

    /** The gate for a page: a per-page override from the config, else by its tier (zone / signature), else none. */
    public static Gate gate(GrimoireBook book, BookPage page) {
        String key = book.getRegistryName().getPath() + ":" + page.id();
        for (String line : MultiverseConfig.get(MultiverseConfig.PAGE_GATES)) {
            int eq = line.indexOf('=');
            if (eq <= 0 || !line.substring(0, eq).trim().equalsIgnoreCase(key)) continue;
            String[] parts = line.substring(eq + 1).split(";");
            int step = Math.max(0, KnightRank.parseStep(parts[0]));
            Set<Race> races = EnumSet.noneOf(Race.class);
            if (parts.length > 1) for (String r : parts[1].split("\\|")) { Race race = Race.byName(r.trim()); if (race != null) races.add(race); }
            return new Gate(step, races);
        }
        String tier = page.costPercent() >= 35 ? MultiverseConfig.get(MultiverseConfig.GATE_SIGNATURE)
                : page.costPercent() >= 20 ? MultiverseConfig.get(MultiverseConfig.GATE_ZONE) : "";
        return new Gate(Math.max(0, KnightRank.parseStep(tier)), EnumSet.noneOf(Race.class));
    }

    // ---------------------------------------------------------------- unlocking
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 40 != 7) return;
        var g = GrimoirePages.grimoireOf(p);
        if (g.isEmpty() || !(g.get().getSkill() instanceof GrimoireBook book)) return;
        tryUnlock(p, g.get(), book);
    }

    /** Opens the first page whose mastery, gates and slot allow it. Returns its mode, or -1. */
    public static int tryUnlock(ServerPlayer p, ManasSkillInstance inst, GrimoireBook book) {
        int n = book.familyCount(), unlocked = 0;
        for (int m = 0; m < n; m++) if (GrimoireBook.isUnlocked(inst, m)) unlocked++;
        if (unlocked >= slots(p, inst, book)) return -1;
        double mastery = book.masteryFrac(inst);
        for (int m = 1; m < n; m++) {
            if (GrimoireBook.isUnlocked(inst, m) || mastery < threshold(m, n)) continue;
            BookPage page = book.page(m);
            Gate gate = gate(book, page);
            if (!gate.passes(p)) {
                remindGate(p, inst, m, page, gate);
                continue;
            }
            GrimoireBook.unlock(inst, m);
            p.sendSystemMessage(Component.literal("Your mastery opens a new page: ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(page.name()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)));
            p.sendSystemMessage(Component.translatable("nusmp.page." + book.getRegistryName().getPath() + "." + page.id()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.8f);
            MultiverseSync.markDirty(p);
            return m;
        }
        return -1;
    }

    /** Tells the player once per page (until it opens) what it waits for. */
    private static void remindGate(ServerPlayer p, ManasSkillInstance inst, int m, BookPage page, Gate gate) {
        var tag = inst.getOrCreateTag();
        int told = tag.getInt("GateTold");
        if ((told & (1 << m)) != 0) return;
        tag.putInt("GateTold", told | (1 << m));
        inst.markDirty();
        p.sendSystemMessage(Component.literal("A page stirs (" + page.name() + "), but it will only open for: " + gate.describe() + ".").withStyle(ChatFormatting.GRAY));
    }

    // ---------------------------------------------------------------- mastery sources besides casting
    /** Altar training: sneak-use your grimoire on a Grimoire Altar. Costs 10% of your magicule; config cooldown. */
    public static boolean altarTraining(ServerPlayer p) {
        var g = GrimoirePages.grimoireOf(p);
        if (g.isEmpty()) return false;
        long now = p.level().getGameTime();
        long ready = p.getPersistentData().getLong("nusmp_altar_training");
        if (now < ready) {
            p.displayClientMessage(Component.literal("You are still spent from training (" + (ready - now + 19) / 20 + "s).").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        var ex = TensuraStorages.getExistenceFrom(p);
        double cost = EnergyHelper.getMaxMagicule(p) * 0.10;
        if (ex == null || ex.getMagicule() < cost) {
            p.displayClientMessage(Component.literal("You need more magicule to train at the altar.").withStyle(ChatFormatting.RED), true);
            return false;
        }
        ex.setMagicule(ex.getMagicule() - cost);
        ex.markDirty();
        int points = MultiverseConfig.get(MultiverseConfig.ALTAR_TRAINING_MASTERY);
        g.get().addMasteryPoint(p, points);
        g.get().markDirty();
        p.getPersistentData().putLong("nusmp_altar_training", now + MultiverseConfig.get(MultiverseConfig.ALTAR_TRAINING_COOLDOWN) * 20L);
        p.displayClientMessage(Component.literal("You train your grimoire at the altar (+" + points + " mastery).").withStyle(ChatFormatting.AQUA), true);
        MultiverseSync.markDirty(p);
        return true;
    }

    /** Spirit Channeling time: one mastery point per N ticks held (called from the channel loop). */
    public static void onChanneling(ServerPlayer p, ManasSkillInstance inst, int heldTicks) {
        int per = Math.max(1, MultiverseConfig.get(MultiverseConfig.CHANNEL_MASTERY_TICKS));
        if (heldTicks > 0 && heldTicks % per == 0) {
            inst.addMasteryPoint(p, 1);
            inst.markDirty();
        }
    }

    /** Missions: gold stars also train the grimoire. */
    public static void onMissionStars(ServerPlayer p, int goldStars) {
        int points = (int) Math.round(goldStars * MultiverseConfig.get(MultiverseConfig.MASTERY_PER_GOLD_STAR));
        if (points <= 0) return;
        GrimoirePages.grimoireOf(p).ifPresent(i -> { i.addMasteryPoint(p, points); i.markDirty(); });
    }
}
