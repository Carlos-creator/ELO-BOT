import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.Timer;

/**
 * Componente visual reutilizable: avatar animado de ELOBOT.
 * Tiene animación al pasar el mouse, pulso mientras el bot piensa y parpadeo ambiental.
 */
public class AnimatedAvatar extends JLabel {

   private double hoverPhase = 0.0;
   private Timer hoverTimer;
   private boolean pulsing = false;
   private double pulsePhase = 0.0;
   private Timer pulseTimer;
   private double blinkPhase = -1.0;
   private Timer blinkTimer;
   private final Timer ambientTimer;
   private final int size;

   public AnimatedAvatar(int size) {
      this.size = size;
      Dimension d = new Dimension(size, size);
      this.setPreferredSize(d);
      this.setMaximumSize(d);
      this.setMinimumSize(d);
      this.setCursor(new Cursor(Cursor.HAND_CURSOR));

      this.addMouseListener(new MouseAdapter() {
         public void mouseEntered(MouseEvent e) {
            startHoverAnim();
         }
      });

      int delay = 3800 + (int) (Math.random() * 2600);
      this.ambientTimer = new Timer(delay, null);
      this.ambientTimer.addActionListener(e -> startBlink());
      this.ambientTimer.start();
   }

   private void startBlink() {
      if (this.pulsing || (this.hoverTimer != null && this.hoverTimer.isRunning())) {
         return;
      }

      this.blinkPhase = 0.0;

      if (this.blinkTimer != null) {
         this.blinkTimer.stop();
      }

      this.blinkTimer = new Timer(16, null);
      this.blinkTimer.addActionListener(e -> {
         this.blinkPhase += 0.14;

         if (this.blinkPhase >= 1.0) {
            this.blinkPhase = -1.0;
            this.blinkTimer.stop();
         }

         this.repaint();
      });

      this.blinkTimer.start();
   }

   private void startHoverAnim() {
      if (this.hoverTimer != null && this.hoverTimer.isRunning()) {
         return;
      }

      this.hoverPhase = 0.0;

      this.hoverTimer = new Timer(15, null);
      this.hoverTimer.addActionListener(e -> {
         this.hoverPhase += 0.05;

         if (this.hoverPhase >= 1.0) {
            this.hoverPhase = 0.0;
            this.hoverTimer.stop();
         }

         this.repaint();
      });

      this.hoverTimer.start();
   }

   public void setPulsing(boolean p) {
      this.pulsing = p;

      if (p) {
         if (this.pulseTimer == null) {
            this.pulseTimer = new Timer(40, e -> {
               this.pulsePhase += 0.22;
               this.repaint();
            });
         }

         this.pulseTimer.start();
      } else {
         if (this.pulseTimer != null) {
            this.pulseTimer.stop();
         }

         this.pulsePhase = 0.0;
         this.repaint();
      }
   }

   public void removeNotify() {
      super.removeNotify();

      if (this.hoverTimer != null) {
         this.hoverTimer.stop();
      }

      if (this.pulseTimer != null) {
         this.pulseTimer.stop();
      }

      if (this.blinkTimer != null) {
         this.blinkTimer.stop();
      }

      if (this.ambientTimer != null) {
         this.ambientTimer.stop();
      }
   }

   private static Color lerp(Color a, Color b, double t) {
      t = Math.max(0.0, Math.min(1.0, t));

      int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
      int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
      int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);

      return new Color(r, g, bl);
   }

   protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      double scale = 1.0;
      double rotation = 0.0;
      Color c1 = Theme.ACCENT;
      Color c2 = Theme.ACCENT2;

      boolean hovering = this.hoverTimer != null && this.hoverTimer.isRunning();

      if (hovering) {
         scale = 1.0 + 0.22 * Math.sin(this.hoverPhase * Math.PI);
         rotation = Math.sin(this.hoverPhase * Math.PI * 2) * 0.35;
         c1 = lerp(Theme.ACCENT, Theme.FG_GREEN, Math.sin(this.hoverPhase * Math.PI));
         c2 = lerp(Theme.ACCENT2, Color.WHITE, 0.3 * Math.sin(this.hoverPhase * Math.PI));
      } else if (this.pulsing) {
         scale = 1.0 + 0.09 * Math.sin(this.pulsePhase);
      }

      int w = this.getWidth();
      int h = this.getHeight();

      g2.translate(w / 2.0, h / 2.0);
      g2.rotate(rotation);
      g2.scale(scale, scale);
      g2.translate(-w / 2.0, -h / 2.0);

      g2.setColor(new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), 45));
      g2.fillOval(-3, -3, w + 6, h + 6);

      g2.setPaint(new GradientPaint(0, 0, c1, w, h, c2));
      g2.fillOval(0, 0, w, h);

      g2.setColor(Color.WHITE);
      g2.setStroke(new BasicStroke(Math.max(1.2f, w * 0.045f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

      g2.drawLine(w / 2, (int) (h * 0.07), w / 2, (int) (h * 0.24));

      int antW = Math.max(3, (int) (w * 0.16));
      g2.fillOval(w / 2 - antW / 2, (int) (h * 0.0), antW, antW);

      double eyeShrink = 1.0;

      if (this.blinkPhase >= 0.0 && !hovering && !this.pulsing) {
         eyeShrink = Math.max(0.12, 1.0 - 0.9 * Math.sin(this.blinkPhase * Math.PI));
      }

      int eyeW = Math.max(3, (int) (w * 0.17));
      int eyeHFull = Math.max(3, (int) (h * 0.24));
      int eyeH = Math.max(2, (int) (eyeHFull * eyeShrink));
      int eyeY = (int) (h * 0.42) + (eyeHFull - eyeH) / 2;
      int eyeLX = (int) (w * 0.26);
      int eyeRX = (int) (w * 0.57);

      g2.setColor(Color.WHITE);
      g2.fillRoundRect(eyeLX, eyeY, eyeW, eyeH, eyeW / 2, Math.max(1, eyeH / 2));
      g2.fillRoundRect(eyeRX, eyeY, eyeW, eyeH, eyeW / 2, Math.max(1, eyeH / 2));

      if (eyeShrink > 0.4) {
         Color pupil = new Color(35, 35, 60);
         int pupilSize = Math.max(2, (int) (eyeW * 0.42));

         g2.setColor(pupil);
         g2.fillOval(
               eyeLX + (eyeW - pupilSize) / 2,
               eyeY + Math.max(0, (eyeH - pupilSize) / 2),
               pupilSize,
               Math.min(pupilSize, Math.max(1, eyeH))
         );

         g2.fillOval(
               eyeRX + (eyeW - pupilSize) / 2,
               eyeY + Math.max(0, (eyeH - pupilSize) / 2),
               pupilSize,
               Math.min(pupilSize, Math.max(1, eyeH))
         );
      }

      g2.setColor(new Color(255, 255, 255, 215));
      g2.setStroke(new BasicStroke(Math.max(1.0f, w * 0.032f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

      int mouthY = (int) (h * 0.72);
      int mouthH = (int) (h * 0.09);

      for (int i = 0; i < 3; i++) {
         int mx = (int) (w * 0.39) + i * (int) (w * 0.11);
         g2.drawLine(mx, mouthY, mx, mouthY + mouthH);
      }

      g2.dispose();
   }
}