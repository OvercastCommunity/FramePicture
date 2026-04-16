package de.howaner.FramePicture.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMapData;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.bukkit.entity.Player;

public class PacketSender implements Runnable {
  private static final Queue<QueuedPacket> queue = new ConcurrentLinkedQueue<>();

  @Override
  public void run() {
    int loads = 0;
    while (!queue.isEmpty() && (loads++ <= Config.FRAME_LOADS_PER_TICK)) {
      QueuedPacket packet = queue.poll();
      if (!packet.player.isOnline()) return;

      PacketEvents.getAPI().getPlayerManager().sendPacket(packet.player, packet.packet);
    }
  }

  public static void removePlayerFromQueue(Player player) {
    synchronized (queue) {
      queue.removeIf(queuedPacket -> queuedPacket.player == player);
    }
  }

  public static void addPacketToQueue(Player player, WrapperPlayServerMapData packet) {
    queue.add(new QueuedPacket(player, packet));
  }

  private static class QueuedPacket {
    public Player player;
    public WrapperPlayServerMapData packet;

    public QueuedPacket(Player player, WrapperPlayServerMapData packet) {
      this.player = player;
      this.packet = packet;
    }
  }
}
