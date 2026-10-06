package com.newuniverse.nusmp.blackclover;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Grimoire pages: which pages each magic can write, the starter page, and the
 * "every 100 kills, roll for a new page" system.
 */
public final class GrimoirePages {
    private GrimoirePages() {}

    /** Page pool per magic. The FIRST page is the starter page. Add new attributes here. */
    /** Legacy separate-page pools. Ported families use book modes instead, so this is empty. */
    public static List<Supplier<? extends ManasSkill>> pool(MagicType magic) {
        return new ArrayList<>();
    }

    // ---------------- player grimoire lookups ----------------

    /** The player's grimoire magic skill instance, if they have one. */
    public static Optional<ManasSkillInstance> grimoireOf(Player p) {
        Skills skills = SkillAPI.getSkillsFrom(p);
        // Several books? The one whose grimoire you are holding wins.
        for (var s : new net.minecraft.world.item.ItemStack[]{p.getMainHandItem(), p.getOffhandItem(), GrimoireSlot.get(p)}) {
            if (!GrimoireItem.isOwnedBy(s, p.getUUID())) continue;
            var held = skills.getSkill(NUSkills.grimoireSkillFor(MagicType.byName(GrimoireItem.data(s).getString("Magic"))).getRegistryName());
            if (held.isPresent()) return held;
        }
        for (var id : NUSkills.allGrimoireSkillIds()) {
            Optional<ManasSkillInstance> i = skills.getSkill(id);
            if (i.isPresent()) return i;
        }
        return Optional.empty();
    }

    public static MagicType magicOf(ManasSkillInstance i) {
        if (i.getSkill() instanceof com.newuniverse.nusmp.book.GrimoireBook b) return b.magic;
        if (i.getSkill() instanceof com.newuniverse.nusmp.skill.GrimoireMagicSkill g && g.fixedMagic() != null) return g.fixedMagic();
        return MagicType.byName(i.getOrCreateTag().getString("Magic"));
    }

    public static GrimoireCover coverOf(ManasSkillInstance i) {
        CompoundTag t = i.getOrCreateTag();
        return GrimoireCover.byName(t.getString("Cover"), t.getInt("Leaves"));
    }

    // ---------------- granting ----------------

    public static boolean has(ServerPlayer p, ManasSkill page) { return SkillAPI.getSkillsFrom(p).getSkill(page.getRegistryName()).isPresent(); }

    public static boolean grant(ServerPlayer p, ManasSkill page) {
        if (has(p, page)) return false;
        boolean ok = SkillAPI.getSkillsFrom(p).learnSkill(page.createDefaultInstance(),
                Component.literal("A new page writes itself into your grimoire.").withStyle(ChatFormatting.GOLD));
        if (ok) {
            p.displayClientMessage(Component.literal("New page: ").withStyle(ChatFormatting.GOLD)
                    .append(page.getName().copy().withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)), false);
            p.displayClientMessage(page.getSkillDescription().copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
        return ok;
    }

    public static void grantStarter(ServerPlayer p, MagicType magic) {
        var pool = pool(magic);
        if (!pool.isEmpty()) grant(p, pool.get(0).get());
    }

    /** Grimoires granted before pages existed get their starter page. */
    public static void migrateStarter(ServerPlayer p) {
        migrateToBooks(p);
        grimoireOf(p).ifPresent(i -> {
            if (i.getSkill() instanceof com.newuniverse.nusmp.book.GrimoireBook) return;
            var pool = pool(magicOf(i));
            if (!pool.isEmpty() && !has(p, pool.get(0).get())) grantStarter(p, magicOf(i));
        });
    }

    // ---------------- kills -> page rolls ----------------

    // 0.24: pages open by mastery only (multiverse.MasteryPages). The old kill counter, kill page rolls and forbidden kill rolls
    // were removed; a kill never writes a page.

    /** Legacy grimoire skills of ported families become their Unique book (cover/magic/pages carried over). */
    public static void migrateToBooks(ServerPlayer p) {
        Skills skills = SkillAPI.getSkillsFrom(p);
        for (MagicType m : MagicType.values()) {
            var book = NUSkills.bookFor(m);
            if (book == null) continue;
            var legacy = skills.getSkill(NUSkills.grimoireSkill(m).getRegistryName());
            if (legacy.isEmpty()) continue;
            ManasSkillInstance inst = book.createDefaultInstance();
            inst.getOrCreateTag().merge(legacy.get().getOrCreateTag().copy());
            if (m == MagicType.TIME) {
                var oldPages = java.util.List.of(NUSkills.PAGE_CHRONO_STASIS, NUSkills.PAGE_CHRONO_STASIS_GRIGORA, NUSkills.PAGE_TIME_ACCELERATION,
                        NUSkills.PAGE_TIME_REVERSAL, NUSkills.PAGE_STOLEN_TIME);
                for (int k = 0; k < oldPages.size(); k++) {
                    if (skills.getSkill(oldPages.get(k).getId()).isPresent()) {
                        com.newuniverse.nusmp.book.GrimoireBook.unlock(inst, k);
                        skills.forgetSkill(oldPages.get(k).getId());
                    }
                }
            }
            skills.forgetSkill(legacy.get().getSkillId());
            skills.learnSkill(inst, Component.literal("Your grimoire awakens as a Unique skill.").withStyle(ChatFormatting.GOLD));
        }
    }
}
