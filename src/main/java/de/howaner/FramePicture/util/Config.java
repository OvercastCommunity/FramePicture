package de.howaner.FramePicture.util;

import de.howaner.FramePicture.FramePicturePlugin;
import java.io.File;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;

public class Config {

  public static boolean CHANGE_SIZE_ENABLED = true;
  public static int SIZE_WIDTH = 128;
  public static int SIZE_HEIGHT = 128;
  public static boolean SIZE_CENTER = true;
  public static double CREATE_PRICE = 10.0;
  public static boolean FRAME_REMOVE_IMAGES = false;
  public static boolean FRAME_LOAD_ON_START = true;
  public static long FRAME_LOADING_DELAY = 1L;
  public static int FRAME_LOADS_PER_TICK = 5;
  // File
  public static File configFile = new File("plugins/FramePicture/config.yml");

  public static void load() {
    YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    CHANGE_SIZE_ENABLED = config.getBoolean("AutoSize.Enabled");
    SIZE_WIDTH = config.getInt("AutoSize.Width");
    SIZE_HEIGHT = config.getInt("AutoSize.Height");
    SIZE_CENTER = config.getBoolean("AutoSize.Center");
    CREATE_PRICE = config.getDouble("Money.CreatePrice");
    if (config.isBoolean("Frame.RemoveImageWhenFrameDestroy"))
      FRAME_REMOVE_IMAGES = config.getBoolean("Frame.RemoveImageWhenFrameDestroy");
    if (config.isBoolean("Frame.LoadOnStart"))
      FRAME_LOAD_ON_START = config.getBoolean("Frame.CacheOnStart");
    if (config.isLong("FrameLoader.TickDelay"))
      FRAME_LOADING_DELAY = Math.max(config.getLong("FrameLoader.TickDelay"), 1L);
    if (config.isInt("FrameLoader.LoadsPerTick"))
      FRAME_LOADS_PER_TICK = Math.max(config.getInt("FrameLoader.LoadsPerTick"), 1);

    FramePicturePlugin.log.info("Configuration loaded!");
  }

  public static void save() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("AutoSize.Enabled", CHANGE_SIZE_ENABLED);
    config.set("AutoSize.Width", SIZE_WIDTH);
    config.set("AutoSize.Height", SIZE_HEIGHT);
    config.set("AutoSize.Center", SIZE_CENTER);
    config.set("Money.CreatePrice", CREATE_PRICE);
    config.set("Frame.RemoveImageWhenFrameDestroy", FRAME_REMOVE_IMAGES);
    config.set("Frame.CacheOnStart", FRAME_LOAD_ON_START);
    config.set("FrameLoader.TickDelay", FRAME_LOADING_DELAY);
    config.set("FrameLoader.LoadsPerTick", FRAME_LOADS_PER_TICK);
    try {
      config.save(configFile);
    } catch (Exception e) {
      FramePicturePlugin.log.log(Level.WARNING, "Error while saving the Config!");
      e.printStackTrace();
    }
  }
}
