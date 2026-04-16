package de.howaner.FramePicture.util;

import java.awt.Image;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapFont;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapView;

public class SimpleMapCanvas implements MapCanvas {

  private final byte[] buffer = new byte[128 * 128];
  private MapCursorCollection cursors = new MapCursorCollection();

  public byte[] getBuffer() {
    return buffer;
  }

  @Override
  public MapView getMapView() {
    return null;
  }

  @Override
  public MapCursorCollection getCursors() {
    return cursors;
  }

  @Override
  public void setCursors(MapCursorCollection cursors) {
    this.cursors = cursors;
  }

  @Override
  public void setPixel(int x, int y, byte color) {
    if (x < 0 || y < 0 || x >= 128 || y >= 128) return;
    buffer[y * 128 + x] = color;
  }

  @Override
  public byte getPixel(int x, int y) {
    if (x < 0 || y < 0 || x >= 128 || y >= 128) return 0;
    return buffer[y * 128 + x];
  }

  @Override
  public byte getBasePixel(int x, int y) {
    return 0;
  }

  @SuppressWarnings("deprecation")
  @Override
  public void drawImage(int x, int y, Image image) {
    byte[] colors = MapPalette.imageToBytes(image);
    int imgWidth = image.getWidth(null);
    int imgHeight = image.getHeight(null);
    for (int row = 0; row < imgHeight; row++) {
      for (int col = 0; col < imgWidth; col++) {
        byte color = colors[row * imgWidth + col];
        if (color == MapPalette.TRANSPARENT) continue;
        setPixel(x + col, y + row, color);
      }
    }
  }

  @Override
  public void drawText(int x, int y, MapFont font, String text) {}
}
