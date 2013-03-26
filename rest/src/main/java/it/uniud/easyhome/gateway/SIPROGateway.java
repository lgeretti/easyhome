package it.uniud.easyhome.gateway;

import it.uniud.easyhome.common.ByteUtils;
import it.uniud.easyhome.common.Endianness;
import it.uniud.easyhome.common.JMSConstants;
import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.common.RunnableState;
import it.uniud.easyhome.contexts.EasyHomeContext;
import it.uniud.easyhome.contexts.HomeAutomationContext;
import it.uniud.easyhome.devices.states.ColoredAlarm;
import it.uniud.easyhome.devices.states.FridgeCode;
import it.uniud.easyhome.network.ModuleCoordinates;
import it.uniud.easyhome.packets.Domain;
import it.uniud.easyhome.packets.Operation;
import it.uniud.easyhome.packets.natives.AlarmStateReqPacket;
import it.uniud.easyhome.packets.natives.AlarmStateRspPacket;
import it.uniud.easyhome.packets.natives.LampStateSetPacket;
import it.uniud.easyhome.packets.natives.NativePacket;
import it.uniud.easyhome.packets.natives.OccupancyAttributeReqPacket;
import it.uniud.easyhome.packets.natives.OccupancyAttributeRspPacket;
import it.uniud.easyhome.rest.RestPaths;

