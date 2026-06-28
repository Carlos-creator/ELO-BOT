import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Cliente HTTP encargado de TODA la comunicación con el backend de ELOBOT (RAG/Mongo).
 * Esta es la única clase que conoce el endpoint y el formato del JSON: el resto de la
 * aplicación (la interfaz Swing) solo le pide "callApi(pregunta, codigoRamo)" y recibe
 * texto de vuelta, sin saber nada de HTTP.
 *
 * Lógica 100% intacta respecto a la versión original: mismo endpoint, mismo formato
 * de petición y misma forma de parsear la respuesta.
 */
public class ElobotApiClient {

   private static final String ENDPOINT = "http://localhost:8001/consultar";

   public String callApi(String pregunta, String codigoRamo) {
      try {
         String preguntaEsc = this.esc(pregunta);
         String body = "{\"pregunta\":\"" + preguntaEsc + "\",\"codigo_ramo\":\"" + this.esc(codigoRamo) + "\"}";
         HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
         HttpResponse<String> response = client.send(
               HttpRequest.newBuilder()
                     .uri(URI.create(ENDPOINT))
                     .header("Content-Type", "application/json")
                     .POST(HttpRequest.BodyPublishers.ofString(body))
                     .build(),
               HttpResponse.BodyHandlers.ofString());
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
}