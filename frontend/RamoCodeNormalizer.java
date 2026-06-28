import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidad de negocio: normaliza el código de un ramo al formato estándar "XXX-000".
 * Ejemplos: "elo329" -> "ELO-329", "ELO329" -> "ELO-329", "elo-329" -> "ELO-329".
 *
 * Clase de utilidad (sin estado): todos sus métodos son estáticos y el constructor
 * es privado para impedir que se instancie.
 */
public final class RamoCodeNormalizer {

   private static final Pattern PATTERN =
         Pattern.compile("^([A-Za-zÁÉÍÓÚáéíóúÑñ]+)[\\s_-]*([0-9]{2,4}[A-Za-z]?)$");

   private RamoCodeNormalizer() {
      // Clase de utilidad: no se instancia.
   }

   public static String normalize(String input) {
      if (input == null) {
         return "";
      }
      String s = input.trim();
      if (s.isEmpty()) {
         return s;
      }
      Matcher m = PATTERN.matcher(s);
      if (m.matches()) {
         return m.group(1).toUpperCase() + "-" + m.group(2).toUpperCase();
      }
      return s.toUpperCase();
   }

   /** Igual que normalize(), pero retorna "" si el texto está vacío o es el placeholder del campo. */
   public static String normalizeOrEmpty(String raw, String placeholder) {
      if (raw == null) {
         return "";
      }
      String s = raw.trim();
      if (s.isEmpty() || s.equals(placeholder)) {
         return "";
      }
      return normalize(s);
   }
}