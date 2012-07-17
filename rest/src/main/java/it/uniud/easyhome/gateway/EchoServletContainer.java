package it.uniud.easyhome.gateway;

import java.io.IOException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class EchoServletContainer extends GenericServlet {
    
    private EchoServer server;
    
    private static final long serialVersionUID = -8819699947334069861L;

    @Override
    public void init() {
        
        this.server = new EchoServer(super.getServletConfig());
        
        try {
            
          Thread serviceThread = new Thread(server);  
          serviceThread.start();
        }
        catch (Exception ex) {
          ex.printStackTrace();
        }
    }
    
    @Override
    public void service(ServletRequest req, ServletResponse res)
            throws ServletException, IOException {
    }

}
