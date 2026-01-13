package productor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class productor_csv {

	// Exchange común y tipo topic
    private static final String EXCHANGE_NAME = "generadorComun";
    private static final String EXCHANGE_TYPE = "topic";

    // Routing key para el formato CSV
    private static final String ROUTING_KEY = "location.csv";
	
    public static void main(String[] args) throws IOException, TimeoutException {

        // Uso: java productor.productor_csv <rabbitMQbroker> <productor-id>
        // Ejemplo: java productor.productor_csv localhost P_CSV
        if (args.length != 2) {
            System.out.println("Usage: java productor.productor_csv <rabbitMQbroker> <productor-id>");
            System.out.println("Example: java productor.productor_csv localhost P_CSV");
            return;
        }

        String rabbitMQ_broker = args[0];
        String ID = args[1];

        // 1. Conexión con el broker
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitMQ_broker);

        Connection connection = factory.newConnection();
        System.out.println("[" + ID + "] Conectado a broker RabbitMQ " + rabbitMQ_broker);

        // 2. Canal
        Channel channel = connection.createChannel();

        // 3. Declarar el exchange de tipo topic (en lugar de declarar cola)
        channel.exchangeDeclare(EXCHANGE_NAME, EXCHANGE_TYPE, false);

        // 4. Publicar mensajes en formato CSV: "matricula, latitud, longitud"
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

            // Esperamos: matricula latitud longitud
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

            // Formato CSV exacto
            String message = matricula + ", " + latitud + ", " + longitud;

            // Publicar al exchange con routing key (NO a una cola)
            channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, null, message.getBytes(StandardCharsets.UTF_8));

            System.out.println(" --> Mensaje enviado desde " + ID + " con key=" + ROUTING_KEY + ": " + message);
        }

        scan.close();

        // 5. Cerrar
        channel.close();
        connection.close();
        System.out.println("\n[" + ID + "] Conexión cerrada!");
    }

}
