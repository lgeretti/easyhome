package it.uniud.easyhome.rest;

import it.uniud.easyhome.processing.*;
import it.uniud.easyhome.processing.Process;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.*;

@Path("/processes")
public class ProcessResource {
    
    @Context
    private UriInfo uriInfo;
	
    private static final List<Process> processes = new ArrayList<Process>();
    
    private static int pidCounter = 0;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Process> getProcesses() {
        return processes;
    }
    
    @GET
    @Path("size")
    @Produces(MediaType.TEXT_PLAIN)
    public String getNumProcesses() {
    	return String.valueOf(processes.size());
    }
    
    // curl -X POST http://localhost:8080/easyhome/rest/processes -H "Content-Type: application/x-www-form-urlencoded" --data-binary "kind=NODE_ANNCE_REGISTRATION"
    @POST
    public Response postProcess(@FormParam("kind") ProcessKind kind) {
        
    	int pid = ++pidCounter;
    	
    	Process process = kind.newProcess(pid, uriInfo);
    	
        processes.add(process);
        
        process.start();
        
        return Response.created(uriInfo.getAbsolutePathBuilder()
                .path(String.valueOf(pid))
                .build()).build();
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
