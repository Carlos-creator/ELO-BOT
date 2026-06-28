import java.awt.Color;
import java.awt.Font;

/**
 * Gestiona la paleta de colores, las tipografías y el estado de tema oscuro/claro
 * de toda la aplicación.
 *
 * Implementado como Singleton: existe una única instancia de Theme, compartida por
 * todos los componentes de la interfaz. Esto evita tener que pasar el tema como
 * parámetro a través de cada panel, botón o ícono — cualquier clase puede consultar
 * Theme.getInstance() para saber cómo pintarse.
 */
public class Theme {

   private static final Theme INSTANCE = new Theme();

   // Colores de marca: no cambian al alternar entre tema oscuro y claro.
   public static final Color ACCENT = new Color(108, 78, 252);
   public static final Color ACCENT2 = new Color(65, 95, 235);
   public static final Color FG_GREEN = new Color(0, 208, 125);
   public static final Color FG_RED = new Color(255, 99, 99);

   // Tipografías: tampoco dependen del tema.
   public static final Font FONT_UI = new Font("Segoe UI", Font.PLAIN, 13);
   public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
   public static final Font FONT_TINY = new Font("Segoe UI", Font.PLAIN, 10);

   // Estado que sí depende del tema activo (oscuro u claro).
   private boolean dark = true;
   private Color bg;
   private Color bgChat;
   private Color bubbleUser;
   private Color bubbleBot;
   private Color fg;
   private Color fgMuted;
   private Color fieldBg;
   private Color border;
   private Color sidebarBg;
   private Color sidebarItemHover;

   private Theme() {
      this.applyPalette(true);
   }

   public static Theme getInstance() {
      return INSTANCE;
   }

   public boolean isDark() {
      return this.dark;
   }

   /** Alterna entre tema oscuro y claro, recalculando toda la paleta dependiente. */
   public void toggle() {
      this.dark = !this.dark;
      this.applyPalette(this.dark);
   }

   private void applyPalette(boolean dark) {
      if (dark) {
         this.bg = new Color(10, 10, 22);
         this.bgChat = new Color(14, 14, 28);
         this.bubbleUser = new Color(65, 95, 235);
         this.bubbleBot = new Color(24, 24, 50);
         this.fg = new Color(232, 232, 248);
         this.fgMuted = new Color(95, 95, 132);
         this.fieldBg = new Color(20, 20, 44);
         this.border = new Color(36, 36, 68);
         this.sidebarBg = new Color(16, 16, 34);
         this.sidebarItemHover = new Color(30, 30, 58);
      } else {
         this.bg = new Color(246, 247, 251);
         this.bgChat = new Color(255, 255, 255);
         this.bubbleUser = new Color(99, 124, 255);
         this.bubbleBot = new Color(232, 233, 241);
         this.fg = new Color(28, 28, 40);
         this.fgMuted = new Color(120, 120, 140);
         this.fieldBg = new Color(255, 255, 255);
         this.border = new Color(221, 223, 233);
         this.sidebarBg = new Color(237, 238, 245);
         this.sidebarItemHover = new Color(223, 225, 237);
      }
   }

   public Color getBg() {
      return this.bg;
   }

   public Color getBgChat() {
      return this.bgChat;
   }

   public Color getBubbleUser() {
      return this.bubbleUser;
   }

   public Color getBubbleBot() {
      return this.bubbleBot;
   }

   public Color getFg() {
      return this.fg;
   }

   public Color getFgMuted() {
      return this.fgMuted;
   }

   public Color getFieldBg() {
      return this.fieldBg;
   }

   public Color getBorder() {
      return this.border;
   }

   public Color getSidebarBg() {
      return this.sidebarBg;
   }

   public Color getSidebarItemHover() {
      return this.sidebarItemHover;
   }
}