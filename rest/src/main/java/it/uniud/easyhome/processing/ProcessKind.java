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
	// Requests the node descriptor as soon as a node is added
	NODE_DESCR_REQUEST { 
		@Override
		public Process newProcess(int pid, UriInfo uriInfo) throws NamingException, JMSException {
			return new NodeDescrRequestProcess(pid,uriInfo,this);
		}
	}, 
	// Updates the node as soon as a descriptor is received
	NODE_DESCR_REGISTRATION { 
		@Override
		public Process newProcess(int pid, UriInfo uriInfo) throws NamingException, JMSException {
			return new NodeDescrRegistrationProcess(pid,uriInfo,this);
		}
	},
	NODE_NEIGH_REQUEST {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo) throws NamingException, JMSException {
			return new NodeNeighRequestProcess(pid,uriInfo,this);
		}
	},
	NODE_NEIGH_REGISTRATION {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo) throws NamingException, JMSException {
			return new NodeNeighRegistrationProcess(pid,uriInfo,this);
		}
	},
	ACTIVE_ENDPOINTS_REQUEST {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo) throws NamingException, JMSException {
			return new ActiveEndpointsRequestProcess(pid,uriInfo,this);
		}		
	},
	ACTIVE_ENDPOINTS_REGISTRATION {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo) throws NamingException, JMSException {
			return new ActiveEndpointsRegistrationProcess(pid,uriInfo,this);
		}		
	},	
	SIMPLE_DESCR_REQUEST {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo) throws NamingException, JMSException {
			return new SimpleDescrRequestProcess(pid,uriInfo,this);
		}
	},
	SIMPLE_DESCR_REGISTRATION {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo) throws NamingException, JMSException {
			return new SimpleDescrRegistrationProcess(pid,uriInfo,this);
		}
	},
	NODE_DISCOVERY_REQUEST {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo) throws NamingException, JMSException {
			return new NodeDiscoveryRequestProcess(pid,uriInfo,this);
		}
	},
	NODE_DISCOVERY_REGISTRATION {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo) throws NamingException, JMSException {
			return new NodeDiscoveryRegistrationProcess(pid,uriInfo,this);
		}
	},
	NODE_POWER_LEVEL_REQUEST {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo) throws NamingException, JMSException {
			return new NodePowerLevelRequestProcess(pid,uriInfo,this);
		}
	},
	NODE_POWER_LEVEL_ACKNOWLEDGMENT {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo) throws NamingException, JMSException {
			return new NodePowerLevelAcknowledgementProcess(pid,uriInfo,this);
		}
	},	
	NODE_POWER_LEVEL_SET {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo) throws NamingException, JMSException {
			return new NodePowerLevelSetProcess(pid,uriInfo,this);
		}
	},		
	NETWORK_UPDATE {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo) throws NamingException, JMSException {
			return new NetworkUpdateProcess(pid,uriInfo,this);
		}
	};
	
	public abstract Process newProcess(int pid, UriInfo uriInfo) throws NamingException, JMSException;
}
