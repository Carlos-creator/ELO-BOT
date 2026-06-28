import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import javax.swing.AbstractButton;
import javax.swing.Icon;

/**
 * Ícono vectorial reutilizable.
 * Permite dibujar íconos sin depender de imágenes externas ni emojis.
 */
public class VectorIcon implements Icon {

   private final String type;
   private final int size;

   public VectorIcon(String type, int size) {
      this.type = type;
      this.size = size;
   }

   public int getIconWidth() {
      return this.size;
   }

   public int getIconHeight() {
      return this.size;
   }

   public void paintIcon(Component c, Graphics g, int x, int y) {
      Color color = c.getForeground() != null ? c.getForeground() : Theme.getInstance().getFgMuted();

      if (c instanceof AbstractButton && !Color.WHITE.equals(color)) {
         boolean hot = ((AbstractButton) c).getModel().isRollover();

         if (hot) {
            color = Theme.ACCENT2;
         }
      }

      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      drawIcon(g2, this.type, x + this.size / 2f, y + this.size / 2f, this.size, color);

      g2.dispose();
   }

   private static void drawIcon(Graphics2D base, String type, float cx, float cy, float size, Color color) {
      Graphics2D g = (Graphics2D) base.create();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(color);

      float h = size / 2f;
      float strokeW = Math.max(1.3f, size * 0.1f);

      g.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

      switch (type) {
         case "save": {
            g.draw(line(cx - h * 0.8f, cy + h * 0.45f, cx + h * 0.8f, cy + h * 0.45f));
            g.draw(line(cx - h * 0.8f, cy + h * 0.45f, cx - h * 0.8f, cy + h * 0.05f));
            g.draw(line(cx + h * 0.8f, cy + h * 0.45f, cx + h * 0.8f, cy + h * 0.05f));
            g.draw(line(cx, cy - h * 0.75f, cx, cy + h * 0.12f));

            int[] xs = {(int) (cx - h * 0.4f), (int) cx, (int) (cx + h * 0.4f)};
            int[] ys = {(int) (cy - h * 0.12f), (int) (cy + h * 0.3f), (int) (cy - h * 0.12f)};

            g.fillPolygon(xs, ys, 3);
            break;
         }

         case "trash": {
            g.draw(line(cx - h * 0.62f, cy - h * 0.55f, cx + h * 0.62f, cy - h * 0.55f));
            g.draw(line(cx - h * 0.22f, cy - h * 0.55f, cx - h * 0.16f, cy - h * 0.8f));
            g.draw(line(cx + h * 0.22f, cy - h * 0.55f, cx + h * 0.16f, cy - h * 0.8f));
            g.draw(line(cx - h * 0.16f, cy - h * 0.8f, cx + h * 0.16f, cy - h * 0.8f));
            g.draw(line(cx - h * 0.48f, cy - h * 0.42f, cx - h * 0.36f, cy + h * 0.78f));
            g.draw(line(cx + h * 0.48f, cy - h * 0.42f, cx + h * 0.36f, cy + h * 0.78f));
            g.draw(line(cx - h * 0.36f, cy + h * 0.78f, cx + h * 0.36f, cy + h * 0.78f));
            g.draw(line(cx, cy - h * 0.22f, cx, cy + h * 0.55f));
            g.draw(line(cx - h * 0.18f, cy - h * 0.22f, cx - h * 0.15f, cy + h * 0.55f));
            g.draw(line(cx + h * 0.18f, cy - h * 0.22f, cx + h * 0.15f, cy + h * 0.55f));
            break;
         }

         case "moon": {
            Ellipse2D full = new Ellipse2D.Float(cx - h * 0.72f, cy - h * 0.72f, h * 1.44f, h * 1.44f);
            Ellipse2D bite = new Ellipse2D.Float(cx - h * 0.18f, cy - h * 0.85f, h * 1.44f, h * 1.44f);

            Area moonArea = new Area(full);
            moonArea.subtract(new Area(bite));

            g.fill(moonArea);
            break;
         }

         case "sun": {
            g.fillOval((int) (cx - h * 0.4f), (int) (cy - h * 0.4f), (int) (h * 0.8f), (int) (h * 0.8f));

            for (int i = 0; i < 8; i++) {
               double ang = Math.PI * 2 * i / 8;
               int x1 = (int) (cx + Math.cos(ang) * h * 0.62f);
               int y1 = (int) (cy + Math.sin(ang) * h * 0.62f);
               int x2 = (int) (cx + Math.cos(ang) * h * 0.95f);
               int y2 = (int) (cy + Math.sin(ang) * h * 0.95f);

               g.drawLine(x1, y1, x2, y2);
            }

            break;
         }

         case "share": {
            int r = Math.max(2, (int) (h * 0.17f));

            int x1 = (int) (cx - h * 0.6f);
            int y1 = (int) cy;

            int x2 = (int) (cx + h * 0.55f);
            int y2 = (int) (cy - h * 0.55f);

            int x3 = (int) (cx + h * 0.55f);
            int y3 = (int) (cy + h * 0.55f);

            g.drawLine(x1, y1, x2, y2);
            g.drawLine(x1, y1, x3, y3);

            g.fillOval(x1 - r, y1 - r, r * 2, r * 2);
            g.fillOval(x2 - r, y2 - r, r * 2, r * 2);
            g.fillOval(x3 - r, y3 - r, r * 2, r * 2);

            break;
         }

         case "copy": {
            g.drawRoundRect((int) (cx - h * 0.55f), (int) (cy - h * 0.65f), (int) (h * 0.95f), (int) (h * 1.05f), 4, 4);
            g.drawRoundRect((int) (cx - h * 0.3f), (int) (cy - h * 0.3f), (int) (h * 0.95f), (int) (h * 1.05f), 4, 4);
            break;
         }

         case "key": {
            g.drawOval((int) (cx - h * 0.85f), (int) (cy - h * 0.4f), (int) (h * 0.8f), (int) (h * 0.8f));
            g.drawLine((int) (cx - h * 0.1f), (int) cy, (int) (cx + h * 0.8f), (int) cy);
            g.drawLine((int) (cx + h * 0.45f), (int) cy, (int) (cx + h * 0.45f), (int) (cy + h * 0.32f));
            g.drawLine((int) (cx + h * 0.72f), (int) cy, (int) (cx + h * 0.72f), (int) (cy + h * 0.24f));
            break;
         }

         case "list": {
            for (int i = 0; i < 3; i++) {
               int y = (int) (cy - h * 0.5f + i * h * 0.5f);

               g.fillOval((int) (cx - h * 0.85f) - 1, y - 2, 4, 4);
               g.drawLine((int) (cx - h * 0.55f), y, (int) (cx + h * 0.85f), y);
            }

            break;
         }

         case "check": {
            g.drawRoundRect((int) (cx - h * 0.85f), (int) (cy - h * 0.85f), (int) (h * 1.7f), (int) (h * 1.7f), 5, 5);
            g.drawLine((int) (cx - h * 0.4f), (int) cy, (int) (cx - h * 0.05f), (int) (cy + h * 0.35f));
            g.drawLine((int) (cx - h * 0.05f), (int) (cy + h * 0.35f), (int) (cx + h * 0.5f), (int) (cy - h * 0.35f));
            break;
         }

         case "book": {
            g.drawRoundRect((int) (cx - h * 0.8f), (int) (cy - h * 0.85f), (int) (h * 1.6f), (int) (h * 1.7f), 3, 3);
            g.drawLine((int) (cx - h * 0.28f), (int) (cy - h * 0.85f), (int) (cx - h * 0.28f), (int) (cy + h * 0.85f));
            g.drawLine((int) (cx - h * 0.02f), (int) (cy - h * 0.4f), (int) (cx + h * 0.55f), (int) (cy - h * 0.4f));
            g.drawLine((int) (cx - h * 0.02f), (int) cy, (int) (cx + h * 0.55f), (int) cy);
            g.drawLine((int) (cx - h * 0.02f), (int) (cy + h * 0.4f), (int) (cx + h * 0.4f), (int) (cy + h * 0.4f));
            break;
         }

         case "class": {
            g.drawRect((int) (cx - h * 0.8f), (int) (cy - h * 0.8f), (int) (h * 1.6f), (int) (h * 1.6f));
            g.drawLine((int) (cx - h * 0.8f), (int) (cy - h * 0.25f), (int) (cx + h * 0.8f), (int) (cy - h * 0.25f));
            g.drawLine((int) (cx - h * 0.8f), (int) (cy + h * 0.3f), (int) (cx + h * 0.8f), (int) (cy + h * 0.3f));
            break;
         }

         case "plus": {
            g.draw(line(cx - h * 0.6f, cy, cx + h * 0.6f, cy));
            g.draw(line(cx, cy - h * 0.6f, cx, cy + h * 0.6f));
            break;
         }

         default:
            break;
      }

      g.dispose();
   }

   private static java.awt.geom.Line2D.Float line(float x1, float y1, float x2, float y2) {
      return new java.awt.geom.Line2D.Float(x1, y1, x2, y2);
   }
}