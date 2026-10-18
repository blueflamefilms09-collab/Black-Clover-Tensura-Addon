package com.newuniverse.nusmp.elite.forge.session;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.elite.forge.ForgeComponents;
import com.newuniverse.nusmp.elite.forge.ForgeConfig;
import com.newuniverse.nusmp.elite.forge.block.ForgeStationBlock;
import com.newuniverse.nusmp.elite.forge.block.ForgeStationBlockEntity;
import com.newuniverse.nusmp.elite.forge.craft.EngravingHandler;
import com.newuniverse.nusmp.elite.forge.craft.Inventories;
import com.newuniverse.nusmp.elite.forge.craft.QualityApplier;
import com.newuniverse.nusmp.elite.forge.craft.SkillProbe;
import com.newuniverse.nusmp.elite.forge.data.ForgeRecipes;
import com.newuniverse.nusmp.elite.forge.rules.EngravingRules;
import com.newuniverse.nusmp.elite.forge.rules.ForgeProgress;
import com.newuniverse.nusmp.elite.forge.rules.ForgeRecipeDef;
import com.newuniverse.nusmp.elite.forge.rules.Quality;
import com.newuniverse.nusmp.elite.forge.rules.QualityRoller;
import com.newuniverse.nusmp.elite.forge.rules.SessionCheck;
import com.newuniverse.nusmp.elite.forge.rules.Specialization;
import com.newuniverse.nusmp.elite.forge.rules.StationTier;
import com.newuniverse.nusmp.elite.forge.store.ForgeStore;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the one place where a forged item comes into being, so that the real game path (a validated session), the operator's tools and the self-tests all craft by the
 * very same code. {@link #craft} takes a finished score and does everything that follows: re-checks and takes the materials ALL-OR-NOTHING (nothing is taken if
 * anything is missing, and nothing is made before the materials are gone, so there is no way to keep both), adds the premium materials' points, asks the player's
 * skills and specialization, rolls the quality ({@link QualityRoller}), builds the stack with its quality and stat bonuses ({@link QualityApplier}) and engravings
 * ({@link EngravingHandler}), hands it over, records the progress, the ledger and the station, and plays the sound. {@link #finish} is the session path: it first
 * judges the client's report with {@link SessionCheck} and refuses a result that is not plausible, taking nothing. A failed forge is never destroying: a poor score gives a
 * Common item, a refused report gives nothing and costs nothing.
 */
public final class ForgeFinisher {
    private ForgeFinisher() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** What happened. {@code message} is a translation key. */
    public record Outcome(boolean ok, String message, ItemStack stack, QualityRoller.Result roll, int score, List<EngravingHandler.Applied> engravings, String reason) {
        public static Outcome fail(String message) { return new Outcome(false, message, ItemStack.EMPTY, null, 0, List.of(), ""); }

        public static Outcome fail(String message, String reason) { return new Outcome(false, message, ItemStack.EMPTY, null, 0, List.of(), reason); }
    }

    /** The session path: judge the report, then craft. Takes nothing when the report is refused. */
    public static Outcome finish(ServerPlayer player, ForgeSessions.Session s, List<int[]> inputs) {
        if (!ForgeConfig.enabled()) return Outcome.fail("nusmp.forge.msg.disabled");
        ServerLevel level = player.getServer().getLevel(s.dimension());
        if (level == null || ForgeStationBlock.tierAt(level, s.pos()) == null) return Outcome.fail("nusmp.forge.msg.station_gone");
        if (!player.level().dimension().equals(s.dimension()) || player.distanceToSqr(s.pos().getCenter()) > square(ForgeConfig.stationRange() + 4)) {
            return Outcome.fail("nusmp.forge.msg.too_far");
        }
        ForgeRecipes.Loaded loaded = ForgeRecipes.get(s.recipeId());
        if (loaded == null || !loaded.available()) return Outcome.fail("nusmp.forge.msg.unknown_recipe");
        SessionCheck.Verdict v = SessionCheck.evaluate(s.steps(), s.seeds(), inputs, ForgeSessions.elapsed(s));
        if (!v.ok()) {
            LOG.info("[nusmp] forge: a result from {} was refused: {}", player.getGameProfile().getName(), v.reason());
            return Outcome.fail("nusmp.forge.msg.rejected", v.reason());
        }
        StationTier tier = ForgeStationBlock.tierAt(level, s.pos());
        return craft(player, level, s.pos(), tier, loaded, s.premium(), v.total());
    }

    /** Crafts the recipe at the given station tier with a finished minigame score (permille). */
    public static Outcome craft(ServerPlayer player, ServerLevel level, BlockPos pos, StationTier tier, ForgeRecipes.Loaded loaded, boolean premium, int scorePermille) {
        ForgeRecipeDef def = loaded.def();
        if (!loaded.available()) return Outcome.fail("nusmp.forge.msg.unknown_recipe");
        if (ForgeConfig.enforceTier() && !tier.canForge(def.tier())) return Outcome.fail("nusmp.forge.msg.tier");
        List<Inventories.Row> pay = new ArrayList<>();
        for (ForgeRecipeDef.Stack st : def.inputs()) {
            Item it = Inventories.item(st.item());
            if (it == null) return Outcome.fail("nusmp.forge.msg.unknown_recipe");
            pay.add(new Inventories.Row(it, st.count()));
        }
        if (!Inventories.hasAll(player, pay)) return Outcome.fail("nusmp.forge.msg.missing");
        int materialPoints = 0;
        if (premium) {
            for (ForgeRecipeDef.Premium p : def.premium()) {
                Item it = Inventories.item(p.item());
                if (it == null) continue;
                // the premium material counts only if the player still has it on top of what the required inputs take (a recipe never lists one item in both)
                if (Inventories.count(player, it) >= p.count()) {
                    pay.add(new Inventories.Row(it, p.count()));
                    materialPoints += p.bonus();
                }
            }
        }
        if (!Inventories.takeAll(player, pay)) return Outcome.fail("nusmp.forge.msg.missing");

        ForgeProgress progress = ForgeStore.of(player);
        Specialization spec = progress.specialization();
        int specPoints = spec == null ? 0 : spec.categoryPoints(def.category());
        int skillPoints = SkillProbe.bonus(player);
        double luck = ForgeConfig.luck() ? QualityRoller.luckFrom(player.getRandom().nextDouble()) : 0.0;
        QualityRoller.Result roll = QualityRoller.roll(new QualityRoller.Inputs(scorePermille / 1000.0, tier, def.baseQuality(), materialPoints, skillPoints, specPoints, luck));
        Quality q = roll.quality();

        Item out = loaded.outputItem();
        ItemStack stack = new ItemStack(out, def.output().count());
        QualityApplier.apply(stack, q, player.getGameProfile().getName(), ForgeConfig.statScale());
        stack.set(ForgeComponents.KIND.get(), def.category().id());
        List<EngravingHandler.Applied> engraved = List.of();
        if (def.engrave() && stack.getMaxStackSize() == 1) {
            int slots = EngravingRules.totalSlots(q, spec, def.category());
            engraved = EngravingHandler.engrave(player.registryAccess(), stack, q, spec, def.category(), EngravingRules.initialCount(slots), player.getRandom().nextLong());
        }
        ItemStack shown = stack.copy();
        if (!player.getInventory().add(stack)) player.drop(stack, false);

        progress.recordForge(tier, def.id(), q);
        ForgeStore.mirror(player);
        if (level.getBlockEntity(pos) instanceof ForgeStationBlockEntity be) be.recordForge(q.level());
        player.giveExperiencePoints(2 + q.level() * 3);
        celebrate(level, pos, q);
        if (q == Quality.MYTHICAL && ForgeConfig.announceMythical() && player.getServer() != null) {
            player.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("nusmp.forge.msg.mythical", player.getDisplayName(), shown.getHoverName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
        }
        return new Outcome(true, "nusmp.forge.msg.done", shown, roll, scorePermille, engraved, "");
    }

    private static double square(double v) { return v * v; }

    private static void celebrate(ServerLevel level, BlockPos pos, Quality q) {
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 0.9F + 0.05F * q.level());
        if (q.level() >= Quality.EPIC.level()) level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.7F, 0.6F + 0.15F * q.level());
        int n = 6 + 6 * q.level();
        level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, n / 2, 0.25, 0.1, 0.25, 0.02);
        level.sendParticles(q.level() >= Quality.LEGENDARY.level() ? ParticleTypes.END_ROD : ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, n, 0.3, 0.3, 0.3, 0.05);
    }
}
