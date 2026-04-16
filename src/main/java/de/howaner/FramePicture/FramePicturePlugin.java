package de.howaner.FramePicture;

import de.howaner.FramePicture.util.Lang;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public class FramePicturePlugin extends JavaPlugin {
  public static Logger log;
  private static FrameManager manager = null;
  private static FramePicturePlugin instance;

  @Override
  public void onLoad() {
    log = this.getLogger();
    instance = this;
    manager = new FrameManager(this);
  }

  @Override
  public void onEnable() {
    if (log == null) log = this.getLogger();
    if (instance == null) instance = this;
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
