package de.howaner.FramePicture.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMapData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.bukkit.entity.Player;

public class PacketSender implements Runnable {
  private static final Map<Player, Queue<QueuedPacket>> queues = new ConcurrentHashMap<>();

  @Override
  public void run() {
    int remainingLoads = Config.FRAME_MAX_LOADS_PER_TICK;
    for (Map.Entry<Player, Queue<QueuedPacket>> entry : queues.entrySet()) {
      if (remainingLoads <= 0) return;

      Player player = entry.getKey();
      Queue<QueuedPacket> queue = entry.getValue();

      if (!player.isOnline()) {
        queues.remove(player, queue);
        continue;
      }

      List<QueuedPacket> batch = new ArrayList<>();
      int loads = 0;
      int playerLoads = Math.min(Config.FRAME_LOADS_PER_TICK, remainingLoads);
      while (!queue.isEmpty() && (loads++ < playerLoads)) {
        QueuedPacket packet = queue.poll();
        if (packet != null) batch.add(packet);
      }
      remainingLoads -= batch.size();

      if (!batch.isEmpty()) {
        sendBatch(player, batch);
      }

      if (queue.isEmpty()) {
        queues.remove(player, queue);
      }
    }
  }

  public static void removePlayerFromQueue(Player player) {
    queues.remove(player);
  }

  public static void addPacketToQueue(Player player, int mapId, byte[] data) {
    queues
        .computeIfAbsent(player, ignored -> new ConcurrentLinkedQueue<>())
        .add(new QueuedPacket(mapId, data));
  }

  private static void sendBatch(Player player, List<QueuedPacket> batch) {
    for (int i = 0; i < batch.size(); i++) {
      QueuedPacket packet = batch.get(i);
      WrapperPlayServerMapData wrapper =
          new WrapperPlayServerMapData(
              packet.mapId, (byte) 3, false, false, null, 128, 128, 0, 0, packet.data);

      if (i == batch.size() - 1) {
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, wrapper);
      } else {
        PacketEvents.getAPI().getPlayerManager().writePacketSilently(player, wrapper);
      }
    }
  }

  private static class QueuedPacket {
    public int mapId;
    public byte[] data;

    public QueuedPacket(int mapId, byte[] data) {
      this.mapId = mapId;
      this.data = data;
    }
  }
}
