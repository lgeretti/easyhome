package it.uniud.easyhome.rest;

import it.uniud.easyhome.common.LogLevel;
import it.uniud.easyhome.processing.*;
import it.uniud.easyhome.processing.Process;

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.naming.NamingException;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.*;

@Path(RestPaths.PROCESSES)
public class ProcessResource {
    
    @Context
    private UriInfo uriInfo;
	
    private static final List<Process> processes = new ArrayList<Process>();
    
    private static int pidCounter = 0;
    
    @GET
    @Path("size")
    @Produces(MediaType.TEXT_PLAIN)
    public String getNumProcesses() {
    	return String.valueOf(processes.size());
    }
    
    // curl -X POST http://localhost:8080/easyhome/rest/processes -H "Content-Type: application/x-www-form-urlencoded" --data-binary "kind=NODE_ANNCE_REGISTRATION"
    @POST
    public Response postProcess(@FormParam("kind") ProcessKind kind, @FormParam("logLevel") LogLevel logLevel) throws NamingException, JMSException {
        
    	int pid = ++pidCounter;
    	
    	LogLevel levelToUse = (logLevel == null ? LogLevel.INFO : logLevel);
    	
    	Process process = kind.newProcess(pid, uriInfo, levelToUse);
    	
        processes.add(process);
        
        process.start();
        
        return Response.created(uriInfo.getAbsolutePathBuilder()
                .path(String.valueOf(pid))
                .build()).build();
    }
    
    // curl -X POST http://localhost:8080/easyhome/rest/processes/2/logLevel -H "Content-Type: application/x-www-form-urlencoded" --data-binary "logLevel=DEBUG" 
    @POST
    @Path("{pid}/logLevel")
    public Response changeLogLevel(@PathParam("pid") int pid,
    							   @FormParam("logLevel") LogLevel logLevel) {

        for (Process process : processes) {
            if (process.getPid() == pid) {
            	process.setLogLevel(logLevel);
                return Response.ok().build();
            }
        }
        
        return Response.status(Status.NOT_FOUND).build();
    } 
    
    // curl -X POST http://localhost:8080/easyhome/rest/processes/logLevel -H "Content-Type: application/x-www-form-urlencoded" --data-binary "logLevel=DEBUG" 
    @POST
    @Path("logLevel")
    public Response changeLogLevel(@FormParam("logLevel") LogLevel logLevel) {

        for (Process process : processes)
            process.setLogLevel(logLevel);
        
    	return Response.ok().build();
    } 

    @DELETE
    @Path("{pid}")
    public Response deleteProcess(@PathParam("pid") int pid) {
        
        for (int i=0; i<processes.size(); i++) {
            if (processes.get(i).getPid() == pid) {
            	processes.get(i).stop();
                processes.remove(i);
                return Response.ok().build();
            }
        }
        
        return Response.status(Status.NOT_FOUND).build();
    }
    
    @DELETE
    public Response clearAll() {
        
    	for (Process pr : processes) {
    		pr.stop();
    	}
    	processes.clear();
        
        return Response.ok().build();
    }
    
}
