package de.howaner.FramePicture;

import com.github.retrooper.packetevents.PacketEvents;
import de.howaner.FramePicture.command.FramePictureCommand;
import de.howaner.FramePicture.listener.ChunkListener;
import de.howaner.FramePicture.listener.FrameListener;
import de.howaner.FramePicture.listener.FramePacketListener;
import de.howaner.FramePicture.util.Config;
import de.howaner.FramePicture.util.Frame;
import de.howaner.FramePicture.util.Lang;
import de.howaner.FramePicture.util.PacketSender;
import de.howaner.FramePicture.util.PictureDatabase;
import de.howaner.FramePicture.util.Utils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EntityTracker;
import net.minecraft.server.v1_8_R3.EntityTrackerEntry;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class FrameManager {
  public FramePicturePlugin p;
  public static File framesFile = new File("plugins/FramePicture/frames.yml");
  private final Map<String, List<Frame>> frames = new HashMap<>();
  private final Map<FrameKey, Frame> framesByKey = new ConcurrentHashMap<>();
  private final Map<Integer, Frame> loadedFramesByEntityId = new ConcurrentHashMap<>();
  private PictureDatabase pictureDB;

  private FramePacketListener framePacketListener;

  public FrameManager(FramePicturePlugin plugin) {
    this.p = plugin;
  }

  public void onEnable() {
    if (!Config.configFile.exists()) Config.save();
    Config.load();
    Config.save();

    Lang.load();

    this.pictureDB = new PictureDatabase();
    this.pictureDB.startScheduler();
    this.loadFrames();
    this.saveFrames();

    Bukkit.getPluginManager().registerEvents(new ChunkListener(this), this.p);
    Bukkit.getPluginManager().registerEvents(new FrameListener(this), this.p);

    p.getCommand("FramePicture").setExecutor(new FramePictureCommand(this));

    this.framePacketListener = new FramePacketListener();
    PacketEvents.getAPI().getEventManager().registerListener(this.framePacketListener);

    if (Config.FRAME_LOAD_ON_START) this.cacheFrames();

    Bukkit.getScheduler()
        .scheduleSyncRepeatingTask(
            this.p, new PacketSender(), Config.FRAME_LOADING_DELAY, Config.FRAME_LOADING_DELAY);
  }

  public void onDisable() {
    this.saveFrames();
    if (this.pictureDB != null) {
      this.pictureDB.stopScheduler();
      this.pictureDB.clear();
    }

    PacketEvents.getAPI().getEventManager().unregisterListener(this.framePacketListener);
    Bukkit.getScheduler().cancelTasks(this.p);
  }

  public void cacheFrames() {
    FramePicturePlugin.log.info("Caching frames ...");
    long memory = Utils.getUsedMemory();

    int amount = 0;
    for (List<Frame> frameList : this.frames.values()) {
      for (Frame frame : frameList) {
        frame.sendTo(null);
        amount++;
      }
    }
    FramePicturePlugin.log.info("Cached " + amount + " frames!");
    long usedMemory = Utils.getUsedMemory() - memory;
    if (usedMemory > 0L)
      FramePicturePlugin.log.info("The frame cache use " + usedMemory + "mb memory!");
  }

  public PictureDatabase getPictureDatabase() {
    return this.pictureDB;
  }

  public void removeFrame(Frame frame) {
    if (frame == null) return;

    this.unbindFrameEntity(frame);
    this.framesByKey.remove(FrameKey.of(frame.getLocation(), frame.getFacing()));

    Chunk chunk = frame.getLocation().getChunk();
    List<Frame> frameList =
        this.getFramesInChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    frameList.remove(frame);
    this.setFramesInChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ(), frameList);

    if (Config.FRAME_REMOVE_IMAGES && this.getFramesWithImage(frame.getPicture()).isEmpty()) {
      if (this.pictureDB.deleteImage(frame.getPicture())) {
        FramePicturePlugin.log.log(Level.INFO, "Removed image for frame #{0}.", frame.getId());
      }
    }

    this.saveFrames();
  }

  public void setFramesInChunk(String world, int chunkX, int chunkZ, List<Frame> frames) {
    this.frames.put(String.format("%s|%d|%d", world, chunkX, chunkZ), frames);
  }

  public Frame getFrame(Location loc, BlockFace face) {
    Frame frame = this.framesByKey.get(FrameKey.of(loc, face));
    if (frame != null || face == null) {
      return frame;
    }

    return this.framesByKey.get(FrameKey.of(loc, null));
  }

  public List<Frame> getFramesInChunk(String world, int chunkX, int chunkZ) {
    List<Frame> frameList = this.frames.get(String.format("%s|%d|%d", world, chunkX, chunkZ));
    if (frameList == null) {
      frameList = new ArrayList<>();
    }
    return frameList;
  }

  public Frame getFrameWithEntityID(int entityId) {
    return this.loadedFramesByEntityId.get(entityId);
  }

  public void sendFrame(Frame frame) {
    if (!frame.isLoaded()) return;

    ItemFrame entity = frame.getEntity();
    WorldServer worldServer = ((CraftWorld) entity.getWorld()).getHandle();
    EntityTracker tracker = worldServer.tracker;
    EntityTrackerEntry trackerEntry = tracker.trackedEntities.d(entity.getEntityId());
    if (trackerEntry == null) return;

    for (EntityPlayer playerNMS : trackerEntry.trackedPlayers) {
      Player player = playerNMS.getBukkitEntity();
      frame.sendTo(player);
    }
  }

  public int getNewFrameID() {
    int highestId = -1;
    for (List<Frame> frameList : this.frames.values()) {
      for (Frame frame : frameList) {
        highestId = Math.max(highestId, frame.getId());
      }
    }

    return (highestId + 1);
  }

  public Frame addFrame(String pictureURL, ItemFrame entity) {
    Frame frame =
        new Frame(this.getNewFrameID(), pictureURL, entity.getLocation(), entity.getFacing());
    this.registerFrame(frame);
    this.bindFrameEntity(frame, entity);

    if (frame.getBufferImage() == null) {
      this.unbindFrameEntity(frame);
      this.framesByKey.remove(FrameKey.of(frame.getLocation(), frame.getFacing()));
      return null;
    }

    Chunk chunk = entity.getLocation().getChunk();
    List<Frame> frameList =
        this.getFramesInChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    frameList.add(frame);
    this.setFramesInChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ(), frameList);

    Utils.setFrameItemWithoutSending(entity, new ItemStack(Material.AIR));
    this.sendFrame(frame);
    this.saveFrames();
    return frame;
  }

  public List<Frame> getFramesWithImage(String image) {
    List<Frame> frameList = new ArrayList<>();
    for (List<Frame> frames : this.frames.values()) {
      for (Frame frame : frames) {
        if (frame.getPicture().equals(image)) {
          frameList.add(frame);
        }
      }
    }
    return frameList;
  }

  public List<Frame> getFrames() {
    List<Frame> frameList = new ArrayList<>();
    for (List<Frame> frames : this.frames.values()) {
      frameList.addAll(frames);
    }
    return frameList;
  }

  public Frame getFrame(ItemFrame entity) {
    return this.loadedFramesByEntityId.get(entity.getEntityId());
  }

  public List<Frame> addMultiFrames(
      BufferedImage img, ItemFrame[] frames, int vertical, int horizontal) {
    if (frames.length == 0 || horizontal <= 0) return null;
    img = Utils.scaleImage(img, img.getWidth() * vertical, img.getHeight() * horizontal);

    int width = img.getWidth() / vertical;
    int height = img.getHeight() / horizontal;

    List<Frame> frameList = new ArrayList<>();
    int globalId = this.getNewFrameID();
    int id = globalId;
    // y = Horizontal
    for (int y = 0; y < horizontal; y++) {
      // x = Vertical
      for (int x = 0; x < vertical; x++) {
        BufferedImage frameImg = Utils.cutImage(img, x * width, y * height, width, height);
        frameImg = Utils.scaleImage(frameImg, 128, 128, false);
        File file =
            this.pictureDB.writeImage(frameImg, String.format("Frame%s_%s-%s", globalId, x, y));

        ItemFrame entity = frames[vertical * y + x];
        Utils.setFrameItemWithoutSending(entity, new ItemStack(Material.AIR));
        Frame frame = this.getFrame(entity);
        if (frame != null) this.removeFrame(frame);

        frame = new Frame(globalId, file.getName(), entity.getLocation(), entity.getFacing());
        this.registerFrame(frame);
        this.bindFrameEntity(frame, entity);

        Chunk chunk = frame.getLocation().getChunk();
        List<Frame> chunkFrames =
            this.getFramesInChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        chunkFrames.add(frame);
        this.setFramesInChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ(), chunkFrames);

        globalId++;
        frameList.add(frame);
        this.sendFrame(frame);
      }
    }

    this.saveFrames();
    return frameList;
  }

  public void loadFrames() {
    YamlConfiguration config = YamlConfiguration.loadConfiguration(framesFile);
    this.frames.clear();
    this.framesByKey.clear();
    this.loadedFramesByEntityId.clear();

    for (String key : config.getKeys(false)) {
      ConfigurationSection section = config.getConfigurationSection(key);
      final int id = Integer.parseInt(key);

      World world = Bukkit.getWorld(section.getString("World"));
      if (world == null) {
        FramePicturePlugin.log.log(
            Level.WARNING, "Can't find world {0}!", section.getString("World"));
        continue;
      }
      final Location loc =
          new Location(world, section.getInt("X"), section.getInt("Y"), section.getInt("Z"));

      BlockFace face = null;
      if (section.contains("Facing")) {
        face = BlockFace.valueOf(section.getString("Facing"));
      }

      String picture = section.getString("Picture");
      Frame frame = new Frame(id, picture, loc, face);
      this.registerFrame(frame);
      Chunk chunk = loc.getChunk();

      if (chunk.isLoaded()) {
        ItemFrame entity = Utils.getItemFrameFromChunk(chunk, loc, face);
        if (entity != null) {
          Utils.setFrameItemWithoutSending(entity, new ItemStack(Material.AIR));
          this.bindFrameEntity(frame, entity);
          this.sendFrame(frame);
        }
      }

      List<Frame> frameList =
          this.getFramesInChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
      frameList.add(frame);
      this.setFramesInChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ(), frameList);
    }
    FramePicturePlugin.log.log(Level.INFO, "Loaded {0} frames!", this.getFrames().size());
  }

  public void saveFrames() {
    YamlConfiguration config = new YamlConfiguration();

    for (List<Frame> frameList : this.frames.values()) {
      for (Frame frame : frameList) {
        ConfigurationSection section = config.createSection(String.valueOf(frame.getId()));

        section.set("Picture", frame.getPicture());
        section.set("World", frame.getLocation().getWorld().getName());
        section.set("X", frame.getLocation().getBlockX());
        section.set("Y", frame.getLocation().getBlockY());
        section.set("Z", frame.getLocation().getBlockZ());

        if (frame.getFacing() != null) {
          section.set("Facing", frame.getFacing().name());
        }
      }
    }

    try {
      config.save(framesFile);
    } catch (IOException e) {
      FramePicturePlugin.log.log(Level.WARNING, "Error while saving the frames!", e);
    }
  }

  public void registerFrame(Frame frame) {
    this.framesByKey.put(FrameKey.of(frame.getLocation(), frame.getFacing()), frame);
  }

  public void bindFrameEntity(Frame frame, ItemFrame entity) {
    this.unbindFrameEntity(frame);
    frame.setEntity(entity);
    this.loadedFramesByEntityId.put(entity.getEntityId(), frame);
  }

  public void unbindFrameEntity(Frame frame) {
    if (!frame.isLoaded()) return;

    this.loadedFramesByEntityId.remove(frame.getEntity().getEntityId());
    frame.setEntity(null);
  }

  public record FrameKey(String world, int x, int y, int z, BlockFace facing) {
    public static FrameKey of(Location loc, BlockFace facing) {
      return new FrameKey(
          loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), facing);
    }
  }
}
