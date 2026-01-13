package middleware_live;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rabbitmq.client.*;

import es.upv.iap.pvalderas.http.HTTPClient;

public class SoporteLogisticaLive {

    // Entrada: mensajes "en bruto" de los generadores
    private static final String IN_EXCHANGE = "generadorComun";
    private static final String IN_EXCHANGE_TYPE = "topic";
    private static final String IN_BINDING_KEY = "location.*";
    private static final String IN_QUEUE = "q.soporte.live";

    // Salida: formato TransIAP-loc/JSON para validación (profes/visualizador/BD)
    private static final String OUT_EXCHANGE = "traslados.localizaciones";
    private static final String OUT_EXCHANGE_TYPE = "fanout";

    // REST SNTN (key + timestamp)
    private static final String REST_BASE = "https://pedvalar.webs.upv.es/iap/rest/sntn";

    public static void main(String[] args) throws IOException, TimeoutException {

        if (args.length != 2) {
            System.out.println("Usage: java middleware_live.SoporteLogisticaLive <rabbitMQbroker> <id>");
            System.out.println("Example: java middleware_live.SoporteLogisticaLive localhost LIVE");
            return;
        }

        String rabbitMQ_broker = args[0];
        final String ID = args[1];

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitMQ_broker);

        Connection connection = factory.newConnection();
        final Channel channel = connection.createChannel();

        // Declaramos exchanges y la cola de entrada del middleware
        channel.exchangeDeclare(IN_EXCHANGE, IN_EXCHANGE_TYPE, false);
        channel.exchangeDeclare(OUT_EXCHANGE, OUT_EXCHANGE_TYPE, false);
        channel.queueDeclare(IN_QUEUE, false, false, false, null);
        channel.queueBind(IN_QUEUE, IN_EXCHANGE, IN_BINDING_KEY);

        System.out.println("[" + ID + "] Conectado a RabbitMQ: " + rabbitMQ_broker);
        System.out.println("[" + ID + "] IN: exchange='" + IN_EXCHANGE + "' binding='" + IN_BINDING_KEY + "' queue='" + IN_QUEUE + "'");
        System.out.println("[" + ID + "] OUT: exchange='" + OUT_EXCHANGE + "' type='" + OUT_EXCHANGE_TYPE + "'");
        System.out.println("[" + ID + "] REST base: " + REST_BASE);
        System.out.println();

        DefaultConsumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {

                String rk = envelope.getRoutingKey();
                String payload = new String(body, StandardCharsets.UTF_8);

                try {
                    ParsedLocation loc = parseLocation(rk, payload);

                    String appKey = fetchAppKey(loc.matricula);
                    String timeStamp = fetchTimeStamp();

                    String outJson = buildTransIAPJson(loc.matricula, loc.lat, loc.lon, appKey, timeStamp);

                    // En fanout la routing key se ignora
                    channel.basicPublish(OUT_EXCHANGE, "", null, outJson.getBytes(StandardCharsets.UTF_8));

                    System.out.println("[" + ID + "] OK (" + rk + ") -> " + outJson);

                } catch (Exception e) {
                    System.out.println("[" + ID + "] ERROR procesando (" + rk + "): " + e.getMessage());
                }
            }
        };

        channel.basicConsume(IN_QUEUE, true, consumer);
    }

    private static ParsedLocation parseLocation(String routingKey, String payload) {

        if ("location.csv".equals(routingKey)) {
            return parseCSV(payload);
        } else if ("location.geojson".equals(routingKey)) {
            return parseGeoJSON(payload);
        } else if ("location.kml".equals(routingKey)) {
            return parseKML(payload);
        }

        throw new IllegalArgumentException("Routing key no soportada: " + routingKey);
    }

    private static ParsedLocation parseCSV(String payload) {
        // "matricula, latitud, longitud"
        String[] parts = payload.split(",");
        if (parts.length < 3) throw new IllegalArgumentException("CSV inválido: " + payload);

        String matricula = parts[0].trim();
        double lat = Double.parseDouble(parts[1].trim());
        double lon = Double.parseDouble(parts[2].trim());

        return new ParsedLocation(matricula, lat, lon);
    }

    private static ParsedLocation parseGeoJSON(String payload) {
        String matricula = extractFirstGroup(payload, "\"vehicle\"\\s*:\\s*\"([^\"]+)\"");

        Pattern p = Pattern.compile("\"coordinates\"\\s*:\\s*\\[\\s*([-0-9.]+)\\s*,\\s*([-0-9.]+)\\s*\\]");
        Matcher m = p.matcher(payload);
        if (!m.find()) throw new IllegalArgumentException("GeoJSON sin coordinates: " + payload);

        double lat = Double.parseDouble(m.group(1));
        double lon = Double.parseDouble(m.group(2));

        return new ParsedLocation(matricula, lat, lon);
    }

    private static ParsedLocation parseKML(String payload) {
        String matricula = extractFirstGroup(payload, "id\"\\s*=\\s*\"([^\"]+)\"");

        Pattern p = Pattern.compile("<coordinates>\\s*([-0-9.]+)\\s*,\\s*([-0-9.]+)\\s*</coordinates>");
        Matcher m = p.matcher(payload);
        if (!m.find()) throw new IllegalArgumentException("KML sin coordinates: " + payload);

        double lat = Double.parseDouble(m.group(1));
        double lon = Double.parseDouble(m.group(2));

        return new ParsedLocation(matricula, lat, lon);
    }

    private static String fetchAppKey(String matricula) throws IOException {
        String url = REST_BASE + "/key/" + matricula + "/get";
        String resp = HTTPClient.get(url, "application/json");

        // El servicio a veces devuelve el appKey como número (sin comillas)
        String key = extractFirstMatch(resp,
                "\"appKey\"\\s*:\\s*\"?([^\"}]+)\"?",
                "\"appkey\"\\s*:\\s*\"?([^\"}]+)\"?",
                "\"key\"\\s*:\\s*\"?([^\"}]+)\"?",
                "\"token\"\\s*:\\s*\"?([^\"}]+)\"?"
        );

        if (key == null) throw new IOException("No se pudo extraer appKey de: " + resp);
        return key;
    }

    private static String fetchTimeStamp() throws IOException {
        String url = REST_BASE + "/timestamp/get";
        String resp = HTTPClient.get(url, "application/json");

        String ts = extractFirstMatch(resp,
                "\"timeStamp\"\\s*:\\s*\"?([^\"}]+)\"?",
                "\"timestamp\"\\s*:\\s*\"?([^\"}]+)\"?"
        );

        if (ts == null) throw new IOException("No se pudo extraer timeStamp de: " + resp);
        return ts;
    }

    private static String buildTransIAPJson(String matricula, double lat, double lon, String auth, String timestamp) {
        return "{"
            + "\"coordenadas\":{\"latitud\":" + lat + ",\"longitud\":" + lon + "},"
            + "\"vehiculo\":\"" + matricula + "\","
            + "\"auth\":\"" + auth + "\","
            + "\"timestamp\":\"" + timestamp + "\""
            + "}";
    }

    private static String extractFirstGroup(String text, String regex) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(text);
        if (!m.find()) throw new IllegalArgumentException("No match regex: " + regex);
        return m.group(1);
    }

    private static String extractFirstMatch(String text, String... regexes) {
        for (String r : regexes) {
            Pattern p = Pattern.compile(r, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(text);
            if (m.find()) return m.group(1);
        }
        return null;
    }

    private static class ParsedLocation {
        String matricula;
        double lat;
        double lon;

        ParsedLocation(String matricula, double lat, double lon) {
            this.matricula = matricula;
            this.lat = lat;
            this.lon = lon;
        }
    }
}
