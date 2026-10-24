package examples;

import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Form;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.LanceMode;

/** Valkyrie armour v2 without the lance (modeArmor.valkyrieLance = OFF). */
public final class Vg2NoLance {
    private Vg2NoLance() {}

    public static void register() { Vg2Common.install(true, Form.VALKYRIE, LanceMode.OFF); }
}
