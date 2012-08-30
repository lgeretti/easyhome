package it.uniud.easyhome.rest;

import it.uniud.easyhome.processing.NodeRegistrationProcess;
import it.uniud.easyhome.processing.Process;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.*;
import javax.ws.rs.*;

@Path("/processes")
public class ProcessResource {
    
    private static final List<Process> processes = new ArrayList<Process>();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Process> getProcesses() {

        return processes;
    }           
    
    @GET
    @Path("/howmany")
    @Produces(MediaType.TEXT_PLAIN)
    public String getNum() {
        
        StringBuilder strb = new StringBuilder();
        strb.append("There are ").append(processes.size()).append(" processes around, with ids");
        for (Process p : processes)
            strb.append(p.getPid()).append(",");
        strb.deleteCharAt(strb.length()-1);
        
        return strb.toString();
    }
    
    @POST
    @Path("{pid}")
    public Response postProcess(@PathParam("pid") int pid) {
        
        for (Process p : processes) {
            if (p.getPid() == pid)
                return Response.notModified().build();
        }
        
        processes.add(new NodeRegistrationProcess(pid,Process.Session.STATELESS,Process.Interaction.SYNC));
        
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
        
        return Response.status(404).build();
    }
    
    @DELETE
    public Response clearAll() {
        
        processes.clear();
        
        return Response.ok().build();
    }
    
}
