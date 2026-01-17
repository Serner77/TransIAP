package registro;

import com.rabbitmq.client.*;
import dao.DAOFactory;
import dao.LocalizacionGPSDAO;
import dao.TrasladoDAO;
import domain.LocalizacionGPS;
import domain.Traslado;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegistroBD {
    
    private static final String RABBITMQ_EXCHANGE = "traslados.localizaciones";
    private static final String RABBITMQ_EXCHANGE_TYPE = "fanout";
    private static final String RABBITMQ_QUEUE = "cola.registro.bd";
    
    public static void main(String[] args) {
        System.out.println("  REGISTRO BD STC - INICIANDO");
        
        if (args.length != 2) {
            System.out.println("Uso: java registro.RegistroBD <broker> <id>");
            System.out.println("Ej: java registro.RegistroBD localhost REG1");
            return;
        }
        
        String broker = args[0];
        String consumerId = args[1];
        
        RegistroBD servicio = new RegistroBD();
        
        try {
            servicio.iniciarConsumidor(broker, consumerId);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void iniciarConsumidor(String broker, final String consumerId) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(broker);
        
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        
        channel.exchangeDeclare(RABBITMQ_EXCHANGE, RABBITMQ_EXCHANGE_TYPE, false);
        
        String queueName = channel.queueDeclare(RABBITMQ_QUEUE, true, false, false, null).getQueue();
        channel.queueBind(queueName, RABBITMQ_EXCHANGE, "");
        
        System.out.println("[RabbitMQ] Conectado a: " + broker);
        System.out.println("[RabbitMQ] Suscrito a exchange: " + RABBITMQ_EXCHANGE);
        System.out.println("[RabbitMQ] Cola: " + queueName);
        System.out.println("[" + consumerId + "] Esperando mensajes...");
        
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                     AMQP.BasicProperties properties, byte[] body) 
                                     throws IOException {
                
                String mensaje = new String(body, StandardCharsets.UTF_8);
                
                try {
                    String[] datos = extraerDatos(mensaje);
                    
                    if (datos == null) {
                        System.err.println("[" + consumerId + "] Formato incorrecto");
                        channel.basicNack(envelope.getDeliveryTag(), false, false);
                        return;
                    }
                    
                    String matricula = datos[0];
                    double latitud = Double.parseDouble(datos[1]);
                    double longitud = Double.parseDouble(datos[2]);
                    
                    boolean exito = guardarEnBD(matricula, latitud, longitud);
                    
                    if (exito) {
                        System.out.println("[" + consumerId + "] Guardado: " + matricula);
                        channel.basicAck(envelope.getDeliveryTag(), false);
                    } else {
                        System.err.println("[" + consumerId + "] Error persistente - Descartando");
                        channel.basicNack(envelope.getDeliveryTag(), false, false);
                    }
                    
                } catch (NumberFormatException e) {
                    System.err.println("[" + consumerId + "] Error numerico");
                    channel.basicNack(envelope.getDeliveryTag(), false, false);
                } catch (Exception e) {
                    System.err.println("[" + consumerId + "] ERROR: " + e.getMessage());
                    
                    if (e.getMessage() != null && 
                        (e.getMessage().contains("connect") || 
                         e.getMessage().contains("Connection"))) {
                        channel.basicNack(envelope.getDeliveryTag(), false, true);
                    } else {
                        channel.basicNack(envelope.getDeliveryTag(), false, false);
                    }
                }
            }
        };
        
        channel.basicConsume(queueName, false, consumer);
        
        System.out.println("[" + consumerId + "] Servicio activo.");
        
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                channel.close();
                connection.close();
                break;
            }
        }
    }
    
    private String[] extraerDatos(String json) {
        try {
            String matricula = "";
            String latitud = "";
            String longitud = "";
            
            Pattern patVehiculo = Pattern.compile("\"vehiculo\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matVehiculo = patVehiculo.matcher(json);
            if (matVehiculo.find()) {
                matricula = matVehiculo.group(1);
            }
            
            Pattern patCoordenadas = Pattern.compile("\"coordenadas\"\\s*:\\s*\\{[^}]*\"latitud\"\\s*:\\s*([-0-9.]+)[^}]*\"longitud\"\\s*:\\s*([-0-9.]+)");
            Matcher matCoords = patCoordenadas.matcher(json);
            if (matCoords.find()) {
                latitud = matCoords.group(1);
                longitud = matCoords.group(2);
            }
            
            if (matricula.isEmpty() || latitud.isEmpty() || longitud.isEmpty()) {
                return null;
            }
            
            return new String[]{matricula, latitud, longitud};
            
        } catch (Exception e) {
            return null;
        }
    }
    
    private boolean guardarEnBD(String matricula, double latitud, double longitud) {
        try {
            DAOFactory daoFactory = DAOFactory.getCurrentInstance();
            daoFactory.connect("localhost", "3306", "root", "", "stc");
            
            LocalizacionGPSDAO localizacionGPSDAO = daoFactory.getLocalizacionGPSDAO();
            TrasladoDAO trasladoDAO = daoFactory.getTrasladoDAO();
            
            Traslado traslado = trasladoDAO.getTrasladoActivoPorVehiculo(matricula);
            
            if (traslado == null) {
                System.err.println("No hay traslado activo para: " + matricula);
                return false;
            }
            
            LocalizacionGPS localizacion = new LocalizacionGPS();
            localizacion.setLatitud(latitud);
            localizacion.setLongitud(longitud);
            localizacion.setTraslado(traslado);
            
            localizacionGPSDAO.saveLocalizacionGPS(localizacion);
            
            trasladoDAO.updateUltimaLocalizaionTraslado(traslado, localizacion);
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Error BD: " + e.getMessage());
            
            if (e.getMessage() != null) {
                String msg = e.getMessage().toLowerCase();
                if (msg.contains("no hay traslado") || 
                    msg.contains("matricula") || 
                    msg.contains("not found") ||
                    msg.contains("no existe")) {
                    return false;
                }
            }
            
            throw e;
        }
    }
}