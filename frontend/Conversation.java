import java.util.List;

/**
 * Modelo de dominio: una conversación guardada en el historial del sidebar.
 * Agrupa un título, el código de ramo asociado y la lista de mensajes intercambiados.
 */
public class Conversation {

   private final String title;
   private final String codigo;
   private final List<ChatMessage> messages;

   public Conversation(String title, String codigo, List<ChatMessage> messages) {
      this.title = title;
      this.codigo = codigo;
      this.messages = messages;
   }

   public String getTitle() {
      return this.title;
   }

   public String getCodigo() {
      return this.codigo;
   }

   public List<ChatMessage> getMessages() {
      return this.messages;
   }
}