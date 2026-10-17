package com.newuniverse.nusmp.avatar.cast;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.avatar.AvatarAiConfig;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.aptitude.Aptitude;
import com.newuniverse.nusmp.avatar.aptitude.AptitudeCatalog;
import com.newuniverse.nusmp.avatar.chatter.Rng;
import com.newuniverse.nusmp.avatar.goal.AvatarSignatures;
import com.newuniverse.nusmp.avatar.outcome.AvatarUniqueSkill;
import com.newuniverse.nusmp.avatar.yandere.YandereFrenzyHandler;
import com.newuniverse.nusmp.entity.TensuraCaster;
import com.newuniverse.nusmp.entity.zogratis.SpellSeal;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Why: the owner wants the Avatar to "press the keybinds" of what the creator gave it in the Soul Forge, so the Ultimate and Unique skill it was made with, the
 * Magic Aptitudes it was born with and the move of its awakened perk have to be things the Avatar actually uses in a fight, in the order the creator would
 * rank them, with cooldowns and a price. This class builds that one ordered loadout for an Avatar from the entity's own facts and presses the best ready entry:
 * first the Ultimate, then the Unique skill, then the perk, then the Tensura skills of each aptitude, then the pages of each aptitude's grimoire, then the
 * creator's own grimoire, and last a plain Tensura magic (the 1.51.0 fallback, kept). What is chosen is decided by the pure {@link LoadoutPlanner}; what a page is
 * allowed to do stays under {@link AvatarSkillList}; the books are those of {@link SimulatedPlayerCaster}; a Tensura skill is taught to the Avatar through ManasCore
 * and pressed through {@link TensuraCaster} (the mod's existing way for a mob to use Tensura skills: press, hold, release). Each press is paid from the Avatar's own
 * magicule pool and starts its own cooldown; a skill that throws is dropped for good by {@link TensuraCaster}, and the next best entry is tried.
 */
public final class VirtualSkillLoadout {
    private static final Logger LOG = LogUtils.getLogger();

    private VirtualSkillLoadout() {}

    /** Ticks a pressed Tensura skill is held before it is released (charge-type skills need a moment). */
    static final int HOLD_TICKS = 8;
    /** A press that failed keeps that entry out for this long, so the loadout moves on to the next one. */
    static final int FAIL_BLOCK_TICKS = 100;

    /** What was pressed. {@code shout} is the name for the chat line, or null when the move already said it. */
    public record Pressed(LoadoutPlanner.Entry entry, String shout) {}

    // ---------------------------------------------------------------- can it cast at all
    /** Does the Avatar have anything it could cast (the weapon AI asks before it turns a grimoire in the hand into a mage's stance)? */
    public static boolean canCastAnything(AvatarEntity a) {
        if (!AvatarAiConfig.castEnabled()) return false;
        if (AvatarAiConfig.loadoutEnabled() && (!a.chosenUltimate().isEmpty() || !a.chosenUnique().isEmpty() || a.perk() != null || !a.magicAptitudes().isEmpty())) return true;
        SimulatedPlayerCaster caster = SimulatedPlayerCaster.of(a);
        return caster != null && caster.hasGrimoire(a);
    }

    // ---------------------------------------------------------------- the entries
    /** The non-page entries (the skills and the perk), in loadout order, ready or not. */
    static List<LoadoutPlanner.Entry> skillEntries(AvatarEntity a) {
        List<LoadoutPlanner.Entry> out = new ArrayList<>();
        addSkill(out, LoadoutPlanner.Kind.ULTIMATE, a.chosenUltimate(), 0);
        addSkill(out, LoadoutPlanner.Kind.UNIQUE, a.chosenUnique(), 0);
        AvatarUniqueSkill.Perk perk = a.perk();
        if (perk != null) {
            boolean close = perk.signature() != AvatarUniqueSkill.Signature.SOUL_FLAME;
            out.add(new LoadoutPlanner.Entry(LoadoutPlanner.Kind.PERK, "perk:" + perk.id(), perk.name(), 0, 0, SkillRoles.Role.ATTACK, 0.0, perk.cooldownTicks(), close,
                    LoadoutPlanner.rankOf(LoadoutPlanner.Kind.PERK, 0)));
        }
        List<String> aptitudes = a.magicAptitudes();
        for (int i = 0; i < aptitudes.size(); i++) {
            Aptitude ap = AptitudeCatalog.byId(aptitudes.get(i));
            if (ap == null) continue;
            for (String skill : ap.skills()) addSkill(out, LoadoutPlanner.Kind.APTITUDE_SKILL, skill, i);
        }
        return LoadoutPlanner.order(out);
    }

    private static void addSkill(List<LoadoutPlanner.Entry> out, LoadoutPlanner.Kind kind, String id, int index) {
        if (id == null || id.isBlank()) return;
        ResourceLocation rl = ResourceLocation.tryParse(id.contains(":") ? id : "tensura:" + id);
        if (rl == null) return;
        String key = rl.toString();
        for (LoadoutPlanner.Entry e : out) if (e.id().equals(key)) return;                    // the same skill twice is one entry (the better rank wins: it came first)
        SkillRoles.Role role = SkillRoles.role(rl.getPath());
        if (role == SkillRoles.Role.NONE) return;                                              // a skill that opens a screen or reads the camera is never pressed
        boolean ultimate = kind == LoadoutPlanner.Kind.ULTIMATE;
        double cost = AvatarAiConfig.loadoutSkillCostPercent() * (ultimate ? 2.0 : 1.0);
        out.add(new LoadoutPlanner.Entry(kind, key, pretty(rl.getPath()), 0, 0, role, cost, SkillRoles.baseCooldown(role, ultimate), false, LoadoutPlanner.rankOf(kind, index)));
    }

    /** The cooldown key of an entry: pages use their page id (as ever), the rest a prefixed key. */
    static String keyOf(LoadoutPlanner.Entry e) {
        return switch (e.kind()) {
            case APTITUDE_PAGE, CREATOR_PAGE -> e.id();
            case PERK -> e.id();
            default -> "sk:" + e.id();
        };
    }

    private static boolean inUsefulRange(AvatarEntity a, LivingEntity target, AvatarUniqueSkill.Perk perk) {
        if (target == null) return true;
        double d = a.distanceTo(target);
        double reach = switch (perk.signature()) {
            case SHOCKWAVE -> 4.5;
            case DASH_STRIKE -> 14.0;
            case SOUL_FLAME -> 18.0;
        };
        return d <= reach && a.hasLineOfSight(target);
    }

    /** Is this skill one the loadout teaches the Avatar by itself (its Soul Forge Unique and Ultimate picks, or a skill of one of its aptitudes)? */
    public static boolean isLoadoutSkill(AvatarEntity a, ResourceLocation id) {
        if (id == null) return false;
        String key = id.toString();
        if (key.equals(a.chosenUnique()) || key.equals(a.chosenUltimate())) return true;
        for (String aptitude : a.magicAptitudes()) {
            Aptitude ap = AptitudeCatalog.byId(aptitude);
            if (ap == null) continue;
            for (String skill : ap.skills()) if (key.equals(skill.contains(":") ? skill : "tensura:" + skill)) return true;
        }
        return false;
    }

    /** The names of the Avatar's skill entries in order (for the info lines): "Raphael, Gluttony, Fire Lance". */
    public static String describe(AvatarEntity a) {
        List<LoadoutPlanner.Entry> all = skillEntries(a);
        if (all.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < all.size() && i < 6; i++) sb.append(i == 0 ? "" : ", ").append(all.get(i).label());
        if (all.size() > 6) sb.append(", ...");
        return sb.toString();
    }

    // ---------------------------------------------------------------- pressing
    /**
     * Presses the best ready entry at {@code target}. {@code opening} is true at the start of a fight (a buff goes first then). Up to three entries are tried: one
     * that fails is put out for a few seconds and the next best is tried. Returns what was pressed, or null when nothing was ready or nothing worked.
     */
    public static Pressed press(AvatarEntity a, LivingEntity target, boolean opening, Rng rng) {
        if (!AvatarAiConfig.castEnabled() || target == null || a.level().isClientSide || SpellSeal.sealed(a)) return null;
        long now = a.level().getGameTime();
        double pool = SimulatedPlayerCaster.poolNow(a);
        double max = SimulatedPlayerCaster.poolMax(a);
        SimulatedPlayerCaster caster = SimulatedPlayerCaster.of(a);

        List<LoadoutPlanner.Entry> ready = new ArrayList<>();
        if (AvatarAiConfig.loadoutEnabled()) {
            for (LoadoutPlanner.Entry e : skillEntries(a)) {
                if (SimulatedPlayerCaster.cooldownLeft(a, keyOf(e), now) > 0) continue;
                if (e.kind() == LoadoutPlanner.Kind.PERK && !inUsefulRange(a, target, a.perk())) continue;
                if (!a.frenzied() && !AvatarManaPool.canPay(pool, max * e.costPercent() / 100.0)) continue;
                ready.add(e);
            }
        }
        boolean skillsReady = !ready.isEmpty();
        if (caster != null) ready.addAll(caster.pageEntries(a));

        LoadoutPlanner.Ctx ctx = new LoadoutPlanner.Ctx(a.distanceTo(target), target.getMaxHealth(), a.getHealth() / Math.max(1.0f, a.getMaxHealth()), opening);
        Set<AvatarPersonality> personalities = personalities(a);
        boolean tensuraFirst = !skillsReady && !ready.isEmpty() && rng.nextInt(5) == 0;       // the 1.51.0 variety: now and then plain Tensura magic instead of a page
        if (!tensuraFirst) {
            for (int attempt = 0; attempt < 3 && !ready.isEmpty(); attempt++) {
                int i = LoadoutPlanner.pick(ready, ctx, personalities, rng);
                if (i < 0) break;
                LoadoutPlanner.Entry e = ready.remove(i);
                Pressed done = pressEntry(a, caster, target, e, now, pool, max);
                if (done != null) return done;
                SimulatedPlayerCaster.blockFor(a, keyOf(e), FAIL_BLOCK_TICKS, now);
            }
        }
        ResourceLocation tensura = SimulatedPlayerCaster.castTensura(a, target);
        if (tensura != null) {
            return new Pressed(new LoadoutPlanner.Entry(LoadoutPlanner.Kind.TENSURA_MAGIC, tensura.toString(), pretty(tensura.getPath()), 0, 0, SkillRoles.Role.ATTACK, 0.0, 100, false,
                    LoadoutPlanner.rankOf(LoadoutPlanner.Kind.TENSURA_MAGIC, 0)), pretty(tensura.getPath()));
        }
        return null;
    }

    private static Pressed pressEntry(AvatarEntity a, SimulatedPlayerCaster caster, LivingEntity target, LoadoutPlanner.Entry e, long now, double pool, double max) {
        switch (e.kind()) {
            case APTITUDE_PAGE, CREATOR_PAGE -> {
                if (caster == null) return null;
                return caster.castPage(a, e.source(), e.mode(), target) == SimulatedPlayerCaster.Result.OK ? new Pressed(e, e.label()) : null;
            }
            case PERK -> {
                AvatarUniqueSkill.Perk perk = a.perk();
                if (perk == null) return null;
                AvatarSignatures.perform(a, target, perk);                                  // says the move's name itself
                SimulatedPlayerCaster.startCooldown(a, keyOf(e), perk.cooldownTicks(), now);
                return new Pressed(e, null);
            }
            case TENSURA_MAGIC -> {
                return null;                                                                // never listed; the fallback runs in press()
            }
            default -> {
                return pressSkill(a, target, e, now, pool, max) ? new Pressed(e, e.label()) : null;
            }
        }
    }

    private static boolean pressSkill(AvatarEntity a, LivingEntity target, LoadoutPlanner.Entry e, long now, double pool, double max) {
        ResourceLocation id = ResourceLocation.tryParse(e.id());
        if (id == null || !learn(a, id)) return false;
        TensuraCaster.ensureMana(a, max);                                                    // the skill spends from Tensura's own pool; the Avatar's real price is paid below
        ResourceLocation cast = TensuraCaster.cast(a, target, List.of(id), HOLD_TICKS);
        if (cast == null) return false;
        double cost = a.frenzied() ? 0.0 : max * e.costPercent() / 100.0;
        if (cost > 0) SimulatedPlayerCaster.setPool(a, AvatarManaPool.pay(pool, cost));
        SimulatedPlayerCaster.startCooldown(a, keyOf(e), e.cooldownTicks(), now);
        if (a.frenzied()) YandereFrenzyHandler.burnForCast(a);
        return true;
    }

    /** Teaches the Avatar the exact skill through ManasCore if it does not know it yet (idempotent). False when there is no such skill. */
    static boolean learn(AvatarEntity a, ResourceLocation id) {
        try {
            var skills = SkillAPI.getSkillsFrom(a);
            if (skills.getSkill(id).isPresent()) return true;
            ManasSkill skill = SkillAPI.getSkillRegistry().get(id);
            if (skill == null) return false;
            skills.learnSkill(skill.createDefaultInstance());
            skills.markDirty();
            return true;
        } catch (RuntimeException | LinkageError soft) {
            LOG.warn("[nusmp] Avatar could not learn {}: {}", id, soft.toString());
            return false;
        }
    }

    private static Set<AvatarPersonality> personalities(AvatarEntity a) {
        Set<AvatarPersonality> s = EnumSet.noneOf(AvatarPersonality.class);
        if (a.primary() != null) s.add(a.primary());
        if (a.secondary() != null) s.add(a.secondary());
        return s;
    }

    /** "absolute_severance" -> "Absolute Severance". */
    public static String pretty(String path) {
        StringBuilder sb = new StringBuilder();
        for (String w : path.toLowerCase(Locale.ROOT).split("_")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }
}