import java.io.*;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class SIPROGateway extends Gateway {
	
	private static final String SIPRO_TARGET = "http://localhost:5000/";
	private static final String INTERNAL_TARGET = "http://localhost:8080/easyhome/rest/";
	
	private static final int DISCOVERY_PERIOD_MS = 5000; 
	
	private static final boolean MOCKED_GATEWAY = true;
	
	private static Client client = Client.create();
	
	private Random rnd;
	
	private volatile Set<Long> sensorsRegistered = new HashSet<Long>();
	private volatile Set<Long> actuatorsRegistered = new HashSet<Long>();
	private List<Long> actuators = new ArrayList<Long>();
	private List<Long> sensors = new ArrayList<Long>();
	
	private long lastDiscoveryTimeMillis = 0;
	
    public SIPROGateway(byte id, int port, LogLevel logLevel) {
    	super(id,ProtocolType.NATIVE,port,logLevel);
    	
    	actuators.add(Long.parseLong("424752",16));
    	actuators.add(Long.parseLong("524742",16));
    	sensors.add(Long.parseLong("424752",16));
    	sensors.add(Long.parseLong("524742",16));
    	sensors.add(Long.parseLong("101010",16));
    	
    	rnd = new Random();
    }
    
    @Override
    public final void open() {

        Thread thr = new Thread(this);
        thr.start();
    }
    
    @Override
    final protected NativePacket readFrom(InputStream is, ByteArrayOutputStream buffer) throws IOException {
    	
    	NativePacket result = null;
    	return result;
    }
    
    @Override
    public void run() {
    	
    	state = RunnableState.STARTED;

        while (state != RunnableState.STOPPING) {
        	
        	Connection jmsConnection = null;
        	
        	try {
                
    	   		Context jndiContext = new InitialContext();
    	        ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup(JMSConstants.CONNECTION_FACTORY);
    	        
                Topic outboundTopic = (Topic) jndiContext.lookup(JMSConstants.OUTBOUND_PACKETS_TOPIC);
                Topic inboundTopic = (Topic) jndiContext.lookup(JMSConstants.INBOUND_PACKETS_TOPIC);
                
    	        jmsConnection = connectionFactory.createConnection();
    	        Session jmsSession = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                
                MessageConsumer outboundConsumer = jmsSession.createConsumer(outboundTopic);
                MessageProducer inboundProducer = jmsSession.createProducer(inboundTopic);
                
                jmsConnection.start();
                
                log(LogLevel.INFO, "Gateway is opened");
                
                while (state != RunnableState.STOPPING) {
                	
                	if (canTryToDiscoverNewDevices()) {
                		tryToAwakeActuator();
                		registerNewDevices();
                	}
                    handleOutboundPacketsTo(null,outboundConsumer,jmsSession,inboundProducer);
                }
                
            } catch (SocketException ex) {
            	// We do not want errors to show when close() is called during operations
            } catch (Exception ex) {
            	ex.printStackTrace();
            } finally {
	        	  try {
	        		  if (jmsConnection != null)
	        			  jmsConnection.close();
	        	  } catch (JMSException ex) {
	        		// Whatever the case, the connection is not available anymore  
	        	  }
            }
        }
        state = RunnableState.STOPPED;
        log(LogLevel.INFO, "Gateway is closed");
    }
    
    @Override
    final protected void write(NativePacket pkt, OutputStream os, Session jmsSession, MessageProducer producer) throws IOException {
    	
		if (LampStateSetPacket.validates(pkt)) {
			
			LampStateSetPacket statePacket = (LampStateSetPacket) pkt;
			
			String paramsString = "output;changeColor"+statePacket.getSeparatedParameters();
			
			log(LogLevel.FINE,"Request to set lamp state values");
			log(LogLevel.ULTRAFINE,"Request: ?method=setValueParam&params="+paramsString);
			
			if (!MOCKED_GATEWAY) {		
		        MultivaluedMap<String,String> queryParams = new MultivaluedMapImpl();
		        queryParams.add("method","setValueParam");
		        queryParams.add("params",paramsString);
				client.resource(SIPRO_TARGET).queryParams(queryParams).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
			}
			
		} else if (OccupancyAttributeReqPacket.validates(pkt) || AlarmStateReqPacket.validates(pkt)) {
			
			long destinationNuid = pkt.getDstCoords().getNuid();
			
			sendCorrespondingSensorData(destinationNuid,jmsSession,producer);
		}
    }
    
    private boolean canTryToDiscoverNewDevices() {
    	
    	if (actuatorsRegistered.size() < actuators.size() || sensorsRegistered.size() < sensors.size()) {
    		if (System.currentTimeMillis() > lastDiscoveryTimeMillis + DISCOVERY_PERIOD_MS) {
    			lastDiscoveryTimeMillis = System.currentTimeMillis();
    			return true;
    		} else
    			return false;
    	} else
    		return false;
    }
    
    private void tryToAwakeActuator() {
    	int lampToChoose = -1;
    	switch (actuatorsRegistered.size()) {
    	case 0:
    		lampToChoose = rnd.nextInt(2);    		
    		break;
    	case 1:
    		lampToChoose = (actuatorsRegistered.contains(actuators.get(0)) ? 1 : 0);
    		break;
    	default:
    	}
    	if (lampToChoose >= 0) {
    		long nuid = actuators.get(lampToChoose);
    		log(LogLevel.ULTRAFINE, "Still " + (actuators.size()-actuatorsRegistered.size()) + " actuators missing : trying to awake lamp with id 0x"+Long.toHexString(nuid));
    		String awakeString = getAwakeString(nuid);
    		log(LogLevel.DEBUG,"Request: ?method=setValueParam&params="+awakeString);
			if (!MOCKED_GATEWAY) {		
		        MultivaluedMap<String,String> queryParams = new MultivaluedMapImpl();
		        queryParams.add("method","setValueParam");
		        queryParams.add("params",awakeString);
				client.resource(SIPRO_TARGET).queryParams(queryParams).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
			}
    	}
    }
    
    private static String getAwakeString(long nuid) {
    	
    	StringBuilder strb = new StringBuilder();
    	
    	String hexNuid = Long.toHexString(nuid);
    	
    	strb.append("output;changeColor;")
    		.append(hexNuid.substring(0, 2)).append(";")
    		.append(hexNuid.substring(2, 4)).append(";")
    		.append(hexNuid.substring(4, 6)).append(";")
    		.append("00;00;00;00;00");
    	
    	return strb.toString();
    }
    
    private void sendCorrespondingSensorData(long destinationNuid, Session jmsSession, MessageProducer producer) {
    	
	    try {
	    	
	    	String xmlContent = null;
	    	
	    	if (!MOCKED_GATEWAY) {
		        MultivaluedMap<String,String> queryParams = new MultivaluedMapImpl();
		        queryParams.add("method","getData");
		        queryParams.add("params","actuators");
	    		ClientResponse dataModelResponse = client.resource(SIPRO_TARGET).queryParams(queryParams).accept(MediaType.TEXT_XML).get(ClientResponse.class);
		    	xmlContent = dataModelResponse.getEntity(String.class);
	    	} else {
		    	File xmlFile = new File("/home/geretti/Public/sources/uniud/easyhome/rest/src/test/resources/sensors.xml");
		    	
		        try{
		            xmlContent = FileUtils.readFileToString(xmlFile);
		        }catch(IOException e){
		            e.printStackTrace();
		        } 
	    	}
	        
	    	InputSource is = new InputSource(new StringReader(xmlContent));
	    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	    	Document doc = dBuilder.parse(is);
	    
	    	NodeList inputs = doc.getElementsByTagName("data").item(0).getChildNodes();
	     
	    	handleSensorReply(inputs,destinationNuid,jmsSession,producer);
	    	
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }	   
    }
    
    private void registerNewDevices() {
    	
	    try {
	    	
	    	String xmlContent = null;
	    	
	    	if (!MOCKED_GATEWAY) {
		        MultivaluedMap<String,String> queryParams = new MultivaluedMapImpl();
		        queryParams.add("method","getDataModel");
		        queryParams.add("params","");
	    		ClientResponse dataModelResponse = client.resource(SIPRO_TARGET).queryParams(queryParams).accept(MediaType.TEXT_XML).get(ClientResponse.class);
		    	xmlContent = dataModelResponse.getEntity(String.class);
	    	} else {
		    	File xmlFile = new File("/home/geretti/Public/sources/uniud/easyhome/rest/src/test/resources/datamodel.xml");
		    	
		        try{
		            xmlContent = FileUtils.readFileToString(xmlFile);
		        }catch(IOException e){
		            e.printStackTrace();
		        } 
	    	}
	        
	    	InputSource is = new InputSource(new StringReader(xmlContent));
	    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	    	Document doc = dBuilder.parse(is);
	    
	    	NodeList dataCategories = doc.getElementsByTagName("data");
	     
	    	if (actuatorsRegistered.size() < actuators.size()) {
	    		handleActuators(dataCategories.item(0));
	    		if (actuatorsRegistered.size() == actuators.size())
	    			log(LogLevel.FINE,"All actuators registered");
	    	}
	    	
	    	if (sensorsRegistered.size() < sensors.size()) {
	    		handleSensors(dataCategories.item(1));
	    		if (sensorsRegistered.size() == sensors.size())
	    			log(LogLevel.FINE,"All sensors registered");
	    	}
	    	
	    	if (actuatorsRegistered.size() == actuators.size() && sensorsRegistered.size() == sensors.size())
	    		log(LogLevel.FINE,"All devices registered");
	    	
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }	
    }
    
    private void handleActuators(Node node) {
    	NodeList children = node.getChildNodes();
    	for (int i=0;i<children.getLength();i++) {
    		Node child = children.item(i);
    		if (child.getNodeType() == Node.ELEMENT_NODE) {
    			Element sensor = (Element)child;
    			long nuid = Long.parseLong(getTxtFor(sensor,"value-3") + getTxtFor(sensor,"value-2") + getTxtFor(sensor,"value-1"),16);
    			if (!actuatorsRegistered.contains(nuid)) {
    				actuatorsRegistered.add(nuid);
    				registerLamp((Element)child);
    			}
    				
    		}
    	}
    }

    private void handleSensors(Node node) {

    	NodeList children = node.getChildNodes();
    	for (int i=0;i<children.getLength();i++) {
    		Node child = children.item(i);
    		if (child.getNodeType() == Node.ELEMENT_NODE) {
    			Element sensor = (Element)child;
    			long nuid = Long.parseLong(getTxtFor(sensor,"value-3") + getTxtFor(sensor,"value-2") + getTxtFor(sensor,"value-1"),16);
    			if (!sensorsRegistered.contains(nuid)) { 
    				sensorsRegistered.add(nuid);
	    			String descrName = getDescriptorName(sensor);
	    			if (descrName.equals("Fridge Alarm Light"))
	    				registerFridge(sensor);
	    			else if (descrName.equals("PIR"))
	    				registerPIR(sensor);
    			}
    		}
    	}
    }    
    
    private void handleSensorReply(NodeList inputs, long nuid, Session jmsSession, MessageProducer producer) {
    	for (int i=0;i<inputs.getLength();i++) {
    		Node input = inputs.item(i);
    		if (input.getNodeType() == Node.ELEMENT_NODE) {
    			String descrName = getDescriptorName((Element)input);
    			if (descrName.equals("Fridge Alarm Light")) {
    				if (handleSensorReplyForFridge((Element)input,nuid,jmsSession,producer))
    					break;
    			} else if (descrName.equals("PIR")) {
    				if (handleSensorReplyForPIR((Element)input,nuid,jmsSession,producer))
    					break;
    			}
    		}
    	}
    }    
    
    private byte fromHexStringToByte(String value) {
    	return (byte)(Integer.parseInt(value,16) & 0xFF);
    }

    private void registerLamp(Element elem) {
    	boolean online = (getTxtFor(elem,"state").equals("ON"));
		long nuid = Long.parseLong(getTxtFor(elem,"value-3") + getTxtFor(elem,"value-2") + getTxtFor(elem,"value-1"),16);
		byte red = fromHexStringToByte(getTxtFor(elem,"value0"));
		byte green = fromHexStringToByte(getTxtFor(elem,"value1"));
		byte blue = fromHexStringToByte(getTxtFor(elem,"value2"));
		byte white = fromHexStringToByte(getTxtFor(elem,"value3"));
		ColoredAlarm alarm = ColoredAlarm.fromCode((byte)(Integer.parseInt(getTxtFor(elem,"value4"),16) & 0xFF));
		log(LogLevel.FINE,"Registering Lamp: online=" + online + ", nuid=0x" + Long.toHexString(nuid) + 
						   ", red=" + red + ", green=" + green + ", blue=" + blue + ", white=" + white + ", alarm=" + alarm);
		
		addNode(nuid);
		
		MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData = new MultivaluedMapImpl();
        formData.add("gatewayId",Byte.toString(this.id));
        formData.add("nuid",Long.toString(nuid));  
        formData.add("online",Boolean.toString(online));
        formData.add("red",Byte.toString(red));
        formData.add("green",Byte.toString(green));
        formData.add("blue",Byte.toString(blue));
        formData.add("white",Byte.toString(white));
        formData.add("alarm",alarm.toString());
        client.resource(INTERNAL_TARGET).path(RestPaths.STATES).path("lamps").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
    }
    
    private void registerPIR(Element elem) {
		long nuid = Long.parseLong(getTxtFor(elem,"value-3") + getTxtFor(elem,"value-2") + getTxtFor(elem,"value-1"),16);
		String occupation = getTxtFor(elem,"value0") + getTxtFor(elem,"value1");
		boolean occupied = (occupation.equals("5031"));
		log(LogLevel.FINE,"Registering PIR: nuid=0x" + Long.toHexString(nuid) + ", occupied=" + occupied);
		
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("gatewayId",Byte.toString(this.id));
        formData.add("nuid",Long.toString(nuid));  
        formData.add("occupied",Boolean.toString(occupied));
        client.resource(INTERNAL_TARGET).path(RestPaths.STATES).path("sensors/presence").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
    }
    
    private void registerFridge(Element elem) {
		long nuid = Long.parseLong(getTxtFor(elem,"value-3") + getTxtFor(elem,"value-2") + getTxtFor(elem,"value-1"),16);
		String codeString = getTxtFor(elem,"value0") + getTxtFor(elem,"value1") + getTxtFor(elem,"value2");
		FridgeCode lastCode = FridgeCode.fromCode(Short.parseShort(codeString));	
		log(LogLevel.FINE,"Registering Fridge: nuid=0x" + Long.toHexString(nuid) + ", code=" + lastCode);
		
		addNode(nuid);
		
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("gatewayId",Byte.toString(this.id));
        formData.add("nuid",Long.toString(nuid));
        formData.add("lastCode",lastCode.toString());
        client.resource(INTERNAL_TARGET).path(RestPaths.STATES).path("fridges").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
    }
    
    private boolean handleSensorReplyForFridge(Element elem, long nuidToMatch, Session jmsSession, MessageProducer producer) {
    	long nuid = Long.parseLong(getTxtFor(elem,"value-3") + getTxtFor(elem,"value-2") + getTxtFor(elem,"value-1"),16);
		if (nuid == nuidToMatch) {
			String codeString = getTxtFor(elem,"value0") + getTxtFor(elem,"value1") + getTxtFor(elem,"value2");
			FridgeCode lastCode = FridgeCode.fromCode(Short.parseShort(codeString));
			
			log(LogLevel.FINE,"Code to transmit back from 0x" + Long.toHexString(nuid) + ": " + lastCode);
			short addr = (short)(nuid & 0xFFFF);
			byte[] addrBytesLittleEndian = ByteUtils.getBytes(addr, Endianness.LITTLE_ENDIAN);
			byte[] codeBytesLittleEndian = ByteUtils.getBytes(lastCode.getCode(), Endianness.LITTLE_ENDIAN);
			
			byte[] payload = new byte[5];
			payload[1] = addrBytesLittleEndian[0];
			payload[2] = addrBytesLittleEndian[1];
			payload[3] = codeBytesLittleEndian[0];
			payload[4] = codeBytesLittleEndian[1];
			ModuleCoordinates sourceCoordinates = new ModuleCoordinates(this.id,nuid,addr,(byte)1);
			ModuleCoordinates destinationCoordinates = new ModuleCoordinates((byte)1,0L,(short)0,(byte)1);
			Operation operation = new Operation((byte)0, Domain.EASYHOME.getCode(), EasyHomeContext.ALARM.getCode(), (byte)0, (byte)1, payload);
			AlarmStateRspPacket packet = new AlarmStateRspPacket(sourceCoordinates,destinationCoordinates,operation);
	        ObjectMessage inboundMessage;
			try {
				inboundMessage = jmsSession.createObjectMessage(packet);
				producer.send(inboundMessage); 
			} catch (JMSException e) {
				e.printStackTrace();
			}
        	return true;
		} else
        	return false;
    }
    
    private boolean handleSensorReplyForPIR(Element	elem, long nuidToMatch, Session jmsSession, MessageProducer producer) {
    	long nuid = Long.parseLong(getTxtFor(elem,"value-3") + getTxtFor(elem,"value-2") + getTxtFor(elem,"value-1"),16);
		if (nuid == nuidToMatch) {
			String occupation = getTxtFor(elem,"value0") + getTxtFor(elem,"value1");
			boolean occupied = occupation.equals("5031");
			log(LogLevel.FINE,"Occupation result to transmit back from 0x" + Long.toHexString(nuid) + ": " + occupied);
			
			short addr = (short)(nuid & 0xFFFF);
			byte[] addrBytesLittleEndian = ByteUtils.getBytes(addr, Endianness.LITTLE_ENDIAN);
			
			byte[] payload = new byte[8];
			payload[1] = addrBytesLittleEndian[0];
			payload[2] = addrBytesLittleEndian[1];
			payload[7] = (occupied ? (byte)0x1 : (byte)0x0);
			ModuleCoordinates sourceCoordinates = new ModuleCoordinates(this.id,nuid,addr,(byte)1);
			ModuleCoordinates destinationCoordinates = new ModuleCoordinates((byte)1,0L,(short)0,(byte)1);
			Operation operation = new Operation((byte)0, Domain.HOME_AUTOMATION.getCode(), HomeAutomationContext.OCCUPANCY_SENSING.getCode(), (byte)0, (byte)1, payload);
			OccupancyAttributeRspPacket packet = new OccupancyAttributeRspPacket(sourceCoordinates,destinationCoordinates,operation);
	        ObjectMessage inboundMessage;
			try {
				inboundMessage = jmsSession.createObjectMessage(packet);
				producer.send(inboundMessage); 
			} catch (JMSException e) {
				e.printStackTrace();
			}
        	return true;
		} else
        	return false;
    }
    
    private void addNode(long nuid) {
    	
		MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData = new MultivaluedMapImpl();
        formData.add("gatewayId",Byte.toString(this.id));
        formData.add("nuid",Long.toString(nuid));  
        formData.add("address",Short.toString((short)(nuid & 0xFFFF)));
        formData.add("permanent",Boolean.toString(true));
        ClientResponse response = client.resource(INTERNAL_TARGET).path(RestPaths.NODES).path("insert").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,formData);
        if (response.getClientResponseStatus() != ClientResponse.Status.CREATED)
        	log(LogLevel.DEBUG, "Node insertion response: " + response.getClientResponseStatus());
    }
    
    private String getTxtFor(Element input, String tagName) {
    	
        NodeList tagList = input.getElementsByTagName(tagName);
        Element tagElement = (Element)tagList.item(0);

        NodeList textFNList = tagElement.getChildNodes();
        return ((Node)textFNList.item(0)).getNodeValue().trim();
    }
    
    private String getDescriptorName(Element input) {
    	
        NodeList inputElementList = input.getElementsByTagName("descriptor");
        Element descrElement = (Element)inputElementList.item(0);
        NodeList descriptorElements = descrElement.getElementsByTagName("name");
        Element descrNameElement = (Element)descriptorElements.item(0);
        NodeList textFNList = descrNameElement.getChildNodes();
        return ((Node)textFNList.item(0)).getNodeValue().trim();
    }
    
}