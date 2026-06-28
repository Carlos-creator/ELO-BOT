import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Modelo de dominio: representa un único mensaje del chat,
 * ya sea escrito por el usuario o generado por ELOBOT.
 *
 * La marca de tiempo se genera automáticamente al crear el mensaje.
 */
public class ChatMessage {

   private static final DateTimeFormatter TFMT = DateTimeFormatter.ofPattern("HH:mm");

   private final String text;
   private final boolean user;
   private final String time;

   public ChatMessage(String text, boolean user) {
      this.text = text;
      this.user = user;
      this.time = LocalTime.now().format(TFMT);
   }

   public String getText() {
      return this.text;
   }

   public boolean isUser() {
      return this.user;
   }

   public String getTime() {
      return this.time;
   }

    
}
