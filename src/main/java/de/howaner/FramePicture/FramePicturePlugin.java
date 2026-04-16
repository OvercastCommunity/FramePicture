package de.howaner.FramePicture;

import de.howaner.FramePicture.util.Lang;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class FramePicturePlugin extends JavaPlugin {
  public static Logger log;
  private static FrameManager manager = null;
  private static FramePicturePlugin instance;
  private boolean invalidBukkit = false;

  private void checkBukkitVersion() {
    try {
      Class.forName("net.minecraft.server.v1_8_R3.Packet");
      this.invalidBukkit = false;
    } catch (Exception e) {
      this.invalidBukkit = true;
    }
  }

  @Override
  public void onLoad() {
    log = this.getLogger();
    instance = this;

    this.checkBukkitVersion();
    if (!this.invalidBukkit) manager = new FrameManager(this);
  }

  @Override
  public void onEnable() {
    if (log == null) log = this.getLogger();
    if (instance == null) instance = this;

    // Check Bukkit Version
    if (this.invalidBukkit) {
      log.severe("You use a not-supported bukkit version!");
      log.severe("This FramePicture version is for Spigot 1.8!");
      Bukkit.getPluginManager().disablePlugin(this);
      return;
    }

    if (manager == null) manager = new FrameManager(this);
    manager.onEnable();
    log.info(Lang.PLUGIN_ENABLED.getText());
  }

  @Override
  public void onDisable() {
    if (manager != null) manager.onDisable();
    log.info(Lang.PLUGIN_DISABLED.getText());
  }

  public static FramePicturePlugin getPlugin() {
    return instance;
  }

  public static FrameManager getManager() {
    return manager;
  }
}
