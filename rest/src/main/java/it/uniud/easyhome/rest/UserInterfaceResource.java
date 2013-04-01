package it.uniud.easyhome.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.JsonUtils;
import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.devices.DeviceType;
import it.uniud.easyhome.devices.FunctionalityType;
import it.uniud.easyhome.devices.HomeAutomationDevice;
import it.uniud.easyhome.devices.LocationType;
import it.uniud.easyhome.devices.Manufacturer;
import it.uniud.easyhome.devices.PersistentInfo;
import it.uniud.easyhome.gateway.ProtocolType;
import it.uniud.easyhome.network.NetworkEvent;
import it.uniud.easyhome.network.NetworkJobType;
import it.uniud.easyhome.network.Node;
import it.uniud.easyhome.network.NodeLogicalType;
import it.uniud.easyhome.packets.natives.NodeNeighReqPacket;
import it.uniud.easyhome.packets.natives.NodePowerLevelSetIssuePacket;
import it.uniud.easyhome.processing.*;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.*;
import javax.ws.rs.*;

import org.codehaus.jettison.json.JSONException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

@Path(RestPaths.ADMIN)
public class UserInterfaceResource {
    
	private static final String TARGET = "http://localhost:8080/easyhome/rest/";
	private static Client client = Client.create();
	private static final byte XBEE_GATEWAY_ID = (byte)2;
	private static final int XBEE_GATEWAY_PORT = 5100;
	private static final byte SIPRO_GATEWAY_ID = (byte)3;
	private static final int SIPRO_GATEWAY_PORT = 5101;
	
	private Connection jmsConnection = null;
	protected Context jndiContext = null;
	protected Session jmsSession = null;

    private ClientResponse insertGateway(byte id, int port, ProtocolType protocol) {
    	
    	return insertGateway(id,port,protocol,null);
    }
	
    private ClientResponse insertGateway(byte id, int port, ProtocolType protocol, LogLevel logLevel) {
        
    	MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
    	formData.add("id", Byte.toString(id));
    	formData.add("port", String.valueOf(port));
    	formData.add("protocol", protocol.toString());
    	if (logLevel != null)
    		formData.add("logLevel", logLevel.toString());
    	ClientResponse response = client.resource(TARGET).path(RestPaths.GATEWAYS)
    							  .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
    	
    	return response;
    }
    
    private ClientResponse insertProcess(ProcessKind kind, LogLevel logLevel) {
        
    	MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
    	formData.add("kind", kind.toString());
    	if (logLevel != null)
    		formData.add("logLevel", logLevel.toString());
    	ClientResponse response = client.resource(TARGET).path(RestPaths.PROCESSES)
    							  .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
    	
    	return response;
    }
    
    private ClientResponse insertProcess(ProcessKind kind) {
    	return insertProcess(kind, null);
    }
    
