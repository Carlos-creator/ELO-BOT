import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ElobotChat extends JFrame {

    // ── PALETA ────────────────────────────────────────────────────────────────────
    private static final Color BG       = new Color( 10,  10,  22);
    private static final Color BG_CHAT  = new Color( 14,  14,  28);
    private static final Color BUBBLE_U = new Color( 65,  95, 235);
    private static final Color BUBBLE_B = new Color( 24,  24,  50);
    private static final Color ACCENT   = new Color(108,  78, 252);
    private static final Color ACCENT2  = new Color( 65,  95, 235);
    private static final Color FG       = new Color(232, 232, 248);
    private static final Color FG_MUTED = new Color( 95,  95, 132);
    private static final Color FG_GREEN = new Color(  0, 208, 125);
    private static final Color FIELD_BG = new Color( 20,  20,  44);
    private static final Color BORDER   = new Color( 36,  36,  68);
    private static final Font  F_UI     = new Font("Segoe UI", Font.PLAIN,  13);
    private static final Font  F_SMALL  = new Font("Segoe UI", Font.PLAIN,  11);
    private static final Font  F_TINY   = new Font("Segoe UI", Font.PLAIN,  10);
    private static final DateTimeFormatter TFMT = DateTimeFormatter.ofPattern("HH:mm");

    // ── ESTADO ────────────────────────────────────────────────────────────────────
    private JPanel      chatPanel;
    private JScrollPane scrollPane;
    private JTextField  inputField;
    private JTextField  codigoField;
    private JButton     sendButton;
    private JPanel      typingRow;
    private Timer       typingTimer;
    private Point       dragStart;

    // ── CONSTRUCTOR ───────────────────────────────────────────────────────────────
    public ElobotChat() {
        setUndecorated(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(840, 680);
        setMinimumSize(new Dimension(640, 500));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.setBorder(BorderFactory.createLineBorder(BORDER));
        root.add(buildHeader(),     BorderLayout.NORTH);
        root.add(buildChatArea(),   BorderLayout.CENTER);
        root.add(buildInputPanel(), BorderLayout.SOUTH);
        setContentPane(root);

        SwingUtilities.invokeLater(() ->
            addBotMessage("¡Hola! Soy ELOBOT 🎓\n" +
                          "Ingresa el código de tu asignatura (ej: ELO-329) y hazme una pregunta.\n" +
                          "Si dejas el campo vacío, buscaré en todos los programas.")
        );
    }

    // ── HEADER ────────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG);
        header.setBorder(new EmptyBorder(14, 20, 14, 20));

        MouseAdapter drag = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { dragStart = e.getPoint(); }
            @Override public void mouseDragged(MouseEvent e)  {
                if (dragStart == null) return;
                Point s = e.getLocationOnScreen();
                setLocation(s.x - dragStart.x, s.y - dragStart.y);
            }
        };
        header.addMouseListener(drag);
        header.addMouseMotionListener(drag);
        header.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

        // Lado izquierdo
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        left.setBackground(BG);
        left.add(buildHeaderAvatar());
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

        // Lado derecho
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.setBackground(BG);
        JLabel status = new JLabel("● En línea");
        status.setFont(F_SMALL);
        status.setForeground(FG_GREEN);
        JButton minBtn   = buildWinBtn(new Color(255, 189, 46), false);
        JButton closeBtn = buildWinBtn(new Color(255,  95,  87), true);
        minBtn.addActionListener(e -> setState(Frame.ICONIFIED));
        closeBtn.addActionListener(e -> System.exit(0));
        right.add(status);
        right.add(Box.createRigidArea(new Dimension(18, 0)));
        right.add(minBtn);
        right.add(Box.createRigidArea(new Dimension(8, 0)));
        right.add(closeBtn);

        header.add(left,  BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.add(header,       BorderLayout.CENTER);
        wrapper.add(buildGradSep(), BorderLayout.SOUTH);
        return wrapper;
    }

    private JLabel buildHeaderAvatar() {
        JLabel av = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(108, 78, 252, 45));
                g2.fillOval(-3, -3, getWidth()+6, getHeight()+6);
                g2.setPaint(new GradientPaint(0, 0, ACCENT, getWidth(), getHeight(), ACCENT2));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("E",
                    (getWidth()  - fm.stringWidth("E")) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        Dimension d = new Dimension(46, 46);
        av.setPreferredSize(d); av.setMaximumSize(d); av.setMinimumSize(d);
        return av;
    }

    private JPanel buildGradSep() {
        return new JPanel() {
            { setPreferredSize(new Dimension(1, 2)); setOpaque(false); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, ACCENT, getWidth(), 0, ACCENT2));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
    }

    // ── ÁREA DE CHAT ──────────────────────────────────────────────────────────────
    private JScrollPane buildChatArea() {
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(BG_CHAT);
        chatPanel.setBorder(new EmptyBorder(20, 18, 20, 18));

        scrollPane = new JScrollPane(chatPanel);
        scrollPane.setBackground(BG_CHAT);
        scrollPane.getViewport().setBackground(BG_CHAT);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        styleScrollBar(scrollPane.getVerticalScrollBar());
        return scrollPane;
    }

    // ── PANEL DE ENTRADA ──────────────────────────────────────────────────────────
    private JPanel buildInputPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(14, 18, 18, 18));
        panel.add(buildGradSep());
        panel.add(Box.createRigidArea(new Dimension(0, 14)));

        // Fila código
        JPanel row1 = new JPanel();
        row1.setLayout(new BoxLayout(row1, BoxLayout.X_AXIS));
        row1.setBackground(BG);
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        JLabel lbl = new JLabel("Código del ramo:");
        lbl.setFont(F_SMALL);
        lbl.setForeground(FG_MUTED);
        codigoField = buildStyledField("Ej: ELO-329");
        codigoField.setHorizontalAlignment(JTextField.CENTER);
        codigoField.setMaximumSize(new Dimension(160, 38));
        codigoField.setPreferredSize(new Dimension(160, 38));
        row1.add(lbl);
        row1.add(Box.createRigidArea(new Dimension(10, 0)));
        row1.add(codigoField);
        row1.add(Box.createHorizontalGlue());
        panel.add(row1);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Fila pregunta
        JPanel row2 = new JPanel();
        row2.setLayout(new BoxLayout(row2, BoxLayout.X_AXIS));
        row2.setBackground(BG);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        inputField = buildStyledField("Escribe tu pregunta...");
        inputField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        sendButton = buildSendButton();
        sendButton.setMaximumSize(new Dimension(52, 44));
        sendButton.setPreferredSize(new Dimension(52, 44));
        JButton uploadBtn = buildUploadButton();
        uploadBtn.setMaximumSize(new Dimension(44, 44));
        uploadBtn.setPreferredSize(new Dimension(44, 44));
        row2.add(uploadBtn);
        row2.add(Box.createRigidArea(new Dimension(8, 0)));
        row2.add(inputField);
        row2.add(Box.createRigidArea(new Dimension(8, 0)));
        row2.add(sendButton);
        panel.add(row2);

        ActionListener onSend = e -> sendMessage();
        sendButton.addActionListener(onSend);
        inputField.addActionListener(onSend);
        return panel;
    }

    private JTextField buildStyledField(String ph) {
        JTextField tf = new JTextField() {
            private boolean focused;
            {
                addFocusListener(new FocusAdapter() {
                    @Override public void focusGained(FocusEvent e) { focused = true;  repaint(); }
                    @Override public void focusLost (FocusEvent e) { focused = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (focused) {
                    g2.setColor(new Color(108, 78, 252, 40));
                    g2.fillRoundRect(-2, -2, getWidth()+4, getHeight()+4, 16, 16);
                }
                g2.setColor(FIELD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        tf.setOpaque(false);
        tf.setCaretColor(FG);
        tf.setFont(F_UI);
        tf.setBorder(new EmptyBorder(8, 14, 8, 14));
        tf.setText(ph);
        tf.setForeground(FG_MUTED);
        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (tf.getText().equals(ph)) { tf.setText(""); tf.setForeground(FG); }
            }
            @Override public void focusLost(FocusEvent e) {
                if (tf.getText().isEmpty()) { tf.setText(ph); tf.setForeground(FG_MUTED); }
            }
        });
        return tf;
    }

    private JButton buildSendButton() {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = !getModel().isEnabled()  ? new Color(35, 35, 60)    :
                            getModel().isPressed()   ? ACCENT.darker()          :
                            getModel().isRollover()  ? new Color(122, 90, 255)  : ACCENT;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                // Flecha →
                g2.setColor(getModel().isEnabled() ? Color.WHITE : new Color(60, 60, 90));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth()/2, cy = getHeight()/2;
                g2.drawLine(cx-8, cy, cx+3, cy);
                int[] ax = {cx, cx+8, cx};
                int[] ay = {cy-5, cy, cy+5};
                g2.fillPolygon(ax, ay, 3);
                g2.dispose();
            }
        };
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── MENSAJES ──────────────────────────────────────────────────────────────────
    private void addBotMessage(String text)  { appendMessage(text, false); }
    private void addUserMessage(String text) { appendMessage(text, true);  }

    private void appendMessage(String text, boolean isUser) {
        String time = LocalTime.now().format(TFMT);
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setBackground(BG_CHAT);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (isUser) {
            row.add(Box.createHorizontalGlue());
            row.add(msgColumn(text, true, time));
        } else {
            JLabel av = buildMiniAvatar();
            av.setAlignmentY(Component.TOP_ALIGNMENT);
            JPanel col = msgColumn(text, false, time);
            col.setAlignmentY(Component.TOP_ALIGNMENT);
            row.add(av);
            row.add(Box.createRigidArea(new Dimension(8, 0)));
            row.add(col);
            row.add(Box.createHorizontalGlue());
        }

        chatPanel.add(row);
        chatPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        scrollToBottom();
    }

    private JPanel msgColumn(String text, boolean isUser, String time) {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);
        col.setAlignmentY(Component.TOP_ALIGNMENT);
        col.add(buildBubble(text, isUser));
        JLabel t = new JLabel(time);
        t.setFont(F_TINY);
        t.setForeground(FG_MUTED);
        t.setBorder(new EmptyBorder(3, isUser ? 0 : 4, 0, 0));
        t.setAlignmentX(isUser ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        col.add(t);
        return col;
    }

    private JLabel buildMiniAvatar() {
        JLabel av = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, ACCENT, getWidth(), getHeight(), ACCENT2));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("E",
                    (getWidth()  - fm.stringWidth("E")) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        Dimension d = new Dimension(30, 30);
        av.setPreferredSize(d); av.setMaximumSize(d); av.setMinimumSize(d);
        return av;
    }

    private void showTyping() {
        typingRow = new JPanel();
        typingRow.setLayout(new BoxLayout(typingRow, BoxLayout.X_AXIS));
        typingRow.setBackground(BG_CHAT);
        typingRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel[] dots = new JLabel[3];
        JPanel bubble = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 10)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BUBBLE_B);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
            }
        };
        bubble.setOpaque(false);

        for (int i = 0; i < 3; i++) {
            dots[i] = new JLabel("●");
            dots[i].setFont(new Font("Segoe UI", Font.BOLD, 12));
            dots[i].setForeground(i == 0 ? FG : FG_MUTED);
            bubble.add(dots[i]);
        }

        final int[] step = {0};
        typingTimer = new Timer(380, e -> {
            step[0] = (step[0] + 1) % 3;
            for (int i = 0; i < 3; i++) dots[i].setForeground(i == step[0] ? FG : FG_MUTED);
        });
        typingTimer.start();

        JLabel av = buildMiniAvatar();
        av.setAlignmentY(Component.TOP_ALIGNMENT);
        typingRow.add(av);
        typingRow.add(Box.createRigidArea(new Dimension(8, 0)));
        typingRow.add(bubble);
        typingRow.add(Box.createHorizontalGlue());
        chatPanel.add(typingRow);
        chatPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        scrollToBottom();
    }

    private void hideTyping() {
        if (typingTimer != null) { typingTimer.stop(); typingTimer = null; }
        if (typingRow == null) return;
        for (int i = 0; i < chatPanel.getComponentCount(); i++) {
            if (chatPanel.getComponent(i) == typingRow) {
                if (i + 1 < chatPanel.getComponentCount()) chatPanel.remove(i + 1);
                chatPanel.remove(i);
                break;
            }
        }
        typingRow = null;
        chatPanel.revalidate();
        chatPanel.repaint();
    }

    private JPanel buildBubble(String text, boolean isUser) {
        Color bg = isUser ? BUBBLE_U : BUBBLE_B;
        JPanel bubble = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
            }
        };
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setOpaque(false);
        bubble.setBorder(new EmptyBorder(10, 14, 10, 14));
        bubble.setMaximumSize(new Dimension(520, Integer.MAX_VALUE));
        bubble.setAlignmentX(isUser ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);

        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setForeground(FG);
        area.setFont(F_UI);
        area.setBorder(null);
        area.setColumns(30);
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        bubble.add(area);
        return bubble;
    }

    private void scrollToBottom() {
        chatPanel.revalidate();
        chatPanel.repaint();
        SwingUtilities.invokeLater(() ->
            scrollPane.getVerticalScrollBar().setValue(
                scrollPane.getVerticalScrollBar().getMaximum()));
    }

    // ── LÓGICA ────────────────────────────────────────────────────────────────────
    private void sendMessage() {
        String ph1 = "Escribe tu pregunta...";
        String ph2 = "Ej: ELO-329";
        String q = inputField.getText().trim();
        if (q.isEmpty() || q.equals(ph1)) return;
        String c = codigoField.getText().trim();
        if (c.equals(ph2)) c = "";

        String finalQ = q, finalC = c;
        addUserMessage(q);
        inputField.setText("");
        inputField.setForeground(FG);
        sendButton.setEnabled(false);
        showTyping();

        new Thread(() -> {
            String resp = callApi(finalQ, finalC);
            SwingUtilities.invokeLater(() -> {
                hideTyping();
                addBotMessage(resp);
                sendButton.setEnabled(true);
            });
        }).start();
    }

    private String callApi(String pregunta, String codigo) {
        try {
            String body = "{\"pregunta\":\"" + esc(pregunta) +
                          "\",\"codigo_ramo\":\"" + esc(codigo) + "\"}";
            HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
            HttpResponse<String> res = client.send(
                HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8001/consultar"))
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(body))
                    .build(),
                BodyHandlers.ofString()
            );
            return parseRespuesta(res.body());
        } catch (Exception e) {
            return "❌ No se pudo conectar con el servidor.\n\n" +
                   "Asegúrate de que la API esté corriendo con:\n" +
                   "    uvicorn api:app --reload --port 8001";
        }
    }

    private String parseRespuesta(String json) {
        int k = json.indexOf("\"respuesta\"");
        if (k < 0) return "Error: respuesta inesperada del servidor.";
        int q = json.indexOf("\"", json.indexOf(":", k)) + 1;
        StringBuilder sb = new StringBuilder();
        for (int i = q; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char n = json.charAt(++i);
                switch (n) {
                    case '"':  sb.append('"');  break;
                    case 'n':  sb.append('\n'); break;
                    case 't':  sb.append('\t'); break;
                    case '\\': sb.append('\\'); break;
                    default:   sb.append(n);
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }

    // ── BOTONES DE VENTANA ────────────────────────────────────────────────────────
    private JButton buildWinBtn(Color color, boolean isClose) {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? color : new Color(45, 45, 72));
                g2.fillOval(0, 0, getWidth(), getHeight());
                if (getModel().isRollover()) {
                    g2.setColor(new Color(0, 0, 0, 90));
                    g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int p = getWidth() / 4;
                    if (isClose) {
                        g2.drawLine(p, p, getWidth()-p, getHeight()-p);
                        g2.drawLine(getWidth()-p, p, p, getHeight()-p);
                    } else {
                        g2.drawLine(p, getHeight()/2, getWidth()-p, getHeight()/2);
                    }
                }
                g2.dispose();
            }
        };
        Dimension d = new Dimension(16, 16);
        btn.setPreferredSize(d); btn.setMaximumSize(d);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton buildUploadButton() {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed()  ? new Color(32, 32, 58) :
                             getModel().isRollover() ? new Color(28, 28, 52) : FIELD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                // Ícono upload (flecha arriba + base)
                g2.setColor(FG_MUTED);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth()/2, cy = getHeight()/2;
                g2.drawLine(cx-7, cy+8, cx+7, cy+8);
                g2.drawLine(cx, cy+5, cx, cy-4);
                int[] ax = {cx-5, cx, cx+5};
                int[] ay = {cy-1, cy-7, cy-1};
                g2.fillPolygon(ax, ay, 3);
                g2.dispose();
            }
        };
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Subir archivo .txt");
        btn.addActionListener(e -> uploadTxt());
        return btn;
    }

    private void uploadTxt() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar archivo .txt");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos de texto", "txt"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String content = new String(Files.readAllBytes(fc.getSelectedFile().toPath()));
                inputField.setText(content.trim());
                inputField.setForeground(FG);
            } catch (Exception ex) {
                addBotMessage("❌ No se pudo leer el archivo.");
            }
        }
    }

    private void styleScrollBar(JScrollBar bar) {
        bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = new Color(42, 42, 75);
                trackColor = BG_CHAT;
            }
            @Override protected JButton createDecreaseButton(int o) { return noop(); }
            @Override protected JButton createIncreaseButton(int o) { return noop(); }
            private JButton noop() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new ElobotChat().setVisible(true));
    }
}
