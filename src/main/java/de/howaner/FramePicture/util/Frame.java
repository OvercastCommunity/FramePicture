package de.howaner.FramePicture.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMapData;
import de.howaner.FramePicture.FramePicturePlugin;
import de.howaner.FramePicture.render.ImageRenderer;
import de.howaner.FramePicture.render.TextRenderer;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapRenderer;

public class Frame {
  private final int id;
  private ItemFrame entity;
  private final BlockFace face;
  private final Location loc;
  private final String picture;
  private WrapperPlayServerEntityMetadata cachedItemPacket = null;
  private WrapperPlayServerMapData cachedDataPacket = null;

  public Frame(final int id, String picture, Location loc, BlockFace face) {
    this.id = id;
    this.picture = picture;
    this.loc = loc;
    this.face = face;
  }

  public boolean isLoaded() {
    return (this.entity != null);
  }

  public int getId() {
    return this.id;
  }

  public short getMapId() {
    return (short) (2300 + this.id);
  }

  public Location getLocation() {
    return this.loc;
  }

  public ItemFrame getEntity() {
    return this.entity;
  }

  public BlockFace getFacing() {
    return this.face;
  }

  public String getPicture() {
    return this.picture;
  }

  public void setEntity(ItemFrame entity) {
    this.entity = entity;
    this.cachedItemPacket = null;
  }

  public void clearCache() {
    this.cachedDataPacket = null;
    this.cachedItemPacket = null;
  }

  public BufferedImage getBufferImage() {
    BufferedImage image =
        FramePicturePlugin.getManager().getPictureDatabase().loadImage(this.picture);
    if (image != null && Config.CHANGE_SIZE_ENABLED)
      image = Utils.scaleImage(image, Config.SIZE_WIDTH, Config.SIZE_HEIGHT);
    return image;
  }

  private byte[] getRenderBuffer() {
    MapRenderer mapRenderer = this.generateRenderer();
    SimpleMapCanvas canvas = new SimpleMapCanvas();
    mapRenderer.render(canvas.getMapView(), canvas, null);
    byte[] buf = canvas.getBuffer();
    byte[] result = new byte[buf.length];
    for (int i = 0; i < buf.length; i++) {
      byte color = buf[i];
      if ((color >= 0) || (color <= -113)) result[i] = color;
    }
    return result;
  }

  public void sendTo(Player player) {
    this.sendItemMeta(player);
    this.sendMapData(player);
  }

  private void sendItemMeta(Player player) {
    if (!this.isLoaded()) return;

    if (this.cachedItemPacket == null) {
      ItemStack item = new ItemStack(Material.MAP);
      item.setDurability(this.getMapId());

      List<EntityData<?>> metadata = new ArrayList<>();
      metadata.add(
          new EntityData<>(
              8, EntityDataTypes.ITEMSTACK, SpigotConversionUtil.fromBukkitItemStack(item)));

      this.cachedItemPacket =
          new WrapperPlayServerEntityMetadata(this.entity.getEntityId(), metadata);
    }

    if (player != null)
      PacketEvents.getAPI().getPlayerManager().sendPacket(player, this.cachedItemPacket);
  }

  private void sendMapData(Player player) {
    if (this.cachedDataPacket == null) {
      byte[] data = this.getRenderBuffer();
      this.cachedDataPacket =
          new WrapperPlayServerMapData(
              this.getMapId(), (byte) 3, false, false, null, 128, 128, 0, 0, data);
    }

    if (player != null) PacketSender.addPacketToQueue(player, this.cachedDataPacket);
  }

  public MapRenderer generateRenderer() {
    BufferedImage image = Frame.this.getBufferImage();
    if (image == null) {
      FramePicturePlugin.log.log(
          Level.WARNING,
          "The picture \"{0}\" from frame #{1} doesn't exists!",
          new Object[] {Frame.this.getPicture(), Frame.this.getId()});
      return new TextRenderer("Can't read image!", this.getId());
    }

    ImageRenderer renderer = new ImageRenderer(image);
    if (Config.CHANGE_SIZE_ENABLED && Config.SIZE_CENTER) {
      if (image.getWidth() < 128) renderer.imageX = (128 - image.getWidth()) / 2;
      if (image.getHeight() < 128) renderer.imageY = (128 - image.getHeight()) / 2;
    }

    return renderer;
  }
}
