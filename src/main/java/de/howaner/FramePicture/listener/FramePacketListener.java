package de.howaner.FramePicture.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import de.howaner.FramePicture.FramePicturePlugin;
import de.howaner.FramePicture.util.Frame;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public class FramePacketListener extends PacketListenerAbstract {

  public FramePacketListener() {
    super(PacketListenerPriority.LOW);
  }

  @Override
  public void onPacketSend(PacketSendEvent event) {
    if (event.getPacketType() != PacketType.Play.Server.SPAWN_ENTITY) return;

    var wrapper = new WrapperPlayServerSpawnEntity(event);
    if (wrapper.getEntityType() != EntityTypes.ITEM_FRAME) return;

    Player player = event.getPlayer();
    int entityID = wrapper.getEntityId();
    Vector3d pos = wrapper.getPosition();
    Location loc = new Location(player.getWorld(), pos.x, pos.y, pos.z);
    int direction = wrapper.getData();

    Chunk chunk = loc.getChunk();
    if (!chunk.isLoaded()) return;

    Frame frame = FramePicturePlugin.getManager().getFrameWithEntityID(entityID);
    if (frame == null) {
      BlockFace facing = convertDirectionToBlockFace(direction);
      frame = FramePicturePlugin.getManager().getFrame(loc, facing);
      if (frame == null) return;
    }

    final Frame frameToSend = frame;
    Bukkit.getScheduler()
        .runTaskLater(FramePicturePlugin.getPlugin(), () -> frameToSend.sendTo(player), 10L);
  }

  private BlockFace convertDirectionToBlockFace(int direction) {
    return switch (direction) {
      case 0 -> BlockFace.SOUTH;
      case 1 -> BlockFace.WEST;
      case 3 -> BlockFace.EAST;
      default -> BlockFace.NORTH;
    };
  }
}
