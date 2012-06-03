package it.uniud.easyhome.push;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class JettyPrimePush {

    public static void main(String[] args) throws Exception {
        Server server = new Server(Integer.parseInt(args[0]));

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        context.addServlet(new ServletHolder(new PushServlet()), "/*");

        server.start();
        server.join();
    }
}