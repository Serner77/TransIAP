package productor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class productor_geojson {

    private static final String EXCHANGE_NAME = "generadorComun";
    private static final String EXCHANGE_TYPE = "topic";

    private static final String ROUTING_KEY = "location.geojson";

    public static void main(String[] args) throws IOException, TimeoutException {

        // Uso: java productor.productor_geojson <rabbitMQbroker> <productor-id>
        if (args.length != 2) {
            System.out.println("Usage: java productor.productor_geojson <rabbitMQbroker> <productor-id>");
            System.out.println("Example: java productor.productor_geojson localhost P_GEOJSON");
            return;
        }

        String rabbitMQ_broker = args[0];
        String ID = args[1];

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitMQ_broker);

        Connection connection = factory.newConnection();
        System.out.println("[" + ID + "] Conectado a broker RabbitMQ " + rabbitMQ_broker);

        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, EXCHANGE_TYPE, false);

        Scanner scan = new Scanner(System.in);

        System.out.println("[" + ID + "] Publicando a exchange='" + EXCHANGE_NAME + "' tipo='" + EXCHANGE_TYPE + "'");
        System.out.println("[" + ID + "] Routing key='" + ROUTING_KEY + "'");
        System.out.println("Introduce ubicaciones como: matricula latitud longitud");
        System.out.println("Ejemplo: 1234ABC 39.4702 -0.3768");
        System.out.println("Escribe 'salir' para terminar.\n");

        while (true) {
            System.out.print("Introduce Mensaje: ");
            String line = scan.nextLine().trim();

            if (line.equalsIgnoreCase("salir")) {
                break;
            }

            String[] parts = line.split("\\s+");
            if (parts.length != 3) {
                System.out.println("Formato inválido. Usa: matricula latitud longitud");
                continue;
            }

            String matricula = parts[0];
            double latitud;
            double longitud;

            try {
                latitud = Double.parseDouble(parts[1]);
                longitud = Double.parseDouble(parts[2]);
            } catch (NumberFormatException e) {
                System.out.println("Latitud/longitud inválidas (usa números).");
                continue;
            }

            // OJO: tu enunciado dice que coordinates = [latitud, longitud]
            // (lo normal sería [longitud, latitud], pero seguimos el enunciado)
            String message =
                "{ \"type\":\"Feature\","
              + " \"geometry\":{ \"type\":\"Point\", \"coordinates\":[" + latitud + ", " + longitud + "] },"
              + " \"properties\":{ \"vehicle\":\"" + matricula + "\" }"
              + "}";

            channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, null, message.getBytes(StandardCharsets.UTF_8));

            System.out.println(" --> Mensaje enviado desde " + ID + " con key=" + ROUTING_KEY + ": " + message);
        }

        scan.close();
        channel.close();
        connection.close();

        System.out.println("\n[" + ID + "] Conexión cerrada!");
    }
}