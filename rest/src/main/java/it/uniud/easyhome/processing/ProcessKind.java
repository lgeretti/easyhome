package it.uniud.easyhome.processing;

import javax.jms.JMSException;
import javax.naming.NamingException;
import javax.ws.rs.core.UriInfo;

public enum ProcessKind {
	
	// Adds new nodes as soon as they announce themselves
	NODE_ANNCE_REGISTRATION { 
		@Override
		public Process newProcess(int pid, UriInfo uriInfo) throws NamingException, JMSException { 
			return new NodeAnnceRegistrationProcess(pid,uriInfo,this);
		}
	}, 
	// Requests the node descriptor as soon as a node is added, then updates the node information
	NODE_DESCR_ACQUIREMENT { 
		@Override
		public Process newProcess(int pid, UriInfo uriInfo) throws NamingException, JMSException {
			return new NodeDescrAcquirementProcess(pid,uriInfo,this);
		}
	};
	
	public abstract Process newProcess(int pid, UriInfo uriInfo) throws NamingException, JMSException;
}
