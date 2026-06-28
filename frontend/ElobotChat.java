// ELOBOT - Asistente Académico UTFSM
// Frontend Java Swing. Backend / API / RAG intactos (ver ElobotApiClient).
//
// Esta clase es la VENTANA PRINCIPAL: construye la interfaz y orquesta el resto de
// las clases del proyecto (composición), pero delega cada responsabilidad concreta:
//   - Theme            -> colores, tipografías y tema oscuro/claro (Singleton)
//   - ElobotApiClient   -> comunicación con el backend
//   - RamoCodeNormalizer -> normalización del código de ramo
//   - ChatMessage / Conversation -> modelo de datos
//   - VectorIcon / AnimatedAvatar -> componentes visuales reutilizables
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
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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

   private static final String PLACEHOLDER_PREGUNTA = "Escribe tu pregunta...";
   private static final String PLACEHOLDER_CODIGO = "Ej: ELO-329";
   private static final String GREETING = "¡Hola! Soy ELOBOT.\nIngresa el código de tu asignatura (ej: ELO-329) y hazme una pregunta.\nSi dejas el campo vacío, buscaré en todos los programas.";

   // ---- Colaboradores (composición: ElobotChat "tiene un" ElobotApiClient) ----
   private final ElobotApiClient apiClient = new ElobotApiClient();

   // ---- Estado / componentes de UI ----
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

   // ---- Datos en memoria (agregación: ElobotChat "tiene muchas" Conversation/ChatMessage) ----
   private List<ChatMessage> currentMessages = new ArrayList<>();
   private List<Conversation> conversationHistory = new ArrayList<>();
   private Conversation activeConversation = null;

   public ElobotChat() {
      this.setUndecorated(true);
      this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      this.setSize(1040, 720);
      this.setMinimumSize(new Dimension(860, 560));
      this.setLocationRelativeTo((Component) null);
      this.rebuildContent();
      SwingUtilities.invokeLater(() -> this.addBotMessage(GREETING));
   }

   // ===================== TEMA =====================

   private void toggleTheme() {
      if (this.typingRow != null) {
         this.hideTyping();
      }
      Theme.getInstance().toggle();
      this.rebuildContent();
      this.showToast(Theme.getInstance().isDark() ? "✓ Tema oscuro activado" : "✓ Tema claro activado");
   }

   // ===================== CONSTRUCCIÓN DE LA INTERFAZ =====================

   private void rebuildContent() {
      Theme theme = Theme.getInstance();
      JPanel root = new JPanel(new BorderLayout());
      root.setBackground(theme.getBg());
      root.setBorder(BorderFactory.createLineBorder(theme.getBorder()));

      root.add(this.buildSidebar(), BorderLayout.WEST);

      JPanel mainArea = new JPanel(new BorderLayout());
      mainArea.setBackground(theme.getBg());

      JPanel topStack = new JPanel();
      topStack.setLayout(new BoxLayout(topStack, BoxLayout.Y_AXIS));
      topStack.setBackground(theme.getBg());
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
      Theme theme = Theme.getInstance();
      JPanel sidebar = new JPanel(new BorderLayout());
      sidebar.setBackground(theme.getSidebarBg());
      sidebar.setPreferredSize(new Dimension(230, 0));
      sidebar.setBorder(new MatteBorder(0, 0, 0, 1, theme.getBorder()));

      JPanel top = new JPanel();
      top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
      top.setBackground(theme.getSidebarBg());
      top.setBorder(new EmptyBorder(18, 16, 12, 16));

      JPanel titleRow = new JPanel();
      titleRow.setLayout(new BoxLayout(titleRow, BoxLayout.X_AXIS));
      titleRow.setBackground(theme.getSidebarBg());
      titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
      JLabel classIcon = new JLabel(new VectorIcon("class", 16));
      classIcon.setForeground(Theme.ACCENT2);
      titleRow.add(classIcon);
      titleRow.add(Box.createRigidArea(new Dimension(7, 0)));
      JLabel title = new JLabel("Historial");
      title.setFont(new Font("Segoe UI", Font.BOLD, 15));
      title.setForeground(theme.getFg());
      titleRow.add(title);
      top.add(titleRow);
      top.add(Box.createRigidArea(new Dimension(0, 4)));
      JLabel subtitle = new JLabel("Conversaciones y ramos consultados");
      subtitle.setFont(Theme.FONT_TINY);
      subtitle.setForeground(theme.getFgMuted());
      subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
      top.add(subtitle);
      top.add(Box.createRigidArea(new Dimension(0, 14)));

      JButton newConvBtn = this.buildSidebarActionButton("Nueva conversación");
      newConvBtn.addActionListener(e -> this.startNewConversation());
      top.add(newConvBtn);

      sidebar.add(top, BorderLayout.NORTH);

      this.historyListPanel = new JPanel();
      this.historyListPanel.setLayout(new BoxLayout(this.historyListPanel, BoxLayout.Y_AXIS));
      this.historyListPanel.setBackground(theme.getSidebarBg());
      this.historyListPanel.setBorder(new EmptyBorder(2, 12, 12, 12));

      JScrollPane historyScroll = new JScrollPane(this.historyListPanel);
      historyScroll.setBorder((Border) null);
      historyScroll.setBackground(theme.getSidebarBg());
      historyScroll.getViewport().setBackground(theme.getSidebarBg());
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
      Theme theme = Theme.getInstance();
      this.historyListPanel.removeAll();
      if (this.conversationHistory.isEmpty()) {
         JLabel empty = new JLabel("Sin conversaciones guardadas");
         empty.setFont(Theme.FONT_TINY);
         empty.setForeground(theme.getFgMuted());
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
               g2.setColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 55));
               g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 10, 10);
            } else if (hover[0]) {
               g2.setColor(Theme.getInstance().getSidebarItemHover());
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

      JLabel lbl = new JLabel(conv.getTitle());
      lbl.setFont(Theme.FONT_UI);
      lbl.setForeground(Theme.getInstance().getFg());
      lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
      JLabel sub = new JLabel((conv.getCodigo() == null || conv.getCodigo().isEmpty()) ? "General" : conv.getCodigo());
      sub.setFont(Theme.FONT_TINY);
      sub.setForeground(Theme.getInstance().getFgMuted());
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
            Color c1 = hot ? Theme.ACCENT2 : Theme.ACCENT;
            Color c2 = hot ? Theme.ACCENT : Theme.ACCENT2;
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
      this.codigoField.setForeground(Theme.getInstance().getFgMuted());
      this.addBotMessage(GREETING);
      this.refreshHistoryList();
   }

   private void archiveCurrentIfNeeded() {
      boolean hasUserMsg = false;
      for (ChatMessage m : this.currentMessages) {
         if (m.isUser()) {
            hasUserMsg = true;
            break;
         }
      }
      if (!hasUserMsg || this.activeConversation != null) {
         return;
      }
      String codigo = RamoCodeNormalizer.normalizeOrEmpty(this.codigoField.getText(), PLACEHOLDER_CODIGO);
      String title = "Conversación";
      for (ChatMessage m : this.currentMessages) {
         if (m.isUser()) {
            title = m.getText().length() > 34 ? m.getText().substring(0, 34) + "…" : m.getText();
            break;
         }
      }
      Conversation conv = new Conversation(title, codigo, this.currentMessages);
      this.conversationHistory.add(0, conv);
   }

   private void loadConversation(Conversation conv) {
      this.archiveCurrentIfNeeded();
      this.activeConversation = conv;
      this.currentMessages = conv.getMessages();
      if (conv.getCodigo() != null && !conv.getCodigo().isEmpty()) {
         this.codigoField.setText(conv.getCodigo());
         this.codigoField.setForeground(Theme.getInstance().getFg());
      } else {
         this.codigoField.setText(PLACEHOLDER_CODIGO);
         this.codigoField.setForeground(Theme.getInstance().getFgMuted());
      }
      this.renderAllMessages();
      this.refreshHistoryList();
   }

   // ---- Header (arrastrable, avatar animado, controles ventana) ----

   private JPanel buildHeader() {
      Theme theme = Theme.getInstance();
      JPanel headerRow = new JPanel(new BorderLayout());
      headerRow.setBackground(theme.getBg());
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
      left.setBackground(theme.getBg());
      this.headerAvatar = new AnimatedAvatar(46);
      left.add(this.headerAvatar);
      left.add(Box.createRigidArea(new Dimension(12, 0)));
      JPanel titles = new JPanel();
      titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
      titles.setBackground(theme.getBg());
      JLabel name = new JLabel("ELOBOT");
      name.setFont(new Font("Segoe UI", Font.BOLD, 18));
      name.setForeground(theme.getFg());
      JLabel sub = new JLabel("Asistente Académico · UTFSM");
      sub.setFont(Theme.FONT_SMALL);
      sub.setForeground(theme.getFgMuted());
      titles.add(name);
      titles.add(sub);
      left.add(titles);

      JPanel right = new JPanel();
      right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
      right.setBackground(theme.getBg());
      JLabel online = new JLabel("● En línea");
      online.setFont(Theme.FONT_SMALL);
      online.setForeground(Theme.FG_GREEN);
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
      wrapper.setBackground(theme.getBg());
      wrapper.add(headerRow, BorderLayout.CENTER);
      wrapper.add(this.buildGradSep(), BorderLayout.SOUTH);
      return wrapper;
   }

   private JPanel buildGradSep() {
      JPanel sep = new JPanel() {
         protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0.0F, 0.0F, Theme.ACCENT, (float) this.getWidth(), 0.0F, Theme.ACCENT2));
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
      Theme theme = Theme.getInstance();
      JPanel row = new JPanel(new BorderLayout());
      row.setBackground(theme.getBg());
      row.setBorder(new EmptyBorder(10, 20, 6, 20));

      JLabel label = new JLabel("Acciones rápidas");
      label.setFont(Theme.FONT_TINY);
      label.setForeground(theme.getFgMuted());
      row.add(label, BorderLayout.WEST);

      JPanel right = new JPanel();
      right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
      right.setBackground(theme.getBg());
      right.add(this.buildIconButton("save", "Guardar conversación (.txt)", this::saveConversationToTxt));
      right.add(Box.createRigidArea(new Dimension(8, 0)));
      right.add(this.buildIconButton("trash", "Limpiar chat actual", this::clearCurrentChat));
      right.add(Box.createRigidArea(new Dimension(8, 0)));
      this.themeButton = this.buildIconButton(theme.isDark() ? "moon" : "sun", "Cambiar tema claro/oscuro", this::toggleTheme);
      right.add(this.themeButton);
      right.add(Box.createRigidArea(new Dimension(8, 0)));
      right.add(this.buildIconButton("share", "Compartir / copiar conversación", this::shareConversation));
      row.add(right, BorderLayout.EAST);
      return row;
   }

   private JButton buildIconButton(String iconType, String tooltip, Runnable action) {
      JButton btn = new JButton() {
         protected void paintComponent(Graphics g) {
            Theme theme = Theme.getInstance();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color bgc = this.getModel().isPressed() ? new Color(theme.getBorder().getRed(), theme.getBorder().getGreen(), theme.getBorder().getBlue())
                  : (this.getModel().isRollover() ? new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 50) : theme.getFieldBg());
            g2.setColor(bgc);
            g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 12, 12);
            g2.setColor(theme.getBorder());
            g2.setStroke(new BasicStroke(1.0F));
            g2.drawRoundRect(0, 0, this.getWidth() - 1, this.getHeight() - 1, 12, 12);
            g2.dispose();
            super.paintComponent(g);
         }
      };
      btn.setIcon(new VectorIcon(iconType, 16));
      btn.setForeground(Theme.getInstance().getFgMuted());
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
         sb.append("[").append(m.getTime()).append("] ").append(m.isUser() ? "Tú" : "ELOBOT").append(":\n");
         sb.append(m.getText()).append("\n\n");
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
      this.toastLabel.setForeground(msg.startsWith("✕") ? Theme.FG_RED : Theme.FG_GREEN);
      if (this.toastTimer != null) {
         this.toastTimer.stop();
      }
      this.toastTimer = new Timer(2400, e -> this.toastLabel.setText(" "));
      this.toastTimer.setRepeats(false);
      this.toastTimer.start();
   }

   // ===================== PREGUNTAS RÁPIDAS (temática POO) =====================

   private JPanel buildQuickQuestionsRow() {
      JPanel row = new JPanel();
      row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
      row.setBackground(Theme.getInstance().getBg());
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
            Theme theme = Theme.getInstance();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean hot = this.getModel().isRollover();
            g2.setColor(hot ? new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 40) : theme.getFieldBg());
            g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 16, 16);
            g2.setColor(hot ? Theme.ACCENT2 : theme.getBorder());
            g2.setStroke(new BasicStroke(1.2F));
            g2.drawRoundRect(0, 0, this.getWidth() - 1, this.getHeight() - 1, 16, 16);
            g2.dispose();
            super.paintComponent(g);
         }
      };
      btn.setIcon(new VectorIcon(iconType, 14));
      btn.setIconTextGap(8);
      btn.setFont(Theme.FONT_SMALL);
      btn.setForeground(Theme.getInstance().getFg());
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
         codigo = RamoCodeNormalizer.normalize(codigoRaw);
         this.codigoField.setText(codigo);
         this.codigoField.setForeground(Theme.getInstance().getFg());
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
         this.inputField.setForeground(Theme.getInstance().getFg());
         this.sendMessage();
      }
   }

   // ===================== ÁREA DE CHAT =====================

   private JScrollPane buildChatArea() {
      Theme theme = Theme.getInstance();
      this.chatPanel = new JPanel();
      this.chatPanel.setLayout(new BoxLayout(this.chatPanel, BoxLayout.Y_AXIS));
      this.chatPanel.setBackground(theme.getBgChat());
      this.chatPanel.setBorder(new EmptyBorder(20, 18, 20, 18));
      this.scrollPane = new JScrollPane(this.chatPanel);
      this.scrollPane.setBackground(theme.getBgChat());
      this.scrollPane.getViewport().setBackground(theme.getBgChat());
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
      ChatMessage m = new ChatMessage(text, isUser);
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
      if (m.isUser()) {
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
      Theme theme = Theme.getInstance();
      JPanel col = new JPanel();
      col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
      col.setOpaque(false);
      col.setAlignmentY(0.0F);
      col.add(this.buildBubble(m.getText(), isUser));
      JLabel time = new JLabel(m.getTime());
      time.setFont(Theme.FONT_TINY);
      time.setForeground(theme.getFgMuted());
      time.setBorder(new EmptyBorder(3, isUser ? 0 : 4, 0, 0));
      time.setAlignmentX(isUser ? 1.0F : 0.0F);
      col.add(time);
      if (!isUser) {
         JPanel actions = new JPanel();
         actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));
         actions.setOpaque(false);
         actions.setBorder(new EmptyBorder(4, 4, 0, 0));
         actions.setAlignmentX(0.0F);
         final String text = m.getText();
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
      btn.setFont(Theme.FONT_TINY);
      btn.setForeground(Theme.getInstance().getFgMuted());
      btn.setBorderPainted(false);
      btn.setContentAreaFilled(false);
      btn.setFocusPainted(false);
      btn.setMargin(new java.awt.Insets(2, 2, 2, 2));
      btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
      btn.addMouseListener(new MouseAdapter() {
         public void mouseEntered(MouseEvent e) {
            btn.setForeground(Theme.ACCENT2);
            btn.repaint();
         }

         public void mouseExited(MouseEvent e) {
            btn.setForeground(Theme.getInstance().getFgMuted());
            btn.repaint();
         }
      });
      btn.addActionListener(e -> action.run());
      return btn;
   }

   private void showTyping() {
      Theme theme = Theme.getInstance();
      this.typingRow = new JPanel();
      this.typingRow.setLayout(new BoxLayout(this.typingRow, BoxLayout.X_AXIS));
      this.typingRow.setBackground(theme.getBgChat());
      this.typingRow.setAlignmentX(0.0F);
      final JLabel[] dots = new JLabel[3];
      JPanel dotsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 10)) {
         protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.getInstance().getBubbleBot());
            g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 18, 18);
            g2.dispose();
         }
      };
      dotsPanel.setOpaque(false);
      for (int i = 0; i < 3; ++i) {
         dots[i] = new JLabel("●");
         dots[i].setFont(new Font("Segoe UI", Font.BOLD, 12));
         dots[i].setForeground(i == 0 ? theme.getFg() : theme.getFgMuted());
         dotsPanel.add(dots[i]);
      }
      final int[] idx = {0};
      this.typingTimer = new Timer(380, e -> {
         idx[0] = (idx[0] + 1) % 3;
         for (int i = 0; i < 3; ++i) {
            dots[i].setForeground(i == idx[0] ? theme.getFg() : theme.getFgMuted());
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
      final Color bubbleColor = isUser ? Theme.getInstance().getBubbleUser() : Theme.getInstance().getBubbleBot();
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
      area.setForeground(isUser ? Color.WHITE : Theme.getInstance().getFg());
      area.setFont(Theme.FONT_UI);
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
      Theme theme = Theme.getInstance();
      JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
      panel.setBackground(theme.getBg());
      panel.setBorder(new EmptyBorder(10, 18, 18, 18));

      this.toastLabel = new JLabel(" ");
      this.toastLabel.setFont(Theme.FONT_TINY);
      this.toastLabel.setForeground(Theme.FG_GREEN);
      this.toastLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
      panel.add(this.toastLabel);
      panel.add(Box.createRigidArea(new Dimension(0, 6)));
      panel.add(this.buildGradSep());
      panel.add(Box.createRigidArea(new Dimension(0, 14)));

      JPanel codigoRow = new JPanel();
      codigoRow.setLayout(new BoxLayout(codigoRow, BoxLayout.X_AXIS));
      codigoRow.setBackground(theme.getBg());
      codigoRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
      JLabel codigoLbl = new JLabel("Código del ramo:");
      codigoLbl.setFont(Theme.FONT_SMALL);
      codigoLbl.setForeground(theme.getFgMuted());
      this.codigoField = this.buildStyledField(PLACEHOLDER_CODIGO);
      this.codigoField.setHorizontalAlignment(JTextField.CENTER);
      this.codigoField.setMaximumSize(new Dimension(160, 38));
      this.codigoField.setPreferredSize(new Dimension(160, 38));
      this.codigoField.addFocusListener(new FocusAdapter() {
         public void focusLost(FocusEvent e) {
            String txt = ElobotChat.this.codigoField.getText().trim();
            if (!txt.isEmpty() && !txt.equals(PLACEHOLDER_CODIGO)) {
               ElobotChat.this.codigoField.setText(RamoCodeNormalizer.normalize(txt));
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
      inputRow.setBackground(theme.getBg());
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
               g2.setColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 40));
               g2.fillRoundRect(-2, -2, this.getWidth() + 4, this.getHeight() + 4, 16, 16);
            }
            g2.setColor(Theme.getInstance().getFieldBg());
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
      field.setCaretColor(Theme.getInstance().getFg());
      field.setFont(Theme.FONT_UI);
      field.setBorder(new EmptyBorder(8, 14, 8, 14));
      field.setText(placeholder);
      field.setForeground(Theme.getInstance().getFgMuted());
      field.addFocusListener(new FocusAdapter() {
         public void focusGained(FocusEvent e) {
            if (field.getText().equals(placeholder)) {
               field.setText("");
               field.setForeground(Theme.getInstance().getFg());
            }
         }

         public void focusLost(FocusEvent e) {
            if (field.getText().isEmpty()) {
               field.setText(placeholder);
               field.setForeground(Theme.getInstance().getFgMuted());
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
                  : (this.getModel().isPressed() ? Theme.ACCENT.darker() : (this.getModel().isRollover() ? new Color(122, 90, 255) : Theme.ACCENT));
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
            codigo = RamoCodeNormalizer.normalize(codigoRaw);
            if (!codigo.equals(codigoRaw)) {
               this.codigoField.setText(codigo);
            }
         }

         this.addUserMessage(pregunta);
         this.inputField.setText("");
         this.inputField.setForeground(Theme.getInstance().getFg());
         this.sendButton.setEnabled(false);
         this.showTyping();

         final String preguntaFinal = pregunta;
         final String codigoFinal = codigo;
         new Thread(() -> {
            String respuesta = this.apiClient.callApi(preguntaFinal, codigoFinal);
            SwingUtilities.invokeLater(() -> {
               this.hideTyping();
               this.addBotMessage(respuesta);
               this.sendButton.setEnabled(true);
            });
         }).start();
      }
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
            Theme theme = Theme.getInstance();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(this.getModel().isPressed() ? new Color(32, 32, 58) : (this.getModel().isRollover() ? new Color(28, 28, 52) : theme.getFieldBg()));
            g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 12, 12);
            g2.setColor(theme.getFgMuted());
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
            this.inputField.setForeground(Theme.getInstance().getFg());
         } catch (Exception ex) {
            this.addBotMessage("✕ No se pudo leer el archivo.");
         }
      }
   }

   private void styleScrollBar(JScrollBar bar) {
      bar.setUI(new BasicScrollBarUI() {
         protected void configureScrollBarColors() {
            this.thumbColor = new Color(42, 42, 75);
            this.trackColor = Theme.getInstance().getBgChat();
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