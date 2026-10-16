package com.newuniverse.nusmp.avatar.naming;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.avatar.AvatarConfig;
import com.newuniverse.nusmp.avatar.AvatarCreation;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.AvatarManager;
import com.newuniverse.nusmp.avatar.AvatarSystem;
import com.newuniverse.nusmp.avatar.ChatEvent;
import com.newuniverse.nusmp.avatar.AvatarChatterEngine;
import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.avatar.appearance.AppearanceData;
import com.newuniverse.nusmp.avatar.appearance.DynamicAppearanceEngine;
import com.newuniverse.nusmp.avatar.dnd.Stat;
import com.newuniverse.nusmp.avatar.outcome.Outcome;
import com.newuniverse.nusmp.avatar.race.AvatarRace;
import com.newuniverse.nusmp.avatar.race.AvatarRaces;
import com.newuniverse.nusmp.quests.NusmpEvents;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the owner's naming rules (section 6) are the heart of the Avatar's loyalty. A formal name costs the creator a Magicule Exhaustion Tax (the
 * {@code nusmp:naming_sleep} debuff, then a permanent share of their maximum magicule for as long as the Avatar lives), gives the Avatar an instant EP surge
 * of +50% to +200% scaled by luck and INT, evolves it into the race's {@code evolves_to_race_id} when the race has one, and locks the bond: a named Avatar
 * can never betray. This class applies all of that on the server, through {@link AvatarCreation#nameAvatar} (our GUI) and through Tensura's own naming
 * event (so naming the Avatar with Tensura's naming works the same way). Everything Tensura-side goes through {@link TensuraBridge}.
 */
public final class TensuraNamingEngine {
    private TensuraNamingEngine() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final ResourceLocation TAX_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "avatar_tax");

    /** Names the Avatar. The creator must be its creator, the name valid and unique among their Avatars, and the Avatar not named yet. */
    public static AvatarCreation.Result name(ServerPlayer creator, AvatarEntity a, String rawName) {
        if (!AvatarConfig.enabled()) return new AvatarCreation.Result(false, "The Avatar system is disabled on this server.");
        if (a == null || !a.isAlive() || a.creatorUuid() == null || !a.creatorUuid().equals(creator.getUUID())) return new AvatarCreation.Result(false, "That is not your Avatar.");
        if (a.isNamed()) return new AvatarCreation.Result(false, a.avatarName() + " already has a name.");
        String name = NameRules.clean(rawName);
        List<String> others = new ArrayList<>(AvatarManager.get(creator.getServer()).names(creator.getUUID()));
        others.remove(a.avatarName());
        NameRules.Verdict v = NameRules.check(name, others);
        if (v != NameRules.Verdict.OK) return new AvatarCreation.Result(false, NameRules.message(v));
        apply(creator, a, name, true);
        return new AvatarCreation.Result(true, "You named your Avatar " + name + ".");
    }

    /**
     * Applies the consequences of a valid name. {@code ourSurge} is false when the name was given through Tensura's own naming (it already applies its own
     * EP gain, so only the bond, the evolution, the tax and the sleep are added here).
     */
    public static void apply(ServerPlayer creator, AvatarEntity a, String name, boolean ourSurge) {
        a.setNamed(true);
        a.setAvatarName(name);
        TensuraBridge.setNaming(a, name, creator.getUUID(), false);

        // Soul-Bond Lock: naming cancels the roll and cures a betrayal; an awakened Avatar stays independent but named.
        Outcome before = a.outcomeState();
        if (before == Outcome.PENDING || before == Outcome.BETRAYAL) {
            a.setOutcomeState(Outcome.SKIPPED_NAMED);
            a.setOutcomeTimer(-1);
            a.setTarget(null);
            a.rebuildGoals();
            if (before == Outcome.BETRAYAL) creator.sendSystemMessage(Component.literal(name + "'s grudge dissolves: the name binds the soul.").withStyle(ChatFormatting.AQUA));
        }

        // EP surge (and evolution)
        int intScore = a.stats().length >= 6 ? a.stats()[Stat.INT.ordinal()] : 10;
        double boost = NamingMath.epBoost(a.storedLuck(), intScore, a.getRandom().nextDouble(), AvatarConfig.namingMinEpBoost(), AvatarConfig.namingMaxEpBoost());
        double mult = ourSurge ? NamingMath.epMultiplier(boost) : 1.0;
        AvatarRace now = AvatarRaces.get(a.raceId());
        AvatarRace next = now != null && now.evolves() ? AvatarRaces.get(now.evolvesTo()) : null;
        boolean evolved = false;
        if (next != null) {
            AppearanceData look = a.appearance();
            float scale = now.defaultScale() > 0 ? look.scale() * next.defaultScale() / now.defaultScale() : next.defaultScale();
            AppearanceData grown = new AppearanceData(look.skinTone(), look.hairId(), look.outfitId(), look.hornId(), look.wingId(), scale, look.auraRgb(), look.eyeRgb());
            a.getEntityData().set(AvatarEntity.RACE, next.id());
            a.setAppearance(DynamicAppearanceEngine.sanitize(grown, next));
            a.applyRace(next, mult);
            evolved = true;
        } else if (ourSurge) {
            TensuraBridge.scaleEnergy(a, mult);
        }

        // the creator pays
        a.setPermanentTax(NamingMath.taxFraction(AvatarConfig.permanentTaxPercent()));
        AvatarManager.get(creator.getServer()).update(a);
        refreshTax(creator);
        int sleep = NamingMath.sleepTicks(AvatarConfig.namingSleepSeconds(), evolved);
        if (sleep > 0) creator.addEffect(new MobEffectInstance(AvatarSystem.NAMING_SLEEP, sleep, 0, false, true, true));

        creator.sendSystemMessage(Component.literal(name + " is named" + (ourSurge ? ": +" + Math.round(boost * 100) + "% EP" : "") + (evolved ? ", and it has evolved into a " + next.displayName() : "")
                + ". Your magicules are exhausted.").withStyle(ChatFormatting.GOLD));
        AvatarChatterEngine.say(a, ChatEvent.NAMING, name);
        NusmpEvents.fire(creator, "avatar.named");
        LOG.debug("[nusmp] Avatar {} named {} (boost {}, evolved {})", a.getUUID(), name, boost, evolved);
    }

    /**
     * Puts the permanent tax of the creator's living Avatars on their maximum magicule ({@code nusmp:avatar_tax}, a transient multiplier), or takes it
     * off when there is none. Cheap; called on login, on naming, on death and by the Avatar's own slow tick so a tax can never get stuck.
     */
    public static void refreshTax(ServerPlayer p) {
        try {
            AttributeInstance inst = p.getAttribute(TensuraAttributes.MAX_MAGICULE);
            if (inst == null) return;
            double tax = AvatarManager.get(p.getServer()).totalTax(p.getUUID());
            AttributeModifier old = inst.getModifier(TAX_ID);
            if (tax <= 0.0) {
                if (old != null) inst.removeModifier(TAX_ID);
                return;
            }
            if (old != null && Math.abs(old.amount() + tax) < 1.0e-9) return;
            inst.removeModifier(TAX_ID);
            inst.addTransientModifier(new AttributeModifier(TAX_ID, -tax, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } catch (Throwable t) {
            LOG.debug("[nusmp] could not refresh the avatar tax: {}", t.toString());
        }
    }
}
