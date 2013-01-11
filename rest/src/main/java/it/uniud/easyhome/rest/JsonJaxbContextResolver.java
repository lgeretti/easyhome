package it.uniud.easyhome.rest;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.uniud.easyhome.devices.DeviceIdentifier;
import it.uniud.easyhome.devices.DevicesWrapper;
import it.uniud.easyhome.gateway.Gateway;
import it.uniud.easyhome.network.NetworkJob;
import it.uniud.easyhome.network.Node;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;

@Provider
public final class JsonJaxbContextResolver implements ContextResolver<JAXBContext> {

    private JAXBContext context;
    private Class<?>[] types;

    public JsonJaxbContextResolver() throws JAXBException {
        types = new Class[] {
            Node.class, 
            Gateway.class,
            NetworkJob.class
        };
        context = new JSONJAXBContext(JSONConfiguration.natural().build(), types);
    }

    @Override
    public JAXBContext getContext(Class<?> objectType) {
        for (Class<?> type : types) {
            if (type==objectType)
                return context;
        }
        return null;
    }
}