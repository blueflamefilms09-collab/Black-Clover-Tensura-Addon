import com.newuniverse.nusmp.labyrinth.mega.lock.LockText;
import com.newuniverse.nusmp.labyrinth.mega.system.FloorInfoText;

import java.util.Map;

/**
 * Why: the English text of every key of the System's floor report and of the escape rules lives in ONE map each in the code (FloorInfoText, LockText), so the lang fragment
 * cannot drift from the code; this prints both maps as "key TAB text" lines for tools/system/gen_floor_info_lang.py, which writes tools/lang/system_floor_info.json.
 * Nothing here is part of the mod.
 */
public final class FloorInfoLangDump {
    public static void main(String[] args) {
        for (Map.Entry<String, String> e : FloorInfoText.all().entrySet()) System.out.println(e.getKey() + "\t" + e.getValue());
        for (Map.Entry<String, String> e : LockText.all().entrySet()) System.out.println(e.getKey() + "\t" + e.getValue());
    }
}
