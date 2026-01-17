package lib;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import java.util.Map;
import java.util.UUID;

public class DirectionIdTransformer extends AbstractMessageTransformer {
    
    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) 
            throws TransformerException {
        
        String directionId;
        
        try {
            Map<String, Object> contenedor = (Map<String, Object>) message.getInvocationProperty("contenedor");
            
            String direccion = (String) contenedor.get("trasladoDireccion");
            String codigoPostal = (String) contenedor.get("trasladoCodigoPostal");
            String ciudad = (String) contenedor.get("trasladoCiudad");
            String pais = (String) contenedor.get("trasladoPais");
            
            String directionKey = (direccion + "|" + codigoPostal + "|" + ciudad + "|" + pais).toLowerCase();
            directionId = "dir_" + Math.abs(directionKey.hashCode());
            
            System.out.println("Persistent Direction ID: " + directionId + " for: " + directionKey);
            
        } catch (Exception e) {
            directionId = UUID.randomUUID().toString();
            System.out.println("Error obteniendo datos, usando UUID: " + directionId);
            e.printStackTrace();
        }
        
        message.setInvocationProperty("generatedDirectionId", directionId);
        
        return message;
    }
}