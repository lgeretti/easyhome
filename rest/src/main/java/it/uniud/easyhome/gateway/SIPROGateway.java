package it.uniud.easyhome.gateway;

import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.exceptions.IncompletePacketException;
import it.uniud.easyhome.packets.natives.LampStateSetPacket;
import it.uniud.easyhome.packets.natives.NativePacket;

import java.io.*;

import javax.jms.MessageProducer;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.codehaus.jettison.json.JSONException;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class SIPROGateway extends Gateway {
	
	private static final String TARGET = "http://localhost:5000/";
	
	private static Client client = Client.create();
			
    public SIPROGateway(byte id, int port, LogLevel logLevel) {
    	super(id,ProtocolType.NATIVE,port,logLevel);
    	MAX_CONNECTIONS = 32;
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
    final protected void write(NativePacket pkt, OutputStream os, MessageProducer producer) throws IOException {
    	
		if (LampStateSetPacket.validates(pkt)) {
			
			LampStateSetPacket statePacket = (LampStateSetPacket) pkt;
			
			String paramsString = statePacket.getIdentifier()+";changeColor"+statePacket.getSeparatedParameters();
			
			log(LogLevel.DEBUG,"params="+paramsString);
			
	        MultivaluedMap<String,String> queryParams = new MultivaluedMapImpl();
	        queryParams.add("method","setValueParam");
	        queryParams.add("params",paramsString);
			
			ClientResponse response = client.resource(TARGET).queryParams(queryParams).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
	
			
		}
    }
  
}