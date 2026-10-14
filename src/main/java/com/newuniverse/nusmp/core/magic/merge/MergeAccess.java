package com.newuniverse.nusmp.core.magic.merge;

import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.multiverse.MasteryPages;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Who has which merged magic. A merged magic is available to a player in two ways: an admin grants it
 * ({@code /multiverse magic grant <player> <merged>}), or the player holds the grimoire skill of EVERY parent, in which case it is
 * granted once automatically. Neither way touches the parents: their skills, pages, mastery and grimoires are only read. The merged
 * skill instance remembers the id it was granted for ("MergedId"), so a slot reused after a removal never gives an old holder the new
 * magic, and a skill whose magic was removed is taken back from online players.
 */
public final class MergeAccess {
    private MergeAccess() {}

    private static final String TAG_ID = "MergedId", AUTO = "nusmp_merged_auto", DENIED = "nusmp_merged_denied";

    // ---------------------------------------------------------------- lookups
    /** The player's merged skill instance for this definition, if they hold it (and it was granted for this very magic). */
    public static Optional<ManasSkillInstance> held(ServerPlayer p, MergedDef def) {
        if (!MergedSkills.has(def.slot())) return Optional.empty();
        Optional<ManasSkillInstance> i = SkillAPI.getSkillsFrom(p).getSkill(MergedSkills.id(def.slot()));
        return i.filter(x -> def.id().equals(x.getOrCreateTag().getString(TAG_ID)));
    }

    /** Does the player hold the grimoire skill of every parent? */
    public static boolean holdsAllParents(ServerPlayer p, MergedDef def) {
        Skills skills = SkillAPI.getSkillsFrom(p);
        for (String name : def.parents()) {
            MagicType t = MagicType.lookup(name);
            if (t == null) return false;
            var skill = NUSkills.grimoireSkillFor(t);
            if (skill == null || skills.getSkill(skill.getRegistryName()).isEmpty()) return false;
        }
        return true;
    }

    // ---------------------------------------------------------------- grant / revoke
    /** Grants the merged magic. False if the player already holds it. Never changes the parents. */
    public static boolean grant(ServerPlayer p, MergedDef def) {
        if (!MergedSkills.has(def.slot()) || held(p, def).isPresent()) return false;
        Skills skills = SkillAPI.getSkillsFrom(p);
        skills.getSkill(MergedSkills.id(def.slot())).ifPresent(stale -> skills.forgetSkill(stale.getSkillId()));      // a slot's old magic (reused slot)
        ManasSkillInstance inst = MergedSkills.skill(def.slot()).createDefaultInstance();
        CompoundTag tag = inst.getOrCreateTag();
        tag.putString(TAG_ID, def.id());
        GrimoireCover cover = GrimoirePages.grimoireOf(p).map(GrimoirePages::coverOf).orElse(GrimoireCover.THREE_LEAF);
        tag.putString("Cover", cover.name());
        tag.putInt("Leaves", cover.tier);
        tag.putString("Magic", def.dominant());
        inst.markDirty();
        boolean ok = skills.learnSkill(inst, Component.translatable("nusmp.merge.learned", def.name()).withStyle(ChatFormatting.GOLD));
        if (ok) {
            forget(p, DENIED, def.id());
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
        return ok;
    }

    /** Takes the merged magic from the player (an admin's decision: it is not granted again automatically). */
    public static boolean revoke(ServerPlayer p, MergedDef def) {
        Optional<ManasSkillInstance> i = held(p, def);
        if (i.isEmpty()) return false;
        SkillAPI.getSkillsFrom(p).forgetSkill(i.get().getSkillId());
        remember(p, DENIED, def.id());
        return true;
    }

    /** A magic was removed: every online holder loses its skill. */
    public static void purge(MinecraftServer server, MergedDef removed) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Skills skills = SkillAPI.getSkillsFrom(p);
            skills.getSkill(MergedSkills.id(removed.slot())).ifPresent(i -> {
                if (removed.id().equals(i.getOrCreateTag().getString(TAG_ID))) skills.forgetSkill(i.getSkillId());
            });
        }
    }

    // ---------------------------------------------------------------- the player's own list (persistent data)
    private static boolean listed(ServerPlayer p, String key, String id) {
        for (Tag t : p.getPersistentData().getList(key, Tag.TAG_STRING)) if (t.getAsString().equals(id)) return true;
        return false;
    }

    private static void remember(ServerPlayer p, String key, String id) {
        if (listed(p, key, id)) return;
        ListTag l = p.getPersistentData().getList(key, Tag.TAG_STRING);
        l.add(StringTag.valueOf(id));
        p.getPersistentData().put(key, l);
    }

    private static void forget(ServerPlayer p, String key, String id) {
        ListTag l = p.getPersistentData().getList(key, Tag.TAG_STRING);
        for (int k = l.size() - 1; k >= 0; k--) if (l.getString(k).equals(id)) l.remove(k);
        p.getPersistentData().put(key, l);
    }

    // ---------------------------------------------------------------- periodic upkeep
    /**
     * Every few seconds per player: auto-grant the merged magics whose parents' grimoires the player holds (once each, never again after
     * an admin revoked it), and take back skills whose magic no longer exists.
     */
    public static void tick(ServerPlayer p) {
        Skills skills = SkillAPI.getSkillsFrom(p);
        for (MergedDef def : MergedRegistry.all()) {
            if (listed(p, AUTO, def.id()) || listed(p, DENIED, def.id()) || held(p, def).isPresent()) continue;
            if (holdsAllParents(p, def) && grant(p, def)) remember(p, AUTO, def.id());
        }
        for (int slot = 0; slot < MergeLimits.MAX_MERGED && MergedSkills.has(slot); slot++) {
            final int s = slot;
            skills.getSkill(MergedSkills.id(s)).ifPresent(i -> {
                MergedDef d = MergedRegistry.bySlot(s);
                if (d == null || !d.id().equals(i.getOrCreateTag().getString(TAG_ID))) skills.forgetSkill(i.getSkillId());
            });
        }
    }

    /** Opens the merged book's pages as its mastery grows, like the parents' grimoires do (multiverse.MasteryPages): one page per call. */
    public static void unlockPages(ServerPlayer p) {
        Skills skills = SkillAPI.getSkillsFrom(p);
        for (MergedDef def : MergedRegistry.all()) {
            Optional<ManasSkillInstance> held = held(p, def);
            if (held.isEmpty() || !(held.get().getSkill() instanceof GrimoireBook book)) continue;
            ManasSkillInstance inst = held.get();
            int n = book.familyCount();
            int open = 0;
            for (int m = 0; m < n; m++) if (GrimoireBook.isUnlocked(inst, m)) open++;
            if (open >= MasteryPages.slots(p, inst, book)) continue;
            double mastery = book.masteryFrac(inst);
            for (int m = 1; m < n; m++) {
                BookPage page = book.page(m);
                if (page == null || GrimoireBook.isUnlocked(inst, m) || mastery < MasteryPages.masteryCost(page, m, n)) continue;
                if (!MasteryPages.gate(book, page).passes(p)) continue;
                GrimoireBook.unlock(inst, m);
                p.sendSystemMessage(Component.translatable("nusmp.merge.page_opened", def.name(), page.name()).withStyle(ChatFormatting.GOLD));
                p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.8f);
                break;
            }
        }
    }

    /** Names of the online players' merged magics are not tracked here; this lists the defs a player holds (for /info). */
    public static List<MergedDef> heldBy(ServerPlayer p) {
        List<MergedDef> out = new ArrayList<>();
        for (MergedDef d : MergedRegistry.all()) if (held(p, d).isPresent()) out.add(d);
        return out;
    }
}
