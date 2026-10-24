package examples;

import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Form;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.LanceMode;

/** Valkyrie armour v2, the VALKYRIE look with the lance floating beside the wearer. */
public final class Vg2Float {
    private Vg2Float() {}

    public static void register() { Vg2Common.install(true, Form.VALKYRIE, LanceMode.FLOATING); }
}
