package com.newuniverse.nusmp.fairytail;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.NUGameRules;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.skill.SkillMsg;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the Fairy Tail skills are a self-contained feature (it does not use the Skill Tree), so it has its OWN registers: the eight skills in
 * a ManasCore skill register (it must hear about them before the registry freezes, so {@link #init} runs in the mod constructor) and the one
 * skill-book item. The mod constructor has exactly one line for all of it. The Unique to Ultimate evolution (Fire Dragon Slayer to Fire Dragon
 * King) is run here, not in the shared evolution table: every five seconds a player whose mastered Fire Dragon Slayer has the EP the pack asks
 * for gets the Fire Dragon King, with the same Voice of the World line the other evolutions have, and the data of the old skill (the eaten
 * charge, the form) carries over. Safe to call twice.
 */
public final class FairyTailSystem {
    private FairyTailSystem() {}

    public static final DeferredRegister<ManasSkill> SKILLS = DeferredRegister.create(SkillAPI.getSkillRegistryKey(), NUSMP.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NUSMP.MODID);

    private static final Map<FtLine, DeferredHolder<ManasSkill, FtSkill>> HOLDERS = new EnumMap<>(FtLine.class);

    static {
        for (FtLine line : FtLine.values()) HOLDERS.put(line, SKILLS.register(line.id, () -> new FtSkill(line)));
    }

    public static final DeferredItem<FtBookItem> BOOK = ITEMS.register("fairy_tail_skill_book", FtBookItem::new);

    private static boolean done;

    /** The single entry point, called from the mod constructor on both sides. */
    public static synchronized void init(IEventBus modBus) {
        if (done) return;
        done = true;
        SKILLS.register(modBus);
        ITEMS.register(modBus);
        FtWorld.init();
        FairyTailSelfTests.register();
        NeoForge.EVENT_BUS.addListener(FairyTailSystem::onPlayerTick);
    }

    /** The registered skill of a line (call after registration). */
    public static ManasSkill skill(FtLine line) {
        var h = HOLDERS.get(line);
        return h == null ? null : h.get();
    }

    /** One skill book of every learnable line, for the creative tab. */
    public static List<ItemStack> creativeItems() {
        List<ItemStack> out = new ArrayList<>();
        for (FtLine line : FtLine.values()) out.add(FtBookItem.of(line));
        return out;
    }

    public static Item bookItem() { return BOOK.get(); }

    // ------------------------------------------------------------------------------------------------------------------ evolution
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 100 != 37) return;
        try {
            if (!NUGameRules.skillEvolution(player.level())) return;
            Skills skills = SkillAPI.getSkillsFrom(player);
            for (FtLine line : FtLine.values()) {
                FtLine up = line.evolvesInto();
                if (up == null) continue;
                ManasSkill base = skill(line), ult = skill(up);
                var owned = skills.getSkill(base.getRegistryName());
                if (owned.isEmpty() || skills.getSkill(ult.getRegistryName()).isPresent()) continue;
                IExistence ex = TensuraStorages.getExistenceFrom(player);
                if (!FtRules.canEvolve(owned.get().isMastered(player), ex == null ? 0 : ex.getEP(), NUConfig.EVOLUTION_MIN_EP.get())) continue;
                evolve(player, skills, owned.get(), ult);
                return;
            }
        } catch (RuntimeException | LinkageError ignored) {
            // an evolution check must never crash the tick
        }
    }

    private static void evolve(ServerPlayer player, Skills skills, ManasSkillInstance old, ManasSkill ult) {
        ManasSkillInstance evolved = ult.createDefaultInstance();
        evolved.getOrCreateTag().merge(old.getOrCreateTag().copy());
        if (!skills.learnSkill(evolved, SkillMsg.t("evolution.learned", "Your skill has evolved!").withStyle(ChatFormatting.GOLD))) return;
        if (NUConfig.EVOLUTION_REPLACES_BASE.get()) skills.forgetSkill(old.getSkillId());
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 0.8F);
        var server = player.getServer();
        if (server == null) return;
        server.getPlayerList().broadcastSystemMessage(
                SkillMsg.t("voice", "[Voice of the World] ").withStyle(ChatFormatting.GOLD)
                        .append(SkillMsg.t("evolution.confirmed", "Confirmed. %s's Unique Skill %s has evolved into the Ultimate Skill %s.",
                                player.getName().copy().withStyle(ChatFormatting.WHITE),
                                old.getDisplayName().copy().withStyle(ChatFormatting.AQUA),
                                ult.getName().copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)).withStyle(ChatFormatting.WHITE)), false);
    }
}
