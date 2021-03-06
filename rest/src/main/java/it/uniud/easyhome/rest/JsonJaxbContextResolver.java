package it.uniud.easyhome.rest;

import it.uniud.easyhome.gateway.Gateway;
import it.uniud.easyhome.devices.Functionality;
import it.uniud.easyhome.devices.Location;
import it.uniud.easyhome.devices.Pairing;
import it.uniud.easyhome.devices.PersistentInfo;
import it.uniud.easyhome.devices.states.*;
import it.uniud.easyhome.network.*;

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

    // Classes not put into this list will return arrays wrapped by an additional JavaScript object named after the class name,
    // instead of returning the naked array
    public JsonJaxbContextResolver() throws JAXBException {
        types = new Class[] {
            Node.class, 
            Gateway.class,
            NetworkJob.class,
            GlobalCoordinates.class,
            Link.class,
            Location.class,
            PersistentInfo.class,
            Functionality.class,
            Pairing.class,
            LampState.class,
            FridgeState.class
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