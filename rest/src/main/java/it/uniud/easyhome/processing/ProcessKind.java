package it.uniud.easyhome.processing;

import javax.ws.rs.core.UriInfo;

public enum ProcessKind {
	
	// Adds new nodes as soon as they announce themselves
	NODE_ANNCE_REGISTRATION { 
		@Override
		public Process newProcess(int pid, UriInfo uriInfo) { 
			return new NodeAnnceRegistrationProcess(pid,uriInfo);
		}
	}, 
	// Requests the node descriptor as soon as a node is added, then updates the node information
	NODE_DESCR_ACQUIREMENT { 
		@Override
		public Process newProcess(int pid, UriInfo uriInfo) { 
			return new NodeDescrAcquirementProcess(pid,uriInfo);
		}
	};
	
	public abstract Process newProcess(int pid, UriInfo uriInfo);
}
