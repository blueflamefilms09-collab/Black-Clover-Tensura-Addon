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
        for (var s : new net.minecraft.world.item.ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
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

    private static final String KILLS = "nusmp_grimoire_kills";

    public static int kills(Player p) { return p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getInt(KILLS); }

    public static void setKills(Player p, int kills) {
        CompoundTag root = p.getPersistentData();
        CompoundTag t = root.getCompound(Player.PERSISTED_NBT_TAG);
        t.putInt(KILLS, kills);
        root.put(Player.PERSISTED_NBT_TAG, t);
    }

    /** Adds kills and rolls once for every threshold crossed. */
    public static void addKills(ServerPlayer p, int amount) {
        int per = Math.max(1, NUConfig.KILLS_PER_ROLL.get());
        int before = kills(p), after = before + amount;
        setKills(p, after);
        for (int k = before / per + 1; k <= after / per; k++) { roll(p); rollForbidden(p, k * per); }
    }

    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        LivingEntity dead = event.getEntity();
        if (dead == killer || grimoireOf(killer).isEmpty()) return;
        if (dead instanceof Player && !NUConfig.PLAYER_KILLS_COUNT.get()) return;
        boolean boss = dead.getType().is(Tags.EntityTypes.BOSSES) || dead.getMaxHealth() >= 300;
        addKills(killer, boss ? NUConfig.BOSS_KILL_WEIGHT.get() : 1);
    }

    private static final String FAILS = "nusmp_grimoire_fails";

    /** One page roll: 20% (+5% luck covers, -10% cracked), the 5th failure in a row always succeeds. */
    public static void roll(ServerPlayer p) {
        Optional<ManasSkillInstance> g = grimoireOf(p);
        if (g.isEmpty()) return;
        ManasSkillInstance inst = g.get();
        GrimoireCover cover = coverOf(inst);
        double chance = NUConfig.PAGE_CHANCE.get();
        if (cover.isRare()) chance += NUConfig.RARE_PAGE_BONUS.get();
        if (cover.isCracked()) chance -= NUConfig.CRACKED_PAGE_PENALTY.get();

        CompoundTag pd = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        int fails = pd.getInt(FAILS);
        boolean pity = fails + 1 >= NUConfig.PAGE_PITY.get();
        boolean success = pity || p.getRandom().nextDouble() * 100.0 < chance;

        if (!success) {
            pd.putInt(FAILS, fails + 1);
            p.getPersistentData().put(Player.PERSISTED_NBT_TAG, pd);
            inst.addMasteryPoint(p, 3);
            inst.markDirty();
            p.displayClientMessage(Component.literal("Your grimoire flutters... and goes quiet.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.6F, 1.4F);
            return;
        }
        pd.putInt(FAILS, 0);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, pd);

        if (inst.getSkill() instanceof com.newuniverse.nusmp.book.GrimoireBook book) {
            List<Integer> locked = new ArrayList<>();
            for (int m = 1; m < book.familyCount(); m++) if (!com.newuniverse.nusmp.book.GrimoireBook.isUnlocked(inst, m)) locked.add(m);
            if (!locked.isEmpty()) {
                int m = locked.get(p.getRandom().nextInt(locked.size()));
                com.newuniverse.nusmp.book.GrimoireBook.unlock(inst, m);
                var page = book.page(m);
                p.displayClientMessage(Component.literal("A new page writes itself in: ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(page.name()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)), false);
                p.displayClientMessage(Component.translatable("nusmp.page." + book.getRegistryName().getPath() + "." + page.id())
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
                p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);
                return;
            }
        }
        inst.addMasteryPoint(p, 25);
        inst.markDirty();
        p.displayClientMessage(Component.literal("No new page appears, but your grimoire's magic deepens.").withStyle(ChatFormatting.AQUA), false);
    }

    /** Five-leaf / triple-spade books: after enough kills, a small chance to write a forbidden page. */
    public static void rollForbidden(ServerPlayer p, int kills) {
        if (kills < NUConfig.FORBIDDEN_MIN_KILLS.get()) return;
        Optional<ManasSkillInstance> g = grimoireOf(p);
        if (g.isEmpty() || !coverOf(g.get()).isForbidden()) return;
        var fs = SkillAPI.getSkillsFrom(p).getSkill(NUSkills.BOOK_FORBIDDEN.getId());
        if (fs.isEmpty() || !(fs.get().getSkill() instanceof com.newuniverse.nusmp.book.GrimoireBook fb)) return;
        if (p.getRandom().nextDouble() * 100 >= NUConfig.FORBIDDEN_PAGE_CHANCE.get()) return;
        List<Integer> locked = new ArrayList<>();
        for (int m = 2; m < fb.familyCount(); m++) if (!com.newuniverse.nusmp.book.GrimoireBook.isUnlocked(fs.get(), m)) locked.add(m);
        if (locked.isEmpty()) return;
        int m = locked.get(p.getRandom().nextInt(locked.size()));
        com.newuniverse.nusmp.book.GrimoireBook.unlock(fs.get(), m);
        com.newuniverse.nusmp.book.ForbiddenMagic.subtitle(p, Component.literal("A forbidden page writes itself in blood: " + fb.page(m).name()));
    }

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
