package com.newuniverse.nusmp.dragonballs;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.multiverse.KnightRank;
import com.newuniverse.nusmp.multiverse.MultiverseProfile;
import com.newuniverse.nusmp.multiverse.Race;
import com.newuniverse.nusmp.relics.RelicCatalog;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.player.ITensuraPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The overpowered wishes (docs/dragon_balls_overpowered.md), server side, one wish at a time: pick any race, rank, skill, item or relic by name. Like the old wishes every one validates first and answers
 * {@link DbWishes.Outcome#ok} false WITHOUT using the wish up when it cannot be granted (unknown or ambiguous name, already that race, a skill already known, the ceiling reached), so a summoner never loses a wish to
 * a typo. The race change goes through the same call Tensura's own {@code /tensura race set} uses ({@code Races.setRace} plus the tracked-evolution reset), the skill through the grant helper the old skill wish
 * uses ({@code Skills.learnSkill}, which Tensura's and this mod's guards can veto). Only used while {@code dragonBalls.overpowered} is on. Cost: one pass over a registry list per wish, nothing per tick.
 */
final class DbWishesPlus {
    private DbWishesPlus() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    private static DbWishes.Outcome no(BallSet set, String key, Object... a) { return new DbWishes.Outcome(false, DbMsg.say(set, key, a)); }

    private static DbWishes.Outcome yes(BallSet set, String key, Object... a) { return new DbWishes.Outcome(true, DbMsg.say(set, key, a)); }

    private static String names(List<WishNames.Entry> l) {
        List<String> out = new ArrayList<>();
        for (WishNames.Entry e : l) out.add(e.name());
        return String.join(", ", out);
    }

    /** Answers a name that did not resolve to exactly one entry: not understood or ambiguous, never used up. */
    private static DbWishes.Outcome unresolved(BallSet set, WishNames.Result r, String unknownKey) {
        return r.status() == WishNames.Status.AMBIGUOUS ? no(set, "name_ambiguous", names(r.entries())) : no(set, unknownKey);
    }

    // ================================================================ race
    static DbWishes.Outcome race(ServerPlayer p, BallSet set, String text) {
        if (WishNames.noName(text)) {
            DbSession.listMenu(p, WishCatalog.races(), "race", ChatFormatting.GREEN, 90);
            return no(set, "race_pick");
        }
        WishNames.Result r = WishNames.resolve(WishCatalog.races(), text);
        if (r.status() != WishNames.Status.OK) return unresolved(set, r, "race_unknown");
        WishNames.Entry e = r.first();
        try {
            if (WishCatalog.DEVIL_ID.equals(e.id())) {                                                   // the mod's own race: a flag on the Magic Knight profile, the Tensura race stays
                if (MultiverseProfile.race(p) == Race.DEVIL) return no(set, "race_same", e.name());
                MultiverseProfile.setDevil(p, true);
                return yes(set, "race", e.name());
            }
            ResourceLocation id = ResourceLocation.tryParse(e.id());
            ManasRace race = id == null ? null : RaceAPI.getRaceRegistry().get(id);
            if (race == null) return no(set, "race_unknown");
            boolean devilFlag = MultiverseProfile.race(p) == Race.DEVIL;
            var current = RaceAPI.getRaceFrom(p).getRace();
            if (current.isPresent() && current.get().getRaceId().equals(id) && !devilFlag) return no(set, "race_same", e.name());
            if (!RaceAPI.getRaceFrom(p).setRace(race, false)) return no(set, "race_failed");           // false = do not teleport to a new spawn (as Tensura's own race command)
            if (devilFlag) MultiverseProfile.setDevil(p, false);                                       // the chosen race shows, the Devil flag would hide it
            ITensuraPlayer tp = TensuraStorages.getPlayerDataFrom(p);
            if (tp != null) {
                tp.setTrackedEvolution(null);
                tp.markDirty();
            }
            return yes(set, "race", e.name());
        } catch (RuntimeException | LinkageError t) {
            LOG.warn("[anime_multiverse] dragon balls: the race wish could not be applied", t);
            return no(set, "race_failed");
        }
    }

    // ================================================================ rank
    static DbWishes.Outcome rank(ServerPlayer p, BallSet set, String text) {
        int cur = MultiverseProfile.step(p);
        int named = WishCatalog.rankStep(text);
        int target = WishRules.rankTarget(set, cur, named);
        if (target < 0) return no(set, "rank_max", KnightRank.describe(MultiverseProfile.rank(p), MultiverseProfile.rankClass(p)));
        KnightRank rank = KnightRank.rankOfStep(target);
        MultiverseProfile.setRank(p, rank, KnightRank.classOfStep(target));
        return yes(set, "rank", KnightRank.describe(rank, KnightRank.classOfStep(target)));
    }

    // ================================================================ skills
    static DbWishes.Outcome skill(ServerPlayer p, BallSet set, String text, DbSession s) {
        if (WishNames.noName(text)) {
            if (set == BallSet.EARTH) return DbWishes.skillClassic(p, set, text, s);                      // Shenron without a name: a random skill of the old list
            DbSession.listMenu(p, sample(WishCatalog.skillsFor(set), 24), "skill", ChatFormatting.AQUA, 24);
            return no(set, "skill_pick_op", DbConfig.skillsPerWish(set));
        }
        int per = DbConfig.skillsPerWish(set);
        WishNames.Result r = WishNames.resolveAll(WishCatalog.skillsFor(set), text, per);
        if (r.status() != WishNames.Status.OK) {
            WishNames.Result any = WishNames.resolveAll(WishCatalog.allSkills(), text, 1);
            if (any.status() == WishNames.Status.OK) return no(set, "skill_tier", any.first().name(), DbConfig.skillCeiling(set).name().toLowerCase(java.util.Locale.ROOT));
            return unresolved(set, r, "skill_unknown");
        }
        Skills skills = SkillAPI.getSkillsFrom(p);
        List<String> learned = new ArrayList<>();
        int known = 0;
        for (WishNames.Entry e : r.entries()) {
            ResourceLocation id = ResourceLocation.tryParse(e.id());
            ManasSkill sk = id == null ? null : SkillAPI.getSkillRegistry().get(id);
            if (sk == null || !WishCatalog.teachableKind(id, sk)) continue;
            if (skills.getSkill(sk.getRegistryName()).isPresent()) { known++; continue; }                // the existing "already known" check
            MutableComponent msg = Component.translatable(DbMsg.P + "skill_learned", e.name()).withStyle(ChatFormatting.GOLD);
            if (skills.learnSkill(sk.createDefaultInstance(), msg)) learned.add(e.name());               // the existing grant helper; a guard that vetoes it answers false
        }
        if (learned.isEmpty()) return no(set, known > 0 ? "skill_known" : "skill_none");
        return yes(set, "skill_op", learned.size(), String.join(", ", learned));
    }

    private static List<WishNames.Entry> sample(List<WishNames.Entry> l, int n) {
        List<WishNames.Entry> c = new ArrayList<>(l);
        java.util.Collections.shuffle(c);
        return c.size() <= n ? c : c.subList(0, n);
    }

    // ================================================================ items
    static DbWishes.Outcome item(ServerPlayer p, BallSet set, String text) {
        if (WishNames.noName(text)) return DbWishes.itemClassic(p, set);                                  // no name: the old random gift
        WishNames.Result r = WishNames.resolve(WishCatalog.items(), text);
        if (r.status() != WishNames.Status.OK) return unresolved(set, r, "item_unknown");
        ResourceLocation id = ResourceLocation.tryParse(r.first().id());
        Item it = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (it == null || WishCatalog.itemBlocked(id, it)) return no(set, "item_unknown");
        ItemStack st = new ItemStack(it, WishRules.itemCount(WishRules.numberIn(text), it.getDefaultMaxStackSize()));
        Component name = st.getHoverName();
        DbWishes.give(p, st);
        return yes(set, "item", st.getCount(), name);
    }

    // ================================================================ weapon (any relic up to God grade, once a week)
    static DbWishes.Outcome weapon(ServerPlayer p, BallSet set, String text) {
        CompoundTag st = DbWishes.state(p);
        long now = System.currentTimeMillis(), last = st.getLong("weaponAt");
        if (!WishRules.weaponReady(last, now)) return no(set, "weapon_week", DbRules.format(WishRules.weaponWaitMs(last, now)));
        ItemStack stack;
        if (WishNames.noName(text)) {
            var sup = RelicCatalog.itemOf(WishRules.WEAPON_RELIC);
            Item it = null;
            try { it = sup == null ? null : sup.get(); } catch (RuntimeException ignored) { /* not registered */ }
            stack = it == null ? ItemStack.EMPTY : new ItemStack(it);
        } else {
            WishNames.Result r = WishNames.resolve(WishCatalog.relics(DbConfig.genesisRelics()), text);
            if (r.status() != WishNames.Status.OK) return unresolved(set, r, "weapon_unknown");
            RelicDef d = Relics.byId(r.first().id());
            stack = d == null || !WishRules.relicGradeAllowed(d.grade().ordinal(), DbConfig.genesisRelics()) ? ItemStack.EMPTY : RelicCatalog.newStack(d);
        }
        if (stack.isEmpty()) return no(set, "weapon_none");
        st.putLong("weaponAt", now);
        p.getPersistentData().put(DbWishes.KEY, st);
        Component name = stack.getHoverName();
        DbWishes.give(p, stack);
        return yes(set, "weapon", name);
    }
}