    // curl -X POST http://localhost:8080/easyhome/rest/ui/changePower -H "Content-Type: application/x-www-form-urlencoded" --data-binary "gatewayId=2&address=0&powerLevel=3"
    @Path("/changePower") 
    @POST
    public Response changePower(@FormParam("gatewayId") byte gatewayId, 
    						   @FormParam("address") short address,
    						   @FormParam("powerLevel") byte powerLevel) throws JSONException, JMSException, NamingException {
    	
    	if (powerLevel < 0 || powerLevel > 4)
    		throw new WebApplicationException(Response.Status.BAD_REQUEST);
    	
    	ClientResponse nodeResponse = client.resource(TARGET).path(RestPaths.NODES).path(Byte.toString(gatewayId)).path(Short.toString(address)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
    	if (nodeResponse.getClientResponseStatus() == ClientResponse.Status.NOT_FOUND)
    		throw new WebApplicationException(Response.Status.NOT_FOUND);
    	
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("type",NetworkJobType.NODE_POWER_LEVEL_SET_ISSUE.toString());
        formData.add("gatewayId",Byte.toString(gatewayId));
        formData.add("address",Short.toString(address));
        formData.add("tsn",Byte.toString((byte)0));
        formData.add("payload",Byte.toString(powerLevel));       
        client.resource(TARGET).path(RestPaths.JOBS).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
        
   		jndiContext = new InitialContext();
        ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup(JMSConstants.CONNECTION_FACTORY);
        
        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
        
        jmsConnection = connectionFactory.createConnection();
        jmsSession = jmsConnection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = jmsSession.createProducer(networkEventsTopic);
            
        jmsConnection.start();
        
        NetworkEvent event = new NetworkEvent(NetworkEvent.EventKind.NODE_POWER_LEVEL_SET_ISSUE,gatewayId,address,new byte[]{powerLevel});
 	    ObjectMessage outboundMessage = jmsSession.createObjectMessage(event);
    	producer.send(outboundMessage);    
    	
    	jmsConnection.close();
    	
        return Response.ok().build();    	
    }
    
    @Path("/minimizeGraph") 
    @POST
    public Response minimizeGraph() throws JSONException, JMSException, NamingException {
        	
   		jndiContext = new InitialContext();
        ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup(JMSConstants.CONNECTION_FACTORY);
        
        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
        
        jmsConnection = connectionFactory.createConnection();
        jmsSession = jmsConnection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = jmsSession.createProducer(networkEventsTopic);
            
        jmsConnection.start();
        
        NetworkEvent event = new NetworkEvent(NetworkEvent.EventKind.NETWORK_GRAPH_MINIMIZATION,(byte)0,(short)0);
 	    ObjectMessage outboundMessage = jmsSession.createObjectMessage(event);
    	producer.send(outboundMessage);    
    	
    	jmsConnection.close();
    	
        return Response.ok().build();    	
    }
    
    @Path("/up")
    @POST
    public Response up() {
    	
    	insertLocationsAndDevices();
		insertGateway(XBEE_GATEWAY_ID, XBEE_GATEWAY_PORT, ProtocolType.XBEE, LogLevel.INFO);
		insertGateway(SIPRO_GATEWAY_ID, SIPRO_GATEWAY_PORT, ProtocolType.SIPRO, LogLevel.DEBUG);
		insertProcess(ProcessKind.NODE_ANNCE_REGISTRATION);
		insertProcess(ProcessKind.NODE_DESCR_REQUEST);
		insertProcess(ProcessKind.NODE_DESCR_REGISTRATION);	
		insertProcess(ProcessKind.ACTIVE_ENDPOINTS_REQUEST);
		insertProcess(ProcessKind.ACTIVE_ENDPOINTS_REGISTRATION);
		insertProcess(ProcessKind.SIMPLE_DESCR_REQUEST);
		insertProcess(ProcessKind.SIMPLE_DESCR_REGISTRATION);
		insertProcess(ProcessKind.NODE_DISCOVERY_REQUEST, LogLevel.DEBUG);
		insertProcess(ProcessKind.NODE_DISCOVERY_REGISTRATION, LogLevel.DEBUG);
		insertProcess(ProcessKind.NODE_POWER_LEVEL_REQUEST);
		insertProcess(ProcessKind.NODE_POWER_LEVEL_REGISTRATION);
		insertProcess(ProcessKind.NODE_POWER_LEVEL_SET_ISSUE);
		insertProcess(ProcessKind.NODE_POWER_LEVEL_SET_ACKNOWLEDGMENT);
		insertProcess(ProcessKind.NETWORK_GRAPH_MINIMIZATION);
		insertProcess(ProcessKind.NETWORK_UPDATE);
		
		insertProcess(ProcessKind.LAMP_STATE_UPDATE, LogLevel.DEBUG);
		
		//insertProcess(ProcessKind.OCCUPANCY_REQUEST, LogLevel.DEBUG);
		insertProcess(ProcessKind.OCCUPANCY_REGISTRATION, LogLevel.DEBUG);
		
		insertProcess(ProcessKind.LIGHT_LEVEL_CONTROL,LogLevel.DEBUG);
		
		//insertProcess(ProcessKind.ALARM_STATE_REQUEST);//,LogLevel.DEBUG);
		insertProcess(ProcessKind.ALARM_STATE_ACKNOWLEDGMENT, LogLevel.DEBUG);
		
		
		
        return Response.ok().build();
    }
    
    private void insertLocationsAndDevices() {
    	
    	try {
	    	
    		List<String> locs = new ArrayList<String>();
	    	locs.add("Salotto");
	    	locs.add("Cucina");
	    	locs.add("Camera da letto doppia");
	    	locs.add("Camera da letto singola");
	    	locs.add("Bagno piccolo");
	    	locs.add("Bagno grande");
	    	
	        MultivaluedMap<String,String> formData;
	        ClientResponse response;
	        Node node;
	        List<Byte> endpoints;
	        
	        // Locations
	        
	        formData = new MultivaluedMapImpl();
	        formData.add("name",locs.get(0));
	        formData.add("type",LocationType.LIVINGROOM.toString());
	        formData.add("imgPath","img/livingroom.svg");
	        client.resource(TARGET).path(RestPaths.LOCATIONS).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	
	        formData = new MultivaluedMapImpl();
	        formData.add("name",locs.get(1));
	        formData.add("type",LocationType.KITCHEN.toString());
	        formData.add("imgPath","img/kitchen.svg");
	        client.resource(TARGET).path(RestPaths.LOCATIONS).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        
	        formData = new MultivaluedMapImpl();
	        formData.add("name",locs.get(2));
	        formData.add("type",LocationType.BEDROOM.toString());
	        formData.add("imgPath","img/doublebedroom.svg");        
	        client.resource(TARGET).path(RestPaths.LOCATIONS).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	 
	        formData = new MultivaluedMapImpl();
	        formData.add("name",locs.get(3));
	        formData.add("type",LocationType.BEDROOM.toString());
	        formData.add("imgPath","img/bedroom.svg");        
	        client.resource(TARGET).path(RestPaths.LOCATIONS).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        
	        formData = new MultivaluedMapImpl();
	        formData.add("name",locs.get(4));
	        formData.add("type",LocationType.BATHROOM.toString());
	        formData.add("imgPath","img/wc.svg");
	        client.resource(TARGET).path(RestPaths.LOCATIONS).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        
	        formData = new MultivaluedMapImpl();
	        formData.add("name",locs.get(5));
	        formData.add("type",LocationType.BATHROOM.toString());
	        formData.add("imgPath","img/bathroom.svg");        
	        client.resource(TARGET).path(RestPaths.LOCATIONS).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        
	        // PLC devices
	    	
	        // Lampada salotto

	        formData = new MultivaluedMapImpl();
	        formData.add("name","Lampada");
	        formData.add("locationName",locs.get(0));
	        formData.add("deviceType", DeviceType.COLORED_LAMP.toString());
	        formData.add("imgPath","img/colorlight.svg");
	        formData.add("help", "E' possibile cambiare luce o colore della lampada");
	    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)3)).path(Long.toString(0x524742L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	response = client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)3)).path(Long.toString(0x524742L)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	        long lampadaSalottoInfoId = JsonUtils.getFrom(response, PersistentInfo.class).getId();
	        
	        // Lampada camera doppia
	        
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Lampada");
	        formData.add("locationName",locs.get(2));
	        formData.add("deviceType", DeviceType.COLORED_LAMP.toString());
	        formData.add("imgPath","img/colorlight.svg");
	        formData.add("help", "E' possibile cambiare luce o colore della lampada");
	    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)3)).path(Long.toString(0x424752L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	response = client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)3)).path(Long.toString(0x424752L)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	        long lampadaCameraDoppiaInfoId = JsonUtils.getFrom(response, PersistentInfo.class).getId();
	        
	        // Frigo
	        
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Frigo (PLC)");
	        formData.add("locationName",locs.get(1));
	        formData.add("imgPath","img/fridge.svg");
	        formData.add("help", "Nessuna funzione correntemente disponibile");
	    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)3)).path(Long.toString(0x101010L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	response = client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)3)).path(Long.toString(0x101010L)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	    	long frigoPLCInfoId = JsonUtils.getFrom(response, PersistentInfo.class).getId();   	
	        
	        // ZigBee devices
	        
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Frigo");
	        formData.add("locationName",locs.get(1));
	        formData.add("imgPath","img/fridge.svg");
	        formData.add("help", "Nessuna funzione correntemente disponibile");
	    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)2)).path(Long.toString(5526146523928327L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	response = client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)2)).path(Long.toString(5526146523928327L)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	    	long frigoZigBeeInfoId = JsonUtils.getFrom(response, PersistentInfo.class).getId();	 
	    	
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Gateway ZigBee");
	        formData.add("locationName",locs.get(0));
	        formData.add("imgPath","img/accesspoint.svg");
	        formData.add("help", "E' possibile spostare il dispositivo in un'altra stanza");
	    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)2)).path(Long.toString(5526146521326185L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        response = client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)2)).path(Long.toString(5526146521326185L)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	        long zigbeeGatewayInfoId = JsonUtils.getFrom(response, PersistentInfo.class).getId();
	    	
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Interfaccia gestuale");
	        formData.add("locationName",locs.get(0));
	        formData.add("deviceType", DeviceType.HAND_CONTROLLER.toString());
	        formData.add("imgPath","img/hand.svg");
	        formData.add("help", "E' possibile cambiare la lampada associata all'interfaccia e spostare il dispositivo in un'altra stanza");
	        client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)2)).path(Long.toString(5526146523928176L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        response = client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)2)).path(Long.toString(5526146523928176L)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	        long gestualInfoId = JsonUtils.getFrom(response, PersistentInfo.class).getId();     
	    	
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Interfaccia gestuale (test)");
	        formData.add("locationName",locs.get(2));
	        formData.add("deviceType", DeviceType.HAND_CONTROLLER.toString());
	        formData.add("imgPath","img/hand.svg");
	        formData.add("help", "E' possibile cambiare la lampada associata all'interfaccia e spostare il dispositivo in un'altra stanza");
	    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)2)).path(Long.toString(5526146523928181L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        response = client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)2)).path(Long.toString(5526146523928181L)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	        long testGestualInfoId = JsonUtils.getFrom(response, PersistentInfo.class).getId();
	    	
	    	// PowerLine
	    	
	        formData = new MultivaluedMapImpl();
	        formData.add("gatewayId",Byte.toString((byte)3));
	        formData.add("nuid",Long.toString(0x100L));
	        formData.add("address",Short.toString((short)0x100));
	        formData.add("permanent",Boolean.toString(true));
	        client.resource(TARGET).path(RestPaths.NODES).path("insert").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Gateway Powerline");
	        formData.add("locationName",locs.get(0));
	        formData.add("imgPath","img/accesspoint.svg");
	        formData.add("help", "E' possibile spostare il dispositivo in un'altra stanza");
	    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)3)).path(Long.toString(0x100L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        response = client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)3)).path(Long.toString(0x100L)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	        long plGatewayId = JsonUtils.getFrom(response, PersistentInfo.class).getId();
	    	
	    	long timerInfoId = addFakeDevices(locs);
	    	
	    	// Functionalities
	    	
	        formData = new MultivaluedMapImpl();
	        formData.add("name","Associazione a lampada");
	        formData.add("type",FunctionalityType.PAIRING.toString());
	        formData.add("imgPath","img/link.svg");
	        formData.add("help", "Scegli una lampada a cui associare l'interfaccia gestuale. La precedente associazione verrà annullata");
	        formData.putSingle("infoId",Long.toString(testGestualInfoId));
	    	client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	formData.putSingle("infoId",Long.toString(gestualInfoId));
	    	client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	
	    	formData = new MultivaluedMapImpl();
	        formData.add("name","Luminosità");
	        formData.add("type",FunctionalityType.LUMINOSITY_CONTROL.toString());
	        formData.add("imgPath","img/sun.svg");
	        formData.add("help", "Seleziona il livello di luminosità preferito, da 1 tacca (0%) a 9 tacche (100%)");
	        formData.putSingle("infoId",Long.toString(lampadaCameraDoppiaInfoId));
	    	client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	formData.putSingle("infoId",Long.toString(lampadaSalottoInfoId));
	    	client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	
	    	formData = new MultivaluedMapImpl();
	        formData.add("name","Colore");
	        formData.add("type",FunctionalityType.COLOR_CONTROL.toString());
	        formData.add("imgPath","img/colorpalette.svg");
	        formData.add("help", "Seleziona l'intensità colore per rosso(R), verde(G) e blu(B), crescente da sinistra verso destra");
	        formData.putSingle("infoId",Long.toString(lampadaCameraDoppiaInfoId));
	    	client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        formData.putSingle("infoId",Long.toString(lampadaSalottoInfoId));
	    	client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	
	    	formData = new MultivaluedMapImpl();
	        formData.add("name","Sensore presenza");
	        formData.add("type",FunctionalityType.OCCUPATION_SENSING.toString());
	        formData.add("imgPath","img/person.svg");
	        formData.add("help", "Nessuna funzione prevista: è semplicemente possibile rilevare lo stato di occupazione della stanza");
	        formData.putSingle("infoId",Long.toString(lampadaCameraDoppiaInfoId));
	    	client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        formData.putSingle("infoId",Long.toString(lampadaSalottoInfoId));
	    	client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);

	    	formData = new MultivaluedMapImpl();
	        formData.add("name","Allarme");
	        formData.add("type",FunctionalityType.ALARM_ISSUING.toString());
	        formData.add("imgPath","img/bell.svg");
	        formData.add("help", "Nessuna funzione prevista: è semplicemente possibile rilevare la presenza di allarme");
	        formData.putSingle("infoId",Long.toString(frigoPLCInfoId));
	    	client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	
	    	formData = new MultivaluedMapImpl();
	        formData.add("name","Allarme");
	        formData.add("type",FunctionalityType.ALARM_ISSUING.toString());
	        formData.add("imgPath","img/bell.svg");
	        formData.add("help", "Nessuna funzione prevista: è semplicemente possibile rilevare la presenza di allarme");
	        formData.putSingle("infoId",Long.toString(frigoZigBeeInfoId));
	    	client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	    	
	    	formData = new MultivaluedMapImpl();
	        formData.add("name","Spostamento");
	        formData.add("type",FunctionalityType.MOVE_ROOM.toString());
	        formData.add("imgPath","img/move.svg");
	        formData.add("help", "Seleziona la nuova stanza in cui spostare il dispositivo");
	        formData.add("infoId",Long.toString(zigbeeGatewayInfoId));
	    	client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        formData.putSingle("infoId",Long.toString(plGatewayId));
	        client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        formData.putSingle("infoId",Long.toString(testGestualInfoId));
	        client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        formData.putSingle("infoId",Long.toString(gestualInfoId));
	        client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        formData.putSingle("infoId",Long.toString(timerInfoId));
	        client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
	        
	        addStates(lampadaSalottoInfoId,lampadaCameraDoppiaInfoId,frigoPLCInfoId,frigoZigBeeInfoId);

            
    	} catch (JSONException ex) {
    		down();
    		throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    	}
    }
    
    private long addFakeDevices(List<String> locs) throws JSONException {
    	
    	MultivaluedMap<String,String> formData;
    	ClientResponse response;
    	
    	// Forno
    	
        formData = new MultivaluedMapImpl();
        formData.add("gatewayId",Byte.toString((byte)4));
        formData.add("nuid",Long.toString(1L));
        formData.add("address",Short.toString((short)1));
        formData.add("permanent",Boolean.toString(true));
        client.resource(TARGET).path(RestPaths.NODES).path("insert").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
    	
        formData = new MultivaluedMapImpl();
        formData.add("name","Forno");
        formData.add("locationName",locs.get(1));
        formData.add("imgPath","img/oven.svg");
        formData.add("help", "Nessuna funzione correntemente disponibile (dispositivo fittizio)");
    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)4)).path(Long.toString(1L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
    	
    	// Macchina del caffè
    	
        formData = new MultivaluedMapImpl();
        formData.add("gatewayId",Byte.toString((byte)4));
        formData.add("nuid",Long.toString(2L));
        formData.add("address",Short.toString((short)2));
        formData.add("permanent",Boolean.toString(true));
        client.resource(TARGET).path(RestPaths.NODES).path("insert").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
    	
        formData = new MultivaluedMapImpl();
        formData.add("name","Macchina del caffè");
        formData.add("locationName",locs.get(1));
        formData.add("imgPath","img/coffee.svg");
        formData.add("help", "Nessuna funzione correntemente disponibile (dispositivo fittizio)");
    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)4)).path(Long.toString(2L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);

    	// Tostapane
    	
        formData = new MultivaluedMapImpl();
        formData.add("gatewayId",Byte.toString((byte)4));
        formData.add("nuid",Long.toString(3L));
        formData.add("address",Short.toString((short)3));
        formData.add("permanent",Boolean.toString(true));
        client.resource(TARGET).path(RestPaths.NODES).path("insert").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);

        formData = new MultivaluedMapImpl();
        formData.add("name","Tostapane");
        formData.add("locationName",locs.get(1));
        formData.add("imgPath","img/bread.svg");
        formData.add("help", "Nessuna funzione correntemente disponibile (dispositivo fittizio)");
    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)4)).path(Long.toString(3L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);

    	// Televisore
    	
        formData = new MultivaluedMapImpl();
        formData.add("gatewayId",Byte.toString((byte)4));
        formData.add("nuid",Long.toString(4L));
        formData.add("address",Short.toString((short)4));
        formData.add("permanent",Boolean.toString(true));
        client.resource(TARGET).path(RestPaths.NODES).path("insert").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
    	
        formData = new MultivaluedMapImpl();
        formData.add("name","Televisore");
        formData.add("locationName",locs.get(0));
        formData.add("imgPath","img/tv.svg");
        formData.add("help", "Nessuna funzione correntemente disponibile (dispositivo fittizio)");
    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)4)).path(Long.toString(4L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);

    	// HiFi
    	
        formData = new MultivaluedMapImpl();
        formData.add("gatewayId",Byte.toString((byte)4));
        formData.add("nuid",Long.toString(5L));
        formData.add("address",Short.toString((short)5));
        formData.add("permanent",Boolean.toString(true));
        client.resource(TARGET).path(RestPaths.NODES).path("insert").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
    	
        formData = new MultivaluedMapImpl();
        formData.add("name","HiFi");
        formData.add("locationName",locs.get(0));
        formData.add("imgPath","img/hifi.svg");
        formData.add("help", "Nessuna funzione correntemente disponibile (dispositivo fittizio)");
    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)4)).path(Long.toString(5L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
    	
    	// Termostato
    	
        formData = new MultivaluedMapImpl();
        formData.add("gatewayId",Byte.toString((byte)4));
        formData.add("nuid",Long.toString(6L));
        formData.add("address",Short.toString((short)6));
        formData.add("permanent",Boolean.toString(true));
        client.resource(TARGET).path(RestPaths.NODES).path("insert").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
    	
        formData = new MultivaluedMapImpl();
        formData.add("name","Termostato");
        formData.add("locationName",locs.get(0));
        formData.add("imgPath","img/thermostat.svg");
        formData.add("help", "Nessuna funzione correntemente disponibile (dispositivo fittizio)");
    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)4)).path(Long.toString(6L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
    	
    	// Sveglia
    	
        formData = new MultivaluedMapImpl();
        formData.add("gatewayId",Byte.toString((byte)4));
        formData.add("nuid",Long.toString(7L));
        formData.add("address",Short.toString((short)7));
        formData.add("permanent",Boolean.toString(true));
        client.resource(TARGET).path(RestPaths.NODES).path("insert").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
    	
        formData = new MultivaluedMapImpl();
        formData.add("name","Sveglia");
        formData.add("locationName",locs.get(3));
        formData.add("imgPath","img/timer.svg");
        formData.add("help", "E' possibile spostare il dispositivo in un'altra stanza (dispositivo fittizio)");
    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)4)).path(Long.toString(7L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
    	response = client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)4)).path(Long.toString(7L)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        long timerInfoId = JsonUtils.getFrom(response, PersistentInfo.class).getId();
    	
        // Lavatrice
        
        formData = new MultivaluedMapImpl();
        formData.add("gatewayId",Byte.toString((byte)4));
        formData.add("nuid",Long.toString(8L));
        formData.add("address",Short.toString((short)8));
        formData.add("permanent",Boolean.toString(true));
        client.resource(TARGET).path(RestPaths.NODES).path("insert").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
        
        formData = new MultivaluedMapImpl();
        formData.add("name","Lavatrice");
        formData.add("locationName",locs.get(5));
        formData.add("imgPath","img/washer.svg");
        formData.add("help", "Nessuna funzione correntemente disponibile (dispositivo fittizio)");
    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).path(Byte.toString((byte)4)).path(Long.toString(8L)).type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
    	
    	return timerInfoId;
    }
    
    private void addStates(long lampadaSalottoInfoId, long lampadaCameraInfoId, long frigoPLCInfoId, long frigoZigBeeInfoId) {

        client.resource(TARGET).path(RestPaths.STATES).path("lamps").path(Long.toString(lampadaSalottoInfoId)).put(ClientResponse.class);
        client.resource(TARGET).path(RestPaths.STATES).path("lamps").path(Long.toString(lampadaCameraInfoId)).put(ClientResponse.class);
        client.resource(TARGET).path(RestPaths.STATES).path("fridges").path(Long.toString(frigoPLCInfoId)).put(ClientResponse.class);
        client.resource(TARGET).path(RestPaths.STATES).path("fridges").path(Long.toString(frigoZigBeeInfoId)).put(ClientResponse.class);
        client.resource(TARGET).path(RestPaths.STATES).path("sensors").path("presence").path(Long.toString(lampadaSalottoInfoId)).put(ClientResponse.class);
        client.resource(TARGET).path(RestPaths.STATES).path("sensors").path("presence").path(Long.toString(lampadaCameraInfoId)).put(ClientResponse.class);
    }
    
    private void stopEventBasedJSFProcesses() throws NamingException, JMSException {
    	
   		jndiContext = new InitialContext();
        ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup(JMSConstants.CONNECTION_FACTORY);
        
        Topic networkEventsTopic = (Topic) jndiContext.lookup(JMSConstants.NETWORK_EVENTS_TOPIC);
        
        jmsConnection = connectionFactory.createConnection();
        jmsSession = jmsConnection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = jmsSession.createProducer(networkEventsTopic);
            
        jmsConnection.start();
        
 	    ObjectMessage nullMessage = jmsSession.createObjectMessage(null);
    	producer.send(nullMessage);    
    	
    	jmsConnection.close();
    }

    @Path("/down")
    @POST
    public Response down() {
    	
    	client.resource(TARGET).path(RestPaths.GATEWAYS).delete();
    	client.resource(TARGET).path(RestPaths.PROCESSES).delete();
    	client.resource(TARGET).path(RestPaths.JOBS).delete();
    	client.resource(TARGET).path(RestPaths.LINKS).delete();
    	client.resource(TARGET).path(RestPaths.FUNCTIONALITIES).delete();
    	client.resource(TARGET).path(RestPaths.PAIRINGS).delete();
    	client.resource(TARGET).path(RestPaths.STATES).delete();
    	client.resource(TARGET).path(RestPaths.NODES).delete();
    	client.resource(TARGET).path(RestPaths.PERSISTENTINFO).delete();
    	client.resource(TARGET).path(RestPaths.LOCATIONS).delete();
    	
    	try {
    		stopEventBasedJSFProcesses();
    	} catch (Exception ex) { }
    	
        return Response.ok().build();
    }
    
}
