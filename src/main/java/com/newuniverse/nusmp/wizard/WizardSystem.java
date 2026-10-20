package com.newuniverse.nusmp.wizard;


import com.newuniverse.nusmp.NUGameRules;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.blackclover.GrimoireAcceptance;
import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.NightmareSouls;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.NUEntities;
import com.newuniverse.nusmp.item.NUItems;
import com.newuniverse.nusmp.mana.Heritage;
import com.newuniverse.nusmp.multiverse.Ceremony;
import com.newuniverse.nusmp.multiverse.MultiverseConfig;
import com.newuniverse.nusmp.multiverse.MultiverseProfile;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * 1.6.0: how a mage gets their grimoire in the Black Clover way. When you come of age (enough play time) and/or have grown enough (enough
 * experience levels) you are given a <b>summons</b> to a Grimoire Tower. At the tower the <b>Tower Wizard</b> sets a randomly drawn
 * questionnaire; your answers decide your magic attribute(s), your kingdom and the blood of a Tensura race mixed into yours; then
 * the grimoire floats to you. Config: wizard.* in nusmp-multiverse-server.toml (wizard mode can be switched off for the older ceremony).
 */
public final class WizardSystem {
    private WizardSystem() {}

    private record Pending(Questionnaire.Sheet sheet, long at, Vec3 where) {}

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    // ---------------------------------------------------------------- the summons
    private static CompoundTag data(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, 10)) pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag root = pd.getCompound(Player.PERSISTED_NBT_TAG);
        if (!root.contains("nusmp_wizard", 10)) root.put("nusmp_wizard", new CompoundTag());
        return root.getCompound("nusmp_wizard");
    }

    public static boolean summoned(Player p) { return data(p).getBoolean("Summoned"); }

    private static boolean grownEnough(ServerPlayer p) {
        int minLevel = MultiverseConfig.get(MultiverseConfig.SUMMONS_MIN_LEVEL), minMinutes = MultiverseConfig.get(MultiverseConfig.SUMMONS_MIN_MINUTES);
        boolean levelOk = minLevel <= 0 || p.experienceLevel >= minLevel;
        long ticks = p.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        boolean ageOk = minMinutes <= 0 || ticks >= minMinutes * 1200L;
        if (minLevel <= 0 && minMinutes <= 0) return true;
        return MultiverseConfig.get(MultiverseConfig.SUMMONS_NEED_BOTH) ? levelOk && ageOk : (minLevel > 0 && levelOk) || (minMinutes > 0 && ageOk);
    }

    /** Hands out the summons (admin command or the age / level trigger). */
    public static void summon(ServerPlayer p) {
        data(p).putBoolean("Summoned", true);
        p.getInventory().placeItemBackInInventory(new ItemStack(NUItems.GRIMOIRE_SUMMONS.get()));
        p.sendSystemMessage(Component.literal("You have come of age: a Grimoire Summons is in your pack. NEXT: right-click it. It carries you to the Tower Wizard's library, where you answer his questions and your grimoire comes to you.").withStyle(ChatFormatting.GOLD));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 100, 20));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal("Right-click the Grimoire Summons in your pack").withStyle(ChatFormatting.YELLOW)));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal("You are summoned").withStyle(ChatFormatting.GOLD)));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.2f, 0.8f);
    }

    /** Every 5 s: come of age and you are summoned. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !MultiverseConfig.get(MultiverseConfig.WIZARD_MODE)) return;
        if (p.tickCount % 2400 == 41 && summoned(p) && !WizardLibrary.inLibrary(p) && GrimoirePages.grimoireOf(p).isEmpty())                  // a reminder of the next step
            p.displayClientMessage(Component.literal("Your Grimoire Summons is waiting: right-click it to visit the Tower Wizard.").withStyle(ChatFormatting.YELLOW), true);
        if (p.tickCount % 100 != 41) return;
        if (summoned(p) || !Ceremony.isEligible(p) || !grownEnough(p)) return;
        summon(p);
    }

    // ---------------------------------------------------------------- the wizard
    /** A wizard stands at the tower (spawned the first time an eligible mage comes near). */
    public static void ensureWizard(ServerLevel level, BlockPos at) {
        if (!level.getEntitiesOfClass(TowerWizardEntity.class, new AABB(at).inflate(24)).isEmpty()) return;
        TowerWizardEntity w = NUEntities.TOWER_WIZARD.get().create(level);
        if (w == null) return;
        BlockPos spot = at.above();
        for (BlockPos c : new BlockPos[]{at.east().above(), at.west().above(), at.south().above(), at.north().above(), at.above(2)}) {
            if (level.noCollision(w.getType().getDimensions().makeBoundingBox(c.getX() + 0.5, c.getY(), c.getZ() + 0.5))) { spot = c; break; }
        }
        w.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0f, 0f);
        level.addFreshEntity(w);
    }

    /** The player speaks to the wizard. */
    public static void talk(ServerPlayer p, TowerWizardEntity wizard) {
        if (WizardLibrary.inLibrary(p) && p.isShiftKeyDown()) { say(p, "Go, then. The door is always open."); WizardLibrary.leave(p); return; }
        if (GrimoirePages.grimoireOf(p).isPresent()) {
            say(p, "You already carry a grimoire. Go, and use it well.");
            if (WizardLibrary.inLibrary(p)) WizardLibrary.leave(p);
            return;
        }
        if (!MultiverseConfig.get(MultiverseConfig.WIZARD_MODE)) { say(p, "The tower chooses its mages in the old way now: pray at the altar."); return; }
        if (!Ceremony.isEligible(p)) { say(p, "The grimoires have not called you."); return; }
        if (!summoned(p) && !p.hasPermissions(2)) { say(p, "You come without a summons. Grow a little more, and one will find you."); return; }
        var sheet = Questionnaire.draw(p.getRandom());
        PENDING.put(p.getUUID(), new Pending(sheet, p.level().getGameTime(), wizard.position()));
        say(p, "Answer honestly, child. The grimoire reads the heart.");
        p.displayClientMessage(Component.literal("Click an answer for each question. (Sneak and right-click the wizard to leave.)").withStyle(ChatFormatting.GRAY), true);
        PacketDistributor.sendToPlayer(p, new WizardNet.OpenPayload(sheet.toTag()));
    }

    private static void say(ServerPlayer p, String msg) {
        p.sendSystemMessage(Component.literal("[Tower Wizard] ").withStyle(ChatFormatting.DARK_PURPLE).append(Component.literal(msg).withStyle(ChatFormatting.WHITE)));
    }

    /** The answers come back. */
    public static void answer(ServerPlayer p, int[] answers) {
        Pending pend = PENDING.remove(p.getUUID());
        if (pend == null || p.level().getGameTime() - pend.at() > 6000 || p.position().distanceToSqr(pend.where()) > 20 * 20) { say(p, "You took too long. Speak to me again."); return; }
        if (GrimoirePages.grimoireOf(p).isPresent()) return;
        var pool = MultiverseConfig.get(MultiverseConfig.ALL_MAGICS) ? MagicFlavors.pool() : GrimoireAcceptance.starterPool(NightmareSouls.soulTypeOf(p));   // 1.16.0: every magic
        var r = Questionnaire.result(pend.sheet(), answers, pool, NUGameRules.forbiddenStart(p.level()),
                MultiverseConfig.get(MultiverseConfig.DUAL_ATTRIBUTE_CHANCE), p.getRandom());
        if (r == null) { say(p, "That is not an answer I asked for."); return; }
        GrimoireCover cover = r.kingdom().coverFor(r.leaves());
        Devil devil = cover.isForbidden() ? GrimoireAcceptance.randomDevil(p.getRandom()) : null;
        Heritage.set(p, r.kingdom(), r.blood());
        com.newuniverse.nusmp.quests.NusmpEvents.fire(p, "wizard.questionnaire");
        ServerLevel sl = p.serverLevel();
        VfxSpawn.sendFollowing(sl, VfxShape.MAGIC_CIRCLE, p, p.position().add(0, 0.08, 0), 0xFFFFD86B, 90, 1.8f);
        say(p, "Yes... I see it. Your grimoire comes.");
        ItemStack visual = GrimoireItem.create(p, cover, r.primary(), devil);
        flight(sl, p, pend.where().add(0, 1.6, 0), visual, () -> arrive(p, r, cover, devil));
    }

    // ---------------------------------------------------------------- the book floats to you
    private static void flight(ServerLevel sl, ServerPlayer p, Vec3 from, ItemStack visual, Runnable arrive) {
        ItemEntity book = new ItemEntity(sl, from.x, from.y, from.z, visual);
        book.setNoGravity(true);
        book.setNeverPickUp();
        book.setDeltaMovement(Vec3.ZERO);
        sl.addFreshEntity(book);
        sl.playSound(null, BlockPos.containing(from), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.4f, 0.7f);
        step(sl, p, book, arrive, 0);
    }

    private static void step(ServerLevel sl, ServerPlayer p, ItemEntity book, Runnable arrive, int i) {
        Vec3 target = p.getEyePosition().add(0, -0.4, 0), to = target.subtract(book.position());
        if (!book.isAlive() || !p.isAlive() || i > 160 || to.length() < 1.3) {
            book.discard();
            if (p.isAlive()) arrive.run();
            return;
        }
        book.setDeltaMovement(to.normalize().scale(Math.min(0.32, 0.04 + i * 0.008)));
        book.hurtMarked = true;
        if (i % 4 == 0) Fx.spray(sl, FxThemes.WIZARD, FxTone.LIGHT, book.getX(), book.getY() + 0.3, book.getZ(), 6, 0.2, 0.2, 0.2, 0.4);
        SpellRuntime.later(sl, 2, () -> step(sl, p, book, arrive, i + 1));
    }

    private static void arrive(ServerPlayer p, Questionnaire.Result r, GrimoireCover cover, Devil devil) {
        GrimoireAcceptance.grantExact(p, cover, r.primary(), devil);
        if (GrimoirePages.grimoireOf(p).isEmpty()) return;                                    // another mod refused the skill
        // 1.16.0: two attributes fuse into the one grimoire (no second book): its fused look, its Signature Spell and, when the pair has no
        // blend in the game, a magic of its own born of them
        String bornName = r.born() ? BornMagic.nameOf(r.parentA(), r.parentB()) : null;
        String signature = GrimoireAcceptance.applyFusion(p, r.primary(), r.parentA(), r.parentB(), bornName);
        for (int i = 0; i < p.getInventory().getContainerSize(); i++)                  // the summons has done its work
            if (p.getInventory().getItem(i).is(NUItems.GRIMOIRE_SUMMONS.get())) p.getInventory().setItem(i, ItemStack.EMPTY);
        MultiverseProfile.setSocialClass(p, r.social());
        MultiverseProfile.setAccepted(p, true, "the Grimoire Tower");
        Ceremony.finish(p, "the Tower Wizard");
        var kt = Heritage.kingdomTrait(r.kingdom());
        var bt = Heritage.bloodTrait(r.blood());
        if (r.fused()) {
            p.sendSystemMessage(Component.literal("Two attributes answered you: " + r.parentA().displayName + " and " + r.parentB().displayName + ". They have fused into one grimoire.")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            p.sendSystemMessage(Component.literal(r.born() ? "A magic that did not exist before is born: " + bornName + " Magic."
                    : "Together they are " + r.primary().displayName + ".").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        }
        p.sendSystemMessage(Component.literal("Your magic: " + (r.born() ? bornName + " Magic" : r.primary().displayName) + "  |  " + cover.displayName()).withStyle(ChatFormatting.AQUA));
        p.sendSystemMessage(Component.literal("No two wizards' magic is the same. Yours is called " + signature + ": " + com.newuniverse.nusmp.mana.Signature.describe(p)
                + ". Its Signature Spell is the last page of your grimoire.").withStyle(ChatFormatting.AQUA));
        var sct = Heritage.socialTrait(r.social());
        p.sendSystemMessage(Component.literal("Your family stands among the " + r.social().displayName + ".  + " + sct.buff() + "  - " + sct.debuff()).withStyle(ChatFormatting.YELLOW));
        p.sendSystemMessage(Component.literal("You hail from the " + r.kingdom().displayName + ".  + " + kt.buff() + "  - " + kt.debuff()).withStyle(ChatFormatting.GOLD));
        p.sendSystemMessage(Component.literal("The blood of the " + r.blood().displayName + " is mixed into yours.  + " + bt.buff() + "  - " + bt.debuff()).withStyle(ChatFormatting.LIGHT_PURPLE));
        p.sendSystemMessage(Component.literal("NEXT: your grimoire is in your pack. Open the Tensura menu and click the Four Kingdoms slot to see everything about yourself in one book. Summon your grimoire with its first page, and train: your stage rises with your rank.").withStyle(ChatFormatting.GREEN));
        if (WizardLibrary.inLibrary(p)) {
            p.sendSystemMessage(Component.literal("The wizard will send you home in a few seconds.").withStyle(ChatFormatting.GRAY));
            SpellRuntime.later(p.serverLevel(), 160, () -> { if (p.isAlive()) WizardLibrary.leave(p); });
        }
    }
}
