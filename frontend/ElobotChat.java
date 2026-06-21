// ELOBOT - Asistente Académico UTFSM
// Frontend Java Swing (mejorado). Backend / API / RAG intactos.
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class ElobotChat extends JFrame {

   // ---- Paleta de colores (mutable para soportar tema oscuro/claro) ----
   private static Color BG = new Color(10, 10, 22);
   private static Color BG_CHAT = new Color(14, 14, 28);
   private static Color BUBBLE_U = new Color(65, 95, 235);
   private static Color BUBBLE_B = new Color(24, 24, 50);
   private static final Color ACCENT = new Color(108, 78, 252);
   private static final Color ACCENT2 = new Color(65, 95, 235);
   private static Color FG = new Color(232, 232, 248);
   private static Color FG_MUTED = new Color(95, 95, 132);
   private static final Color FG_GREEN = new Color(0, 208, 125);
   private static final Color FG_RED = new Color(255, 99, 99);
   private static Color FIELD_BG = new Color(20, 20, 44);
   private static Color BORDER = new Color(36, 36, 68);
   private static Color SIDEBAR_BG = new Color(16, 16, 34);
   private static Color SIDEBAR_ITEM_HOVER = new Color(30, 30, 58);

   private static final Font F_UI = new Font("Segoe UI", Font.PLAIN, 13);
   private static final Font F_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
   private static final Font F_TINY = new Font("Segoe UI", Font.PLAIN, 10);
   private static final DateTimeFormatter TFMT = DateTimeFormatter.ofPattern("HH:mm");

   private static final String PLACEHOLDER_PREGUNTA = "Escribe tu pregunta...";
   private static final String PLACEHOLDER_CODIGO = "Ej: ELO-329";
   private static final String GREETING = "¡Hola! Soy ELOBOT.\nIngresa el código de tu asignatura (ej: ELO-329) y hazme una pregunta.\nSi dejas el campo vacío, buscaré en todos los programas.";

   // ---- Estado / componentes ----
   private JPanel chatPanel;
   private JScrollPane scrollPane;
   private JTextField inputField;
   private JTextField codigoField;
   private JButton sendButton;
   private JButton themeButton;
   private JLabel toastLabel;
   private Timer toastTimer;
   private JPanel typingRow;
   private Timer typingTimer;
   private AnimatedAvatar headerAvatar;
   private AnimatedAvatar typingAvatarRef;
   private JPanel historyListPanel;
   private Point dragStart;

   private boolean darkTheme = true;
   private List<ChatMessage> currentMessages = new ArrayList<>();
   private List<Conversation> conversationHistory = new ArrayList<>();
   private Conversation activeConversation = null;

   // ===================== MODELOS =====================

   private static class ChatMessage {
      String text;
      boolean user;
      String time;
   }

   private static class Conversation {
      String title;
      String codigo;
      List<ChatMessage> messages;
   }

   // ===================== ÍCONOS VECTORIALES (sin emojis, sin imágenes externas) =====================

   /** Icon reutilizable que delega el dibujo a drawIcon() y reacciona al estado hover del botón. */
   private static class VectorIcon implements Icon {
      private final String type;
      private final int size;

      VectorIcon(String type, int size) {
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
         Color color = c.getForeground() != null ? c.getForeground() : FG_MUTED;
         if (c instanceof AbstractButton && !Color.WHITE.equals(color)) {
            boolean hot = ((AbstractButton) c).getModel().isRollover();
            if (hot) {
               color = ACCENT2;
            }
         }
         Graphics2D g2 = (Graphics2D) g.create();
         g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         drawIcon(g2, this.type, x + this.size / 2f, y + this.size / 2f, this.size, color);
         g2.dispose();
      }
   }

   /**
    * Dibuja un ícono vectorial pequeño y reconocible (sin depender de fuentes de emoji).
    * (cx, cy) = centro; size = ancho/alto del cuadro contenedor.
    */
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
            int x1 = (int) (cx - h * 0.6f), y1 = (int) cy;
            int x2 = (int) (cx + h * 0.55f), y2 = (int) (cy - h * 0.55f);
            int x3 = (int) (cx + h * 0.55f), y3 = (int) (cy + h * 0.55f);
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

   public ElobotChat() {
      applyPalette(this.darkTheme);
      this.setUndecorated(true);
      this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      this.setSize(1040, 720);
      this.setMinimumSize(new Dimension(860, 560));
      this.setLocationRelativeTo((Component) null);
      rebuildContent();
      SwingUtilities.invokeLater(() -> this.addBotMessage(GREETING));
   }

   // ===================== TEMA =====================

   private void applyPalette(boolean dark) {
      if (dark) {
         BG = new Color(10, 10, 22);
         BG_CHAT = new Color(14, 14, 28);
         BUBBLE_U = new Color(65, 95, 235);
         BUBBLE_B = new Color(24, 24, 50);
         FG = new Color(232, 232, 248);
         FG_MUTED = new Color(95, 95, 132);
         FIELD_BG = new Color(20, 20, 44);
         BORDER = new Color(36, 36, 68);
         SIDEBAR_BG = new Color(16, 16, 34);
         SIDEBAR_ITEM_HOVER = new Color(30, 30, 58);
      } else {
         BG = new Color(246, 247, 251);
         BG_CHAT = new Color(255, 255, 255);
         BUBBLE_U = new Color(99, 124, 255);
         BUBBLE_B = new Color(232, 233, 241);
         FG = new Color(28, 28, 40);
         FG_MUTED = new Color(120, 120, 140);
         FIELD_BG = new Color(255, 255, 255);
         BORDER = new Color(221, 223, 233);
         SIDEBAR_BG = new Color(237, 238, 245);
         SIDEBAR_ITEM_HOVER = new Color(223, 225, 237);
      }
   }

   private void toggleTheme() {
      if (this.typingRow != null) {
         this.hideTyping();
      }
      this.darkTheme = !this.darkTheme;
      applyPalette(this.darkTheme);
      rebuildContent();
      showToast(this.darkTheme ? "✓ Tema oscuro activado" : "✓ Tema claro activado");
   }

   // ===================== CONSTRUCCIÓN DE LA INTERFAZ =====================

   private void rebuildContent() {
      JPanel root = new JPanel(new BorderLayout());
      root.setBackground(BG);
      root.setBorder(BorderFactory.createLineBorder(BORDER));

      root.add(this.buildSidebar(), BorderLayout.WEST);

      JPanel mainArea = new JPanel(new BorderLayout());
      mainArea.setBackground(BG);

      JPanel topStack = new JPanel();
      topStack.setLayout(new BoxLayout(topStack, BoxLayout.Y_AXIS));
      topStack.setBackground(BG);
      topStack.add(this.buildHeader());
      topStack.add(this.buildToolbarRow());
      topStack.add(this.buildQuickQuestionsRow());
      mainArea.add(topStack, BorderLayout.NORTH);

      mainArea.add(this.buildChatArea(), BorderLayout.CENTER);
      mainArea.add(this.buildInputPanel(), BorderLayout.SOUTH);

      root.add(mainArea, BorderLayout.CENTER);

      this.setContentPane(root);
      this.renderAllMessages();
      this.revalidate();
      this.repaint();
   }

   // ---- Sidebar estilo ChatGPT ----

   private JPanel buildSidebar() {
      JPanel sidebar = new JPanel(new BorderLayout());
      sidebar.setBackground(SIDEBAR_BG);
      sidebar.setPreferredSize(new Dimension(230, 0));
      sidebar.setBorder(new MatteBorder(0, 0, 0, 1, BORDER));

      JPanel top = new JPanel();
      top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
      top.setBackground(SIDEBAR_BG);
      top.setBorder(new EmptyBorder(18, 16, 12, 16));

      JPanel titleRow = new JPanel();
      titleRow.setLayout(new BoxLayout(titleRow, BoxLayout.X_AXIS));
      titleRow.setBackground(SIDEBAR_BG);
      titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
      JLabel classIcon = new JLabel(new VectorIcon("class", 16));
      classIcon.setForeground(ACCENT2);
      titleRow.add(classIcon);
      titleRow.add(Box.createRigidArea(new Dimension(7, 0)));
      JLabel title = new JLabel("Historial");
      title.setFont(new Font("Segoe UI", Font.BOLD, 15));
      title.setForeground(FG);
      titleRow.add(title);
      top.add(titleRow);
      top.add(Box.createRigidArea(new Dimension(0, 4)));
      JLabel subtitle = new JLabel("Conversaciones y ramos consultados");
      subtitle.setFont(F_TINY);
      subtitle.setForeground(FG_MUTED);
      subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
      top.add(subtitle);
      top.add(Box.createRigidArea(new Dimension(0, 14)));

      JButton newConvBtn = this.buildSidebarActionButton("Nueva conversación");
      newConvBtn.addActionListener(e -> this.startNewConversation());
      top.add(newConvBtn);

      sidebar.add(top, BorderLayout.NORTH);

      this.historyListPanel = new JPanel();
      this.historyListPanel.setLayout(new BoxLayout(this.historyListPanel, BoxLayout.Y_AXIS));
      this.historyListPanel.setBackground(SIDEBAR_BG);
      this.historyListPanel.setBorder(new EmptyBorder(2, 12, 12, 12));

      JScrollPane historyScroll = new JScrollPane(this.historyListPanel);
      historyScroll.setBorder((Border) null);
      historyScroll.setBackground(SIDEBAR_BG);
      historyScroll.getViewport().setBackground(SIDEBAR_BG);
      historyScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      historyScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      this.styleScrollBar(historyScroll.getVerticalScrollBar());
      sidebar.add(historyScroll, BorderLayout.CENTER);

      this.refreshHistoryList();
      return sidebar;
   }

   private void refreshHistoryList() {
      if (this.historyListPanel == null) {
         return;
      }
      this.historyListPanel.removeAll();
      if (this.conversationHistory.isEmpty()) {
         JLabel empty = new JLabel("Sin conversaciones guardadas");
         empty.setFont(F_TINY);
         empty.setForeground(FG_MUTED);
         empty.setBorder(new EmptyBorder(10, 4, 10, 4));
         empty.setAlignmentX(Component.LEFT_ALIGNMENT);
         this.historyListPanel.add(empty);
      } else {
         for (Conversation conv : this.conversationHistory) {
            this.historyListPanel.add(this.buildHistoryItem(conv));
            this.historyListPanel.add(Box.createRigidArea(new Dimension(0, 4)));
         }
      }
      this.historyListPanel.revalidate();
      this.historyListPanel.repaint();
   }

   private JPanel buildHistoryItem(final Conversation conv) {
      final boolean[] hover = {false};
      JPanel item = new JPanel() {
         protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean active = conv == ElobotChat.this.activeConversation;
            if (active) {
               g2.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 55));
               g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 10, 10);
            } else if (hover[0]) {
               g2.setColor(SIDEBAR_ITEM_HOVER);
               g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 10, 10);
            }
            g2.dispose();
         }
      };
      item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));
      item.setOpaque(false);
      item.setAlignmentX(Component.LEFT_ALIGNMENT);
      item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
      item.setBorder(new EmptyBorder(8, 10, 8, 10));
      item.setCursor(new Cursor(Cursor.HAND_CURSOR));

      JLabel lbl = new JLabel(conv.title);
      lbl.setFont(F_UI);
      lbl.setForeground(FG);
      lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
      JLabel sub = new JLabel((conv.codigo == null || conv.codigo.isEmpty()) ? "General" : conv.codigo);
      sub.setFont(F_TINY);
      sub.setForeground(FG_MUTED);
      sub.setAlignmentX(Component.LEFT_ALIGNMENT);
      item.add(lbl);
      item.add(sub);

      item.addMouseListener(new MouseAdapter() {
         public void mouseEntered(MouseEvent e) {
            hover[0] = true;
            item.repaint();
         }

         public void mouseExited(MouseEvent e) {
            hover[0] = false;
            item.repaint();
         }

         public void mouseClicked(MouseEvent e) {
            ElobotChat.this.loadConversation(conv);
         }
      });
      return item;
   }

   private JButton buildSidebarActionButton(String text) {
      JButton btn = new JButton(text) {
         protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean hot = this.getModel().isRollover();
            Color c1 = hot ? ACCENT2 : ACCENT;
            Color c2 = hot ? ACCENT : ACCENT2;
            g2.setPaint(new GradientPaint(0, 0, c1, this.getWidth(), 0, c2));
            g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
         }
      };
      btn.setIcon(new VectorIcon("plus", 12));
      btn.setIconTextGap(8);
      btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
      btn.setForeground(Color.WHITE);
      btn.setFocusPainted(false);
      btn.setBorderPainted(false);
      btn.setContentAreaFilled(false);
      btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
      btn.setAlignmentX(Component.LEFT_ALIGNMENT);
      btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
      btn.setBorder(new EmptyBorder(8, 10, 8, 10));
      return btn;
   }

   private void startNewConversation() {
      this.archiveCurrentIfNeeded();
      this.currentMessages = new ArrayList<>();
      this.activeConversation = null;
      this.chatPanel.removeAll();
      this.chatPanel.revalidate();
      this.chatPanel.repaint();
      this.codigoField.setText(PLACEHOLDER_CODIGO);
      this.codigoField.setForeground(FG_MUTED);
      this.addBotMessage(GREETING);
      this.refreshHistoryList();
   }

   private void archiveCurrentIfNeeded() {
      boolean hasUserMsg = false;
      for (ChatMessage m : this.currentMessages) {
         if (m.user) {
            hasUserMsg = true;
            break;
         }
      }
      if (!hasUserMsg || this.activeConversation != null) {
         return;
      }
      Conversation conv = new Conversation();
      conv.codigo = this.normalizeCodigoOrEmpty(this.codigoField.getText());
      String title = "Conversación";
      for (ChatMessage m : this.currentMessages) {
         if (m.user) {
            title = m.text.length() > 34 ? m.text.substring(0, 34) + "…" : m.text;
            break;
         }
      }
      conv.title = title;
      conv.messages = this.currentMessages;
      this.conversationHistory.add(0, conv);
   }

   private void loadConversation(Conversation conv) {
      this.archiveCurrentIfNeeded();
      this.activeConversation = conv;
      this.currentMessages = conv.messages;
      if (conv.codigo != null && !conv.codigo.isEmpty()) {
         this.codigoField.setText(conv.codigo);
         this.codigoField.setForeground(FG);
      } else {
         this.codigoField.setText(PLACEHOLDER_CODIGO);
         this.codigoField.setForeground(FG_MUTED);
      }
      this.renderAllMessages();
      this.refreshHistoryList();
   }

   // ---- Header (arrastrable, avatar animado, controles ventana) ----

   private JPanel buildHeader() {
      JPanel headerRow = new JPanel(new BorderLayout());
      headerRow.setBackground(BG);
      headerRow.setBorder(new EmptyBorder(14, 20, 14, 20));
      MouseAdapter dragHandler = new MouseAdapter() {
         public void mousePressed(MouseEvent e) {
            ElobotChat.this.dragStart = e.getPoint();
         }

         public void mouseDragged(MouseEvent e) {
            if (ElobotChat.this.dragStart != null) {
               Point p = e.getLocationOnScreen();
               ElobotChat.this.setLocation(p.x - ElobotChat.this.dragStart.x, p.y - ElobotChat.this.dragStart.y);
            }
         }
      };
      headerRow.addMouseListener(dragHandler);
      headerRow.addMouseMotionListener(dragHandler);
      headerRow.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

      JPanel left = new JPanel();
      left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
      left.setBackground(BG);
      this.headerAvatar = new AnimatedAvatar(46);
      left.add(this.headerAvatar);
      left.add(Box.createRigidArea(new Dimension(12, 0)));
      JPanel titles = new JPanel();
      titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
      titles.setBackground(BG);
      JLabel name = new JLabel("ELOBOT");
      name.setFont(new Font("Segoe UI", Font.BOLD, 18));
      name.setForeground(FG);
      JLabel sub = new JLabel("Asistente Académico · UTFSM");
      sub.setFont(F_SMALL);
      sub.setForeground(FG_MUTED);
      titles.add(name);
      titles.add(sub);
      left.add(titles);

      JPanel right = new JPanel();
      right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
      right.setBackground(BG);
      JLabel online = new JLabel("● En línea");
      online.setFont(F_SMALL);
      online.setForeground(FG_GREEN);
      JButton minBtn = this.buildWinBtn(new Color(255, 189, 46), false);
      JButton closeBtn = this.buildWinBtn(new Color(255, 95, 87), true);
      minBtn.addActionListener(e -> this.setState(JFrame.ICONIFIED));
      closeBtn.addActionListener(e -> System.exit(0));
      right.add(online);
      right.add(Box.createRigidArea(new Dimension(18, 0)));
      right.add(minBtn);
      right.add(Box.createRigidArea(new Dimension(8, 0)));
      right.add(closeBtn);

      headerRow.add(left, BorderLayout.WEST);
      headerRow.add(right, BorderLayout.EAST);

      JPanel wrapper = new JPanel(new BorderLayout());
      wrapper.setBackground(BG);
      wrapper.add(headerRow, BorderLayout.CENTER);
      wrapper.add(this.buildGradSep(), BorderLayout.SOUTH);
      return wrapper;
   }

   /** Avatar robot vectorial: rebote/giro/cambio de color al pasar el mouse, pulso mientras el bot piensa, y parpadeo ambiental. */
   private static class AnimatedAvatar extends JLabel {
      private double hoverPhase = 0.0;
      private Timer hoverTimer;
      private boolean pulsing = false;
      private double pulsePhase = 0.0;
      private Timer pulseTimer;
      private double blinkPhase = -1.0;
      private Timer blinkTimer;
      private final Timer ambientTimer;
      private final int size;

      AnimatedAvatar(int size) {
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

      void setPulsing(boolean p) {
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
         Color c1 = ElobotChat.ACCENT;
         Color c2 = ElobotChat.ACCENT2;
         boolean hovering = this.hoverTimer != null && this.hoverTimer.isRunning();
         if (hovering) {
            scale = 1.0 + 0.22 * Math.sin(this.hoverPhase * Math.PI);
            rotation = Math.sin(this.hoverPhase * Math.PI * 2) * 0.35;
            c1 = lerp(ElobotChat.ACCENT, ElobotChat.FG_GREEN, Math.sin(this.hoverPhase * Math.PI));
            c2 = lerp(ElobotChat.ACCENT2, Color.WHITE, 0.3 * Math.sin(this.hoverPhase * Math.PI));
         } else if (this.pulsing) {
            scale = 1.0 + 0.09 * Math.sin(this.pulsePhase);
         }
         int w = this.getWidth();
         int h = this.getHeight();
         g2.translate(w / 2.0, h / 2.0);
         g2.rotate(rotation);
         g2.scale(scale, scale);
         g2.translate(-w / 2.0, -h / 2.0);

         // Halo + cabeza del robot (círculo con degradado, coherente con la paleta de la app)
         g2.setColor(new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), 45));
         g2.fillOval(-3, -3, w + 6, h + 6);
         g2.setPaint(new GradientPaint(0, 0, c1, w, h, c2));
         g2.fillOval(0, 0, w, h);

         // Antena
         g2.setColor(Color.WHITE);
         g2.setStroke(new BasicStroke(Math.max(1.2f, w * 0.045f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
         g2.drawLine(w / 2, (int) (h * 0.07), w / 2, (int) (h * 0.24));
         int antW = Math.max(3, (int) (w * 0.16));
         g2.fillOval(w / 2 - antW / 2, (int) (h * 0.0), antW, antW);

         // Ojos (rectángulos redondeados tipo "pantalla"), con parpadeo
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
            g2.fillOval(eyeLX + (eyeW - pupilSize) / 2, eyeY + Math.max(0, (eyeH - pupilSize) / 2), pupilSize, Math.min(pupilSize, Math.max(1, eyeH)));
            g2.fillOval(eyeRX + (eyeW - pupilSize) / 2, eyeY + Math.max(0, (eyeH - pupilSize) / 2), pupilSize, Math.min(pupilSize, Math.max(1, eyeH)));
         }

         // Boca (rejilla tipo altavoz)
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

   private JPanel buildGradSep() {
      JPanel sep = new JPanel() {
         protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0.0F, 0.0F, ACCENT, (float) this.getWidth(), 0.0F, ACCENT2));
            g2.fillRect(0, 0, this.getWidth(), this.getHeight());
            g2.dispose();
         }
      };
      sep.setPreferredSize(new Dimension(1, 2));
      sep.setOpaque(false);
      return sep;
   }

   // ===================== TOOLBAR SUPERIOR =====================

   private JPanel buildToolbarRow() {
      JPanel row = new JPanel(new BorderLayout());
      row.setBackground(BG);
      row.setBorder(new EmptyBorder(10, 20, 6, 20));

      JLabel label = new JLabel("Acciones rápidas");
      label.setFont(F_TINY);
      label.setForeground(FG_MUTED);
      row.add(label, BorderLayout.WEST);

      JPanel right = new JPanel();
      right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
      right.setBackground(BG);
      right.add(this.buildIconButton("save", "Guardar conversación (.txt)", this::saveConversationToTxt));
      right.add(Box.createRigidArea(new Dimension(8, 0)));
      right.add(this.buildIconButton("trash", "Limpiar chat actual", this::clearCurrentChat));
      right.add(Box.createRigidArea(new Dimension(8, 0)));
      this.themeButton = this.buildIconButton(this.darkTheme ? "moon" : "sun", "Cambiar tema claro/oscuro", this::toggleTheme);
      right.add(this.themeButton);
      right.add(Box.createRigidArea(new Dimension(8, 0)));
      right.add(this.buildIconButton("share", "Compartir / copiar conversación", this::shareConversation));
      row.add(right, BorderLayout.EAST);
      return row;
   }

   private JButton buildIconButton(String iconType, String tooltip, Runnable action) {
      JButton btn = new JButton() {
         protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color bgc = this.getModel().isPressed() ? new Color(BORDER.getRed(), BORDER.getGreen(), BORDER.getBlue())
                  : (this.getModel().isRollover() ? new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 50) : FIELD_BG);
            g2.setColor(bgc);
            g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 12, 12);
            g2.setColor(BORDER);
            g2.setStroke(new BasicStroke(1.0F));
            g2.drawRoundRect(0, 0, this.getWidth() - 1, this.getHeight() - 1, 12, 12);
            g2.dispose();
            super.paintComponent(g);
         }
      };
      btn.setIcon(new VectorIcon(iconType, 16));
      btn.setForeground(FG_MUTED);
      btn.setFocusPainted(false);
      btn.setBorderPainted(false);
      btn.setContentAreaFilled(false);
      btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
      btn.setToolTipText(tooltip);
      Dimension d = new Dimension(42, 36);
      btn.setPreferredSize(d);
      btn.setMaximumSize(d);
      btn.setMinimumSize(d);
      btn.addActionListener(e -> action.run());
      return btn;
   }

   private void saveConversationToTxt() {
      JFileChooser chooser = new JFileChooser();
      chooser.setDialogTitle("Guardar conversación");
      chooser.setFileFilter(new FileNameExtensionFilter("Archivo de texto", "txt"));
      chooser.setSelectedFile(new File("conversacion_elobot.txt"));
      if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
         File file = chooser.getSelectedFile();
         if (!file.getName().toLowerCase().endsWith(".txt")) {
            file = new File(file.getParentFile(), file.getName() + ".txt");
         }
         try (FileWriter fw = new FileWriter(file)) {
            fw.write(this.buildTranscript());
            this.showToast("✓ Conversación guardada");
         } catch (Exception ex) {
            this.showToast("✕ No se pudo guardar el archivo");
         }
      }
   }

   private void clearCurrentChat() {
      this.currentMessages = new ArrayList<>();
      this.activeConversation = null;
      this.chatPanel.removeAll();
      this.chatPanel.revalidate();
      this.chatPanel.repaint();
      this.addBotMessage(GREETING);
      this.refreshHistoryList();
      this.showToast("✓ Chat limpiado");
   }

   private void shareConversation() {
      this.copyToClipboard(this.buildTranscript());
      this.showToast("✓ Conversación copiada al portapapeles");
   }

   private String buildTranscript() {
      StringBuilder sb = new StringBuilder();
      sb.append("Conversación ELOBOT\n");
      sb.append("=====================\n\n");
      for (ChatMessage m : this.currentMessages) {
         sb.append("[").append(m.time).append("] ").append(m.user ? "Tú" : "ELOBOT").append(":\n");
         sb.append(m.text).append("\n\n");
      }
      return sb.toString();
   }

   private void copyToClipboard(String text) {
      StringSelection sel = new StringSelection(text);
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
   }

   private void showToast(String msg) {
      if (this.toastLabel == null) {
         return;
      }
      this.toastLabel.setText(msg);
      this.toastLabel.setForeground(msg.startsWith("✕") ? FG_RED : FG_GREEN);
      if (this.toastTimer != null) {
         this.toastTimer.stop();
      }
      this.toastTimer = new Timer(2400, e -> this.toastLabel.setText(" "));
      this.toastTimer.setRepeats(false);
      this.toastTimer.start();
   }

   // ===================== PREGUNTAS RÁPIDAS =====================

   private JPanel buildQuickQuestionsRow() {
      JPanel row = new JPanel();
      row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
      row.setBackground(BG);
      row.setBorder(new EmptyBorder(4, 20, 14, 20));

      String[] tipos = {"Requisitos", "Contenidos", "Evaluaciones", "Bibliografía"};
      String[] iconos = {"key", "list", "check", "book"};
      for (int i = 0; i < tipos.length; i++) {
         final String tipo = tipos[i];
         JButton chip = this.buildChipButton(iconos[i], tipo);
         chip.addActionListener(e -> this.handleQuickQuestion(tipo));
         row.add(chip);
         row.add(Box.createRigidArea(new Dimension(8, 0)));
      }
      row.add(Box.createHorizontalGlue());
      return row;
   }

   private JButton buildChipButton(String iconType, String text) {
      JButton btn = new JButton(text) {
         protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean hot = this.getModel().isRollover();
            g2.setColor(hot ? new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 40) : FIELD_BG);
            g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 16, 16);
            g2.setColor(hot ? ACCENT2 : BORDER);
            g2.setStroke(new BasicStroke(1.2F));
            g2.drawRoundRect(0, 0, this.getWidth() - 1, this.getHeight() - 1, 16, 16);
            g2.dispose();
            super.paintComponent(g);
         }
      };
      btn.setIcon(new VectorIcon(iconType, 14));
      btn.setIconTextGap(8);
      btn.setFont(F_SMALL);
      btn.setForeground(FG);
      btn.setFocusPainted(false);
      btn.setBorderPainted(false);
      btn.setContentAreaFilled(false);
      btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
      btn.setBorder(new EmptyBorder(7, 14, 7, 14));
      return btn;
   }

   private void handleQuickQuestion(String tipo) {
      String codigoRaw = this.codigoField.getText().trim();
      String codigo = "";
      if (!codigoRaw.isEmpty() && !codigoRaw.equals(PLACEHOLDER_CODIGO)) {
         codigo = this.normalizeCodigo(codigoRaw);
         this.codigoField.setText(codigo);
         this.codigoField.setForeground(FG);
      }
      String pregunta;
      switch (tipo) {
         case "Requisitos":
            pregunta = codigo.isEmpty() ? "¿Cuáles son los requisitos del ramo?" : "¿Cuáles son los requisitos de " + codigo + "?";
            break;
         case "Contenidos":
            pregunta = codigo.isEmpty() ? "¿Cuáles son los contenidos del ramo?" : "¿Cuáles son los contenidos de " + codigo + "?";
            break;
         case "Evaluaciones":
            pregunta = codigo.isEmpty() ? "¿Cómo son las evaluaciones del ramo?" : "¿Cómo son las evaluaciones de " + codigo + "?";
            break;
         case "Bibliografía":
            pregunta = codigo.isEmpty() ? "¿Cuál es la bibliografía recomendada?" : "¿Cuál es la bibliografía recomendada de " + codigo + "?";
            break;
         default:
            pregunta = "";
      }
      if (!pregunta.isEmpty()) {
         this.inputField.setText(pregunta);
         this.inputField.setForeground(FG);
         this.sendMessage();
      }
   }

   // ===================== ÁREA DE CHAT =====================

   private JScrollPane buildChatArea() {
      this.chatPanel = new JPanel();
      this.chatPanel.setLayout(new BoxLayout(this.chatPanel, BoxLayout.Y_AXIS));
      this.chatPanel.setBackground(BG_CHAT);
      this.chatPanel.setBorder(new EmptyBorder(20, 18, 20, 18));
      this.scrollPane = new JScrollPane(this.chatPanel);
      this.scrollPane.setBackground(BG_CHAT);
      this.scrollPane.getViewport().setBackground(BG_CHAT);
      this.scrollPane.setBorder((Border) null);
      this.scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
      this.scrollPane.getVerticalScrollBar().setUnitIncrement(16);
      this.styleScrollBar(this.scrollPane.getVerticalScrollBar());
      return this.scrollPane;
   }

   private void renderAllMessages() {
      if (this.chatPanel == null) {
         return;
      }
      this.chatPanel.removeAll();
      for (ChatMessage m : this.currentMessages) {
         this.renderMessageRow(m);
      }
      this.scrollToBottom();
   }

   private void appendMessage(String text, boolean isUser) {
      ChatMessage m = new ChatMessage();
      m.text = text;
      m.user = isUser;
      m.time = LocalTime.now().format(TFMT);
      this.currentMessages.add(m);
      this.renderMessageRow(m);
      this.scrollToBottom();
   }

   private void addBotMessage(String text) {
      this.appendMessage(text, false);
   }

   private void addUserMessage(String text) {
      this.appendMessage(text, true);
   }

   private void renderMessageRow(ChatMessage m) {
      JPanel row = new JPanel();
      row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
      row.setOpaque(false);
      row.setAlignmentX(0.0F);
      if (m.user) {
         row.add(Box.createHorizontalGlue());
         row.add(this.msgColumn(m, true));
      } else {
         AnimatedAvatar avatar = new AnimatedAvatar(30);
         avatar.setAlignmentY(0.0F);
         JPanel col = this.msgColumn(m, false);
         col.setAlignmentY(0.0F);
         row.add(avatar);
         row.add(Box.createRigidArea(new Dimension(8, 0)));
         row.add(col);
         row.add(Box.createHorizontalGlue());
      }
      this.chatPanel.add(row);
      this.chatPanel.add(Box.createRigidArea(new Dimension(0, 10)));
   }

   private JPanel msgColumn(ChatMessage m, boolean isUser) {
      JPanel col = new JPanel();
      col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
      col.setOpaque(false);
      col.setAlignmentY(0.0F);
      col.add(this.buildBubble(m.text, isUser));
      JLabel time = new JLabel(m.time);
      time.setFont(F_TINY);
      time.setForeground(FG_MUTED);
      time.setBorder(new EmptyBorder(3, isUser ? 0 : 4, 0, 0));
      time.setAlignmentX(isUser ? 1.0F : 0.0F);
      col.add(time);
      if (!isUser) {
         JPanel actions = new JPanel();
         actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));
         actions.setOpaque(false);
         actions.setBorder(new EmptyBorder(4, 4, 0, 0));
         actions.setAlignmentX(0.0F);
         final String text = m.text;
         JButton copyBtn = this.buildActionLink("copy", "Copiar respuesta", () -> {
            this.copyToClipboard(text);
            this.showToast("✓ Respuesta copiada");
         });
         JButton shareBtn = this.buildActionLink("share", "Compartir", () -> {
            this.copyToClipboard(text);
            this.showToast("✓ Lista para compartir (copiada)");
         });
         actions.add(copyBtn);
         actions.add(Box.createRigidArea(new Dimension(10, 0)));
         actions.add(shareBtn);
         col.add(actions);
      }
      return col;
   }

   private JButton buildActionLink(String iconType, String text, Runnable action) {
      JButton btn = new JButton(text);
      btn.setIcon(new VectorIcon(iconType, 12));
      btn.setIconTextGap(5);
      btn.setFont(F_TINY);
      btn.setForeground(FG_MUTED);
      btn.setBorderPainted(false);
      btn.setContentAreaFilled(false);
      btn.setFocusPainted(false);
      btn.setMargin(new java.awt.Insets(2, 2, 2, 2));
      btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
      btn.addMouseListener(new MouseAdapter() {
         public void mouseEntered(MouseEvent e) {
            btn.setForeground(ACCENT2);
            btn.repaint();
         }

         public void mouseExited(MouseEvent e) {
            btn.setForeground(FG_MUTED);
            btn.repaint();
         }
      });
      btn.addActionListener(e -> action.run());
      return btn;
   }

   private void showTyping() {
      this.typingRow = new JPanel();
      this.typingRow.setLayout(new BoxLayout(this.typingRow, BoxLayout.X_AXIS));
      this.typingRow.setBackground(BG_CHAT);
      this.typingRow.setAlignmentX(0.0F);
      final JLabel[] dots = new JLabel[3];
      JPanel dotsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 10)) {
         protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BUBBLE_B);
            g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 18, 18);
            g2.dispose();
         }
      };
      dotsPanel.setOpaque(false);
      for (int i = 0; i < 3; ++i) {
         dots[i] = new JLabel("●");
         dots[i].setFont(new Font("Segoe UI", Font.BOLD, 12));
         dots[i].setForeground(i == 0 ? FG : FG_MUTED);
         dotsPanel.add(dots[i]);
      }
      final int[] idx = {0};
      this.typingTimer = new Timer(380, e -> {
         idx[0] = (idx[0] + 1) % 3;
         for (int i = 0; i < 3; ++i) {
            dots[i].setForeground(i == idx[0] ? FG : FG_MUTED);
         }
      });
      this.typingTimer.start();

      this.typingAvatarRef = new AnimatedAvatar(30);
      this.typingAvatarRef.setAlignmentY(0.0F);
      this.typingAvatarRef.setPulsing(true);
      if (this.headerAvatar != null) {
         this.headerAvatar.setPulsing(true);
      }

      this.typingRow.add(this.typingAvatarRef);
      this.typingRow.add(Box.createRigidArea(new Dimension(8, 0)));
      this.typingRow.add(dotsPanel);
      this.typingRow.add(Box.createHorizontalGlue());
      this.chatPanel.add(this.typingRow);
      this.chatPanel.add(Box.createRigidArea(new Dimension(0, 10)));
      this.scrollToBottom();
   }

   private void hideTyping() {
      if (this.typingTimer != null) {
         this.typingTimer.stop();
         this.typingTimer = null;
      }
      if (this.headerAvatar != null) {
         this.headerAvatar.setPulsing(false);
      }
      if (this.typingAvatarRef != null) {
         this.typingAvatarRef.setPulsing(false);
         this.typingAvatarRef = null;
      }
      if (this.typingRow != null) {
         for (int i = 0; i < this.chatPanel.getComponentCount(); ++i) {
            if (this.chatPanel.getComponent(i) == this.typingRow) {
               if (i + 1 < this.chatPanel.getComponentCount()) {
                  this.chatPanel.remove(i + 1);
               }
               this.chatPanel.remove(i);
               break;
            }
         }
         this.typingRow = null;
         this.chatPanel.revalidate();
         this.chatPanel.repaint();
      }
   }

   private JPanel buildBubble(String text, boolean isUser) {
      final Color bubbleColor = isUser ? BUBBLE_U : BUBBLE_B;
      JPanel bubble = new JPanel() {
         protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bubbleColor);
            g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 18, 18);
            g2.dispose();
         }
      };
      bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
      bubble.setOpaque(false);
      bubble.setBorder(new EmptyBorder(10, 14, 10, 14));
      bubble.setMaximumSize(new Dimension(520, Integer.MAX_VALUE));
      bubble.setAlignmentX(isUser ? 1.0F : 0.0F);
      JTextArea area = new JTextArea(text);
      area.setEditable(false);
      area.setLineWrap(true);
      area.setWrapStyleWord(true);
      area.setOpaque(false);
      area.setForeground(isUser ? Color.WHITE : FG);
      area.setFont(F_UI);
      area.setBorder((Border) null);
      area.setColumns(30);
      area.setAlignmentX(0.0F);
      bubble.add(area);
      return bubble;
   }

   private void scrollToBottom() {
      this.chatPanel.revalidate();
      this.chatPanel.repaint();
      SwingUtilities.invokeLater(() -> this.scrollPane.getVerticalScrollBar().setValue(this.scrollPane.getVerticalScrollBar().getMaximum()));
   }

   // ===================== PANEL DE ENTRADA =====================

   private JPanel buildInputPanel() {
      JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
      panel.setBackground(BG);
      panel.setBorder(new EmptyBorder(10, 18, 18, 18));

      this.toastLabel = new JLabel(" ");
      this.toastLabel.setFont(F_TINY);
      this.toastLabel.setForeground(FG_GREEN);
      this.toastLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
      panel.add(this.toastLabel);
      panel.add(Box.createRigidArea(new Dimension(0, 6)));
      panel.add(this.buildGradSep());
      panel.add(Box.createRigidArea(new Dimension(0, 14)));

      JPanel codigoRow = new JPanel();
      codigoRow.setLayout(new BoxLayout(codigoRow, BoxLayout.X_AXIS));
      codigoRow.setBackground(BG);
      codigoRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
      JLabel codigoLbl = new JLabel("Código del ramo:");
      codigoLbl.setFont(F_SMALL);
      codigoLbl.setForeground(FG_MUTED);
      this.codigoField = this.buildStyledField(PLACEHOLDER_CODIGO);
      this.codigoField.setHorizontalAlignment(JTextField.CENTER);
      this.codigoField.setMaximumSize(new Dimension(160, 38));
      this.codigoField.setPreferredSize(new Dimension(160, 38));
      this.codigoField.addFocusListener(new FocusAdapter() {
         public void focusLost(FocusEvent e) {
            String txt = ElobotChat.this.codigoField.getText().trim();
            if (!txt.isEmpty() && !txt.equals(PLACEHOLDER_CODIGO)) {
               ElobotChat.this.codigoField.setText(ElobotChat.this.normalizeCodigo(txt));
            }
         }
      });
      codigoRow.add(codigoLbl);
      codigoRow.add(Box.createRigidArea(new Dimension(10, 0)));
      codigoRow.add(this.codigoField);
      codigoRow.add(Box.createHorizontalGlue());
      panel.add(codigoRow);
      panel.add(Box.createRigidArea(new Dimension(0, 10)));

      JPanel inputRow = new JPanel();
      inputRow.setLayout(new BoxLayout(inputRow, BoxLayout.X_AXIS));
      inputRow.setBackground(BG);
      inputRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
      this.inputField = this.buildStyledField(PLACEHOLDER_PREGUNTA);
      this.inputField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
      this.sendButton = this.buildSendButton();
      this.sendButton.setMaximumSize(new Dimension(52, 44));
      this.sendButton.setPreferredSize(new Dimension(52, 44));
      JButton uploadBtn = this.buildUploadButton();
      uploadBtn.setMaximumSize(new Dimension(44, 44));
      uploadBtn.setPreferredSize(new Dimension(44, 44));
      inputRow.add(uploadBtn);
      inputRow.add(Box.createRigidArea(new Dimension(8, 0)));
      inputRow.add(this.inputField);
      inputRow.add(Box.createRigidArea(new Dimension(8, 0)));
      inputRow.add(this.sendButton);
      panel.add(inputRow);

      ActionListener sendAction = e -> this.sendMessage();
      this.sendButton.addActionListener(sendAction);
      this.inputField.addActionListener(sendAction);
      return panel;
   }

   private JTextField buildStyledField(final String placeholder) {
      final JTextField field = new JTextField() {
         protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (Boolean.TRUE.equals(this.getClientProperty("focused"))) {
               g2.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 40));
               g2.fillRoundRect(-2, -2, this.getWidth() + 4, this.getHeight() + 4, 16, 16);
            }
            g2.setColor(FIELD_BG);
            g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 12, 12);
            super.paintComponent(g);
            g2.dispose();
         }
      };
      field.addFocusListener(new FocusAdapter() {
         public void focusGained(FocusEvent e) {
            field.putClientProperty("focused", Boolean.TRUE);
            field.repaint();
         }

         public void focusLost(FocusEvent e) {
            field.putClientProperty("focused", Boolean.FALSE);
            field.repaint();
         }
      });
      field.setOpaque(false);
      field.setCaretColor(FG);
      field.setFont(F_UI);
      field.setBorder(new EmptyBorder(8, 14, 8, 14));
      field.setText(placeholder);
      field.setForeground(FG_MUTED);
      field.addFocusListener(new FocusAdapter() {
         public void focusGained(FocusEvent e) {

            if (field.getText().equals(placeholder)) {
               field.setText("");
               field.setForeground(FG);
            }
         }

         public void focusLost(FocusEvent e) {
            if (field.getText().isEmpty()) {
               field.setText(placeholder);
               field.setForeground(FG_MUTED);
            }
         }
      });
      return field;
   }

   private JButton buildSendButton() {
      JButton btn = new JButton() {
         protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = !this.getModel().isEnabled() ? new Color(35, 35, 60)
                  : (this.getModel().isPressed() ? ACCENT.darker() : (this.getModel().isRollover() ? new Color(122, 90, 255) : ACCENT));
            g2.setColor(c);
            g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 12, 12);
            g2.setColor(this.getModel().isEnabled() ? Color.WHITE : new Color(60, 60, 90));
            g2.setStroke(new BasicStroke(2.0F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = this.getWidth() / 2;
            int cy = this.getHeight() / 2;
            g2.drawLine(cx - 8, cy, cx + 3, cy);
            int[] xs = {cx, cx + 8, cx};
            int[] ys = {cy - 5, cy, cy + 5};
            g2.fillPolygon(xs, ys, 3);
            g2.dispose();
         }
      };
      btn.setBorderPainted(false);
      btn.setContentAreaFilled(false);
      btn.setFocusPainted(false);
      btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
      return btn;
   }

   private void sendMessage() {
      String pregunta = this.inputField.getText().trim();
      if (!pregunta.isEmpty() && !pregunta.equals(PLACEHOLDER_PREGUNTA)) {
         String codigoRaw = this.codigoField.getText().trim();
         String codigo;
         if (codigoRaw.isEmpty() || codigoRaw.equals(PLACEHOLDER_CODIGO)) {
            codigo = "";
         } else {
            codigo = this.normalizeCodigo(codigoRaw);
            if (!codigo.equals(codigoRaw)) {
               this.codigoField.setText(codigo);
            }
         }

         this.addUserMessage(pregunta);
         this.inputField.setText("");
         this.inputField.setForeground(FG);
         this.sendButton.setEnabled(false);
         this.showTyping();

         final String preguntaFinal = pregunta;
         final String codigoFinal = codigo;
         new Thread(() -> {
            String respuesta = this.callApi(preguntaFinal, codigoFinal);
            SwingUtilities.invokeLater(() -> {
               this.hideTyping();
               this.addBotMessage(respuesta);
               this.sendButton.setEnabled(true);
            });
         }).start();
      }
   }

   // ===================== NORMALIZACIÓN DE CÓDIGO DE RAMO =====================

   /**
    * Convierte códigos como "elo329" o "ELO329" al formato estándar "ELO-329".
    * Si el código ya viene en formato correcto (o con guión/espacio), también se normaliza.
    */
   private String normalizeCodigo(String input) {
      if (input == null) {
         return "";
      }
      String s = input.trim();
      if (s.isEmpty()) {
         return s;
      }
      Matcher m = Pattern.compile("^([A-Za-zÁÉÍÓÚáéíóúÑñ]+)[\\s_-]*([0-9]{2,4}[A-Za-z]?)$").matcher(s);
      if (m.matches()) {
         return m.group(1).toUpperCase() + "-" + m.group(2).toUpperCase();
      }
      return s.toUpperCase();
   }

   private String normalizeCodigoOrEmpty(String raw) {
      if (raw == null) {
         return "";
      }
      String s = raw.trim();
      if (s.isEmpty() || s.equals(PLACEHOLDER_CODIGO)) {
         return "";
      }
      return this.normalizeCodigo(s);
   }

   // ===================== API (INTACTO) =====================

   private String callApi(String pregunta, String codigoRamo) {
      try {
         String preguntaEsc = this.esc(pregunta);
         String body = "{\"pregunta\":\"" + preguntaEsc + "\",\"codigo_ramo\":\"" + this.esc(codigoRamo) + "\"}";
         HttpClient client = HttpClient.newBuilder().version(Version.HTTP_1_1).build();
         HttpResponse<String> response = client.send(
               HttpRequest.newBuilder()
                     .uri(URI.create("http://localhost:8001/consultar"))
                     .header("Content-Type", "application/json")
                     .POST(BodyPublishers.ofString(body))
                     .build(),
               BodyHandlers.ofString());
         return this.parseRespuesta(response.body());
      } catch (Exception ex) {
         return "✕ No se pudo conectar con el servidor.\n\nAsegúrate de que la API esté corriendo con:\n    uvicorn api:app --reload --port 8001";
      }
   }

   private String parseRespuesta(String json) {
      int idx = json.indexOf("\"respuesta\"");
      if (idx < 0) {
         return "Error: respuesta inesperada del servidor.";
      } else {
         int start = json.indexOf("\"", json.indexOf(":", idx)) + 1;
         StringBuilder sb = new StringBuilder();
         for (int i = start; i < json.length(); ++i) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
               ++i;
               char next = json.charAt(i);
               switch (next) {
                  case '"':
                     sb.append('"');
                     break;
                  case '\\':
                     sb.append('\\');
                     break;
                  case 'n':
                     sb.append('\n');
                     break;
                  case 't':
                     sb.append('\t');
                     break;
                  default:
                     sb.append(next);
               }
            } else {
               if (c == '"') {
                  break;
               }
               sb.append(c);
            }
         }
         return sb.toString();
      }
   }

   private String esc(String s) {
      return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
   }

   // ===================== CONTROLES DE VENTANA / ADJUNTOS =====================

   private JButton buildWinBtn(final Color hoverColor, final boolean isClose) {
      JButton btn = new JButton() {
         protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(this.getModel().isRollover() ? hoverColor : new Color(45, 45, 72));
            g2.fillOval(0, 0, this.getWidth(), this.getHeight());
            if (this.getModel().isRollover()) {
               g2.setColor(new Color(0, 0, 0, 90));
               g2.setStroke(new BasicStroke(1.5F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
               int q = this.getWidth() / 4;
               if (isClose) {
                  g2.drawLine(q, q, this.getWidth() - q, this.getHeight() - q);
                  g2.drawLine(this.getWidth() - q, q, q, this.getHeight() - q);
               } else {
                  g2.drawLine(q, this.getHeight() / 2, this.getWidth() - q, this.getHeight() / 2);
               }
            }
            g2.dispose();
         }
      };
      Dimension d = new Dimension(16, 16);
      btn.setPreferredSize(d);
      btn.setMaximumSize(d);
      btn.setBorderPainted(false);
      btn.setContentAreaFilled(false);
      btn.setFocusPainted(false);
      btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
      return btn;
   }

   private JButton buildUploadButton() {
      JButton btn = new JButton() {
         protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(this.getModel().isPressed() ? new Color(32, 32, 58) : (this.getModel().isRollover() ? new Color(28, 28, 52) : FIELD_BG));
            g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 12, 12);
            g2.setColor(FG_MUTED);
            g2.setStroke(new BasicStroke(1.8F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = this.getWidth() / 2;
            int cy = this.getHeight() / 2;
            g2.drawLine(cx - 7, cy + 8, cx + 7, cy + 8);
            g2.drawLine(cx, cy + 5, cx, cy - 4);
            int[] xs = {cx - 5, cx, cx + 5};
            int[] ys = {cy - 1, cy - 7, cy - 1};
            g2.fillPolygon(xs, ys, 3);
            g2.dispose();
         }
      };
      btn.setBorderPainted(false);
      btn.setContentAreaFilled(false);
      btn.setFocusPainted(false);
      btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
      btn.setToolTipText("Subir archivo .txt");
      btn.addActionListener(e -> this.uploadTxt());
      return btn;
   }

   private void uploadTxt() {
      JFileChooser chooser = new JFileChooser();
      chooser.setDialogTitle("Seleccionar archivo .txt");
      chooser.setFileFilter(new FileNameExtensionFilter("Archivos de texto", "txt"));
      if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
         try {
            String content = new String(Files.readAllBytes(chooser.getSelectedFile().toPath()));
            this.inputField.setText(content.trim());
            this.inputField.setForeground(FG);
         } catch (Exception ex) {
            this.addBotMessage("✕ No se pudo leer el archivo.");
         }
      }
   }

   private void styleScrollBar(JScrollBar bar) {
      bar.setUI(new BasicScrollBarUI() {
         protected void configureScrollBarColors() {
            this.thumbColor = new Color(42, 42, 75);
            this.trackColor = BG_CHAT;
         }

         protected JButton createDecreaseButton(int orientation) {
            return this.noop();
         }

         protected JButton createIncreaseButton(int orientation) {
            return this.noop();
         }

         private JButton noop() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
         }
      });
   }

   public static void main(String[] args) {
      try {
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (Exception ignored) {
      }
      SwingUtilities.invokeLater(() -> new ElobotChat().setVisible(true));
   }
}