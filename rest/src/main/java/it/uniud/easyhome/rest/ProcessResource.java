package it.uniud.easyhome.rest;

import it.uniud.easyhome.processing.NodeRegistrationProcess;
import it.uniud.easyhome.processing.Process;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.*;

@Path("/processes")
public class ProcessResource {
    
    private static final List<Process> processes = new ArrayList<Process>();
    
    private int pidCounter = 0;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Process> getProcesses() {

        return processes;
    }           
    
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getNum() {
        
        StringBuilder strb = new StringBuilder();
        strb.append("There are ").append(processes.size()).append(" processes around, with ids");
        for (Process p : processes)
            strb.append(p.getPid()).append(",");
        strb.deleteCharAt(strb.length()-1);
        
        return strb.toString();
    }
    
 // curl -X POST http://localhost:8080/easyhome/rest/processes -H "Content-Type: application/x-www-form-urlencoded" --data-binary "kind=nodeRegistration"
    @POST
    public Response postProcess(@FormParam("kind") String kind) {
        
    	int pid = ++pidCounter;
    	
    	Process process = null;
    	
    	switch (kind) {
	    	case "nodeRegistration":
	    		process = new NodeRegistrationProcess(pid);
	    		break;
	    	default:
    	}
    	
        processes.add(process);
        
        process.start();
        
        return Response.ok().build();
    }

    @DELETE
    @Path("{pid}")
    public Response deleteProcess(@PathParam("pid") int pid) {
        
        for (int i=0; i<processes.size(); i++) {
            if (processes.get(i).getPid() == pid) {
                processes.remove(i);
                return Response.ok().build();
            }
        }
        
        return Response.status(Status.NOT_FOUND).build();
    }
    
    @DELETE
    public Response clearAll() {
        
        processes.clear();
        
        return Response.ok().build();
    }
    
}
