package com.newuniverse.nusmp.heroweapon.weapon.carriage;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.client.heroweapon.carriage.CarriageClient;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.HeroWeaponSystem;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.kit.VbSkill;
import com.newuniverse.nusmp.heroweapon.weapon.kit.WeaponSelfTest;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.List;
import java.util.UUID;

import static com.newuniverse.nusmp.heroweapon.weapon.carriage.CarriageNumbers.*;

/**
 * Why: the behaviour of the Vassal Carriage (the vassal weapon of Fitoaria, the carriage the Filolial Queen draws). Using the weapon calls a carriage that
 * the wielder rides and drives by looking where they want to go; the nine techniques are orders for that vehicle (gallop, drift ram, soothing ride, cabin
 * barrier, Haikuikku, waypoint and portal, and the Crash! Charge! ultimate), and the passives of the forms decide what the carriage is (a third seat, faster
 * wheels, a stronger frame, the Queen's pull). Every number is {@link CarriageNumbers}'; the live skills are built from that table. The constructor does the
 * work that has to happen once and early: it attaches the carriage entity's registry and attributes to the mod bus (the shared start-up has stored the bus
 * before the logics are built), starts the client renderer on the client, registers the self-test {@code hero.weapon.carriage}, and installs the clean-up
 * listeners (the registry is emptied when the server stops, and a wielder who leaves the game takes their carriage with them).
 */
public final class CarriageLogic implements HeroLogic {
    private static boolean wired;

    private final List<HeroSkill> skills;

    public CarriageLogic() {
        this.skills = List.of(
                skill(CALL, CarriageSpells::call),
                skill(BOOST, CarriageSpells::boost),
                skill(DRIFT_RAM, CarriageSpells::driftRam),
                skill(COMFORT, CarriageSpells::comfort),
                skill(CABIN_BARRIER, CarriageSpells::cabinBarrier),
                skill(HAIKUIKKU, CarriageSpells::haikuikku),
                skill(WAYPOINT, CarriageSpells::waypoint),
                skill(PORTAL, CarriageSpells::portal),
                skill(CRASH_CHARGE, CarriageSpells::crashCharge));
        wire();
    }

    private static VbSkill skill(Budget.Spec spec, VbSkill.Body body) {
        return VbSkill.of(spec, CarriageText.name(spec.id()), CarriageText.desc(spec.id()), body);
    }

    private static synchronized void wire() {
        if (wired) return;
        wired = true;
        CarriageEntities.init(HeroWeaponSystem.modBus());
        if (FMLEnvironment.dist == Dist.CLIENT) CarriageClient.init(HeroWeaponSystem.modBus());
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            CarriageRegistry.clear();
            CarriageEntity.forgetFaultLog();
            VbSkill.forgetLogged();
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            try {
                if (e.getEntity() instanceof ServerPlayer p) sendAway(p.getServer(), p.getUUID());
            } catch (RuntimeException ex) {
                LogUtils.getLogger().warn("[nusmp] the carriage of a wielder who left could not be sent away cleanly", ex);
            }
        });
        SelfTests.register("hero.weapon.carriage", CarriageLogic::selfTest);
    }

    /** Sends the wielder's carriage away (everybody aboard is put on the ground first); nothing happens when there is none. */
    private static void sendAway(MinecraftServer server, UUID wielder) {
        CarriageEntity car = CarriageRegistry.find(server, wielder);
        if (car != null) car.dismiss();
    }

    private static SelfTests.Result selfTest(MinecraftServer server) {
        return WeaponSelfTest.run(server, HeroKind.CARRIAGE, ALL, CarriageLogic::check, fp -> sendAway(server, fp.getUUID()));
    }

    /**
     * The carriage's own checks, after the common ones have cast every technique once beside the dummy (the first of them, Call the Carriage, has put a
     * carriage in front of the stand-in): it exists, it names the stand-in, it has the seats the form promises, a stranger cannot board, and the stand-in
     * drives it. When the stand-in could be seated, using Call again sends the carriage away and a third use calls a fresh one; the clean-up sends that away.
     */
    private static String check(WeaponSelfTest.Fixture f) {
        HeroCtx c = f.ctx();
        UUID who = f.player().getUUID();
        CarriageEntity car = CarriageRegistry.find(f.server(), who);
        if (car == null) return "Call the Carriage left no carriage in the registry";
        if (!who.equals(car.ownerUuid())) return "the carriage names another owner";
        int promised = CarriageSpells.chassis(c).seats();
        if (car.seats() != promised) return "the carriage has " + car.seats() + " seats but the form promises " + promised;
        if (car.mayBoard(f.dummy())) return "a stranger (the dummy) was allowed to board";
        if (car.canAddPassenger(f.dummy())) return "the carriage would take a stranger as a passenger";
        boolean riding = car.getControllingPassenger() == f.player();
        if (!riding) {                                                              // a stand-in has no real connection: if it cannot sit, the drive checks are skipped, not failed
            try {
                riding = f.player().startRiding(car, true) && car.getControllingPassenger() == f.player();
            } catch (RuntimeException e) {
                riding = false;
            }
        }
        if (riding) {
            long faults = VbSkill.faults();
            boolean sent = CarriageSpells.call(c);
            if (VbSkill.faults() != faults) return VbSkill.lastFault();
            if (!sent || !car.isRemoved() || CarriageRegistry.find(f.server(), who) != null) return "using Call while driving did not send the carriage away";
            if (f.player().isPassenger()) return "the stand-in is still a passenger of the removed carriage";
            if (!CarriageSpells.call(c)) return "Call the Carriage refused a second call beside the dummy";
            CarriageEntity second = CarriageRegistry.find(f.server(), who);
            if (second == null || second == car) return "the second call did not make a fresh carriage";
            car = second;
        }
        car.dismiss();
        if (!car.isRemoved() || CarriageRegistry.find(f.server(), who) != null) return "sending the carriage away left it standing or registered";
        return null;
    }

    @Override
    public HeroKind kind() { return HeroKind.CARRIAGE; }

    @Override
    public List<HeroSkill> skills() { return skills; }

    /** The weapon is put away: the carriage goes with it (everybody aboard is put on the ground first). */
    @Override
    public void onDismiss(HeroCtx c) { sendAway(c.level.getServer(), c.player.getUUID()); }
}
