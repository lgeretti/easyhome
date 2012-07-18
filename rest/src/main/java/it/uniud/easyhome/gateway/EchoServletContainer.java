package it.uniud.easyhome.gateway;

import java.io.IOException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class EchoServletContainer extends GenericServlet {
    
    private EchoServer server = null;
    
    private static final long serialVersionUID = -8819699947334069861L;

    @Override
    public void init() {
        
        int port;
        
        try {
            port = Integer.parseInt(super.getServletConfig().getInitParameter("it.uniud.easyhome.gateway.property.port"));
            
            this.server = new EchoServer(port);
            
            try {
                
              Thread serviceThread = new Thread(server);  
              serviceThread.start();
            }
            catch (Exception ex) {
              ex.printStackTrace();
            }
            
        } catch (NumberFormatException ex) {
            System.out.println("Error: provide an it.uniud.easyhome.gateway.property.port value in the servlet configuration.");
        }
    }
    
    @Override
    public void service(ServletRequest req, ServletResponse res)
            throws ServletException, IOException {
        // Nothing to do, no actual protocol handler exists
    }

    @Override
    public void destroy() {
        if (server != null)
            server.close();
    }
    
}
