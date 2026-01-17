package visualizador;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.*;

public class Visualizador {

    private static final String EXCHANGE_NAME = "traslados.localizaciones";
    private static final String EXCHANGE_TYPE = "fanout";

    public static void main(String[] args) throws IOException, TimeoutException {

        if (args.length != 2) {
            System.out.println("Usage: java visualizador.VisualizadorFinal <rabbitMQbroker> <id>");
            return;
        }

        String broker = args[0];
        final String ID = args[1];

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(broker);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, EXCHANGE_TYPE, false);

        // Cola temporal para este visualizador
        String queueName = channel.queueDeclare("", false, true, true, null).getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "");

        System.out.println("[" + ID + "] Escuchando exchange '" + EXCHANGE_NAME + "' (fanout)");
        System.out.println("[" + ID + "] Esperando TransIAP-loc/JSON...\n");

        DefaultConsumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {

                String msg = new String(body, StandardCharsets.UTF_8);
                System.out.println("[" + ID + "] Recibido:\n" + msg + "\n");
            }
        };

        channel.basicConsume(queueName, true, consumer);
    }
}
