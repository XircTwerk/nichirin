package com.xirc.nichirin.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Client-side, per-user preferences that must survive a game restart — persisted to
 * {@code config/nichirin-client.properties} in the run directory (NOT the server config, since these are
 * personal client choices). Currently just remembers whether the "press G to open the menu" join tip has
 * been dismissed via its "Don't show this again" link.
 */
@Environment(EnvType.CLIENT)
public final class MenuTipPrefs {

    private static final String FILE_NAME = "nichirin-client.properties";
    private static final String HIDE_MENU_TIP = "hideMenuTip";

    private static Boolean cachedHidden;

    private MenuTipPrefs() {}

    public static boolean isMenuTipHidden() {
        if (cachedHidden == null) {
            cachedHidden = Boolean.parseBoolean(load().getProperty(HIDE_MENU_TIP, "false"));
        }
        return cachedHidden;
    }

    public static void setMenuTipHidden(boolean hidden) {
        cachedHidden = hidden;
        Properties props = load();
        props.setProperty(HIDE_MENU_TIP, Boolean.toString(hidden));
        save(props);
    }

    private static File file() {
        return new File(new File(Minecraft.getInstance().gameDirectory, "config"), FILE_NAME);
    }

    private static Properties load() {
        Properties props = new Properties();
        File f = file();
        if (f.isFile()) {
            try (FileInputStream in = new FileInputStream(f)) {
                props.load(in);
            } catch (IOException ignored) {
            }
        }
        return props;
    }

    private static void save(Properties props) {
        File dir = new File(Minecraft.getInstance().gameDirectory, "config");
        if (!dir.exists() && !dir.mkdirs()) return;
        try (FileOutputStream out = new FileOutputStream(new File(dir, FILE_NAME))) {
            props.store(out, "Breath of Nichirin client preferences");
        } catch (IOException ignored) {
        }
    }
}
