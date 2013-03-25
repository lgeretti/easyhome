package it.uniud.easyhome.processing;

import it.uniud.easyhome.common.LogLevel;

import javax.jms.JMSException;
import javax.naming.NamingException;
import javax.ws.rs.core.UriInfo;

public enum ProcessKind {
	
	// Adds new nodes as soon as they announce themselves
	NODE_ANNCE_REGISTRATION { 
		@Override
		public Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException { 
			return new NodeAnnceRegistrationProcess(pid,uriInfo,this,logLevel);
		}
	}, 
	// Requests the node descriptor as soon as a node is added
	NODE_DESCR_REQUEST { 
		@Override
		public Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException {
			return new NodeDescrRequestProcess(pid,uriInfo,this,logLevel);
		}
	}, 
	// Updates the node as soon as a descriptor is received
	NODE_DESCR_REGISTRATION { 
		@Override
		public Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException {
			return new NodeDescrRegistrationProcess(pid,uriInfo,this,logLevel);
		}
	},
	NODE_NEIGH_REQUEST {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException {
			return new NodeNeighRequestProcess(pid,uriInfo,this,logLevel);
		}
	},
	NODE_NEIGH_REGISTRATION {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException {
			return new NodeNeighRegistrationProcess(pid,uriInfo,this,logLevel);
		}
	},
	ACTIVE_ENDPOINTS_REQUEST {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException {
			return new ActiveEndpointsRequestProcess(pid,uriInfo,this,logLevel);
		}		
	},
	ACTIVE_ENDPOINTS_REGISTRATION {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException {
			return new ActiveEndpointsRegistrationProcess(pid,uriInfo,this,logLevel);
		}		
	},	
	SIMPLE_DESCR_REQUEST {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException {
			return new SimpleDescrRequestProcess(pid,uriInfo,this,logLevel);
		}
	},
	SIMPLE_DESCR_REGISTRATION {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException {
			return new SimpleDescrRegistrationProcess(pid,uriInfo,this,logLevel);
		}
	},
	NODE_DISCOVERY_REQUEST {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException {
			return new NodeDiscoveryRequestProcess(pid,uriInfo,this,logLevel);
		}
	},
	NODE_DISCOVERY_REGISTRATION {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException {
			return new NodeDiscoveryRegistrationProcess(pid,uriInfo,this,logLevel);
		}
	},
	NODE_POWER_LEVEL_REQUEST {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException {
			return new NodePowerLevelRequestProcess(pid,uriInfo,this,logLevel);
		}
	},
	NODE_POWER_LEVEL_REGISTRATION {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException {
			return new NodePowerLevelRegistrationProcess(pid,uriInfo,this,logLevel);
		}
	},	
	NODE_POWER_LEVEL_SET_ISSUE {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException {
			return new NodePowerLevelSetIssueProcess(pid,uriInfo,this,logLevel);
		}
	},
	NODE_POWER_LEVEL_SET_ACKNOWLEDGMENT {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException {
			return new NodePowerLevelSetAcknowledgmentProcess(pid,uriInfo,this,logLevel);
		}
	},	
	OCCUPANCY_REQUEST {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException {
			return new OccupancySensingRequestProcess(pid,uriInfo,this,logLevel);
		}
	},
	OCCUPANCY_REGISTRATION {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException {
			return new OccupancySensingRegistrationProcess(pid,uriInfo,this,logLevel);
		}
	},	
	NETWORK_UPDATE {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException {
			return new NetworkUpdateProcess(pid,uriInfo,this,logLevel);
		}
	}, 
	NETWORK_GRAPH_MINIMIZATION {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException {
			return new NetworkGraphMinimizationProcess(pid,uriInfo,this,logLevel);
		}
	},
	LIGHT_LEVEL_CONTROL {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException {
			return new LightLevelControlProcess(pid,uriInfo,this,logLevel);
		}
	},
	ALARM_STATE_REQUEST {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException {
			return new AlarmStateRequestProcess(pid,uriInfo,this,logLevel);
		}
	},
	ALARM_STATE_ACKNOWLEDGMENT {
		@Override
		public Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException {
			return new AlarmStateAcknowledgmentProcess(pid,uriInfo,this,logLevel);
		}
	};
	
	public abstract Process newProcess(int pid, UriInfo uriInfo, LogLevel logLevel) throws NamingException, JMSException;
}
