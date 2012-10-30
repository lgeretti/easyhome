package it.uniud.easyhome.rest;

import it.uniud.easyhome.network.Node;

import java.util.List;

import javax.ejb.Remote;

@Remote
public interface NetworkResourceEJBRemote {

	public List<Node> getNodes();
	public Node findNodeById(long nodeId);
	public boolean insertOrUpdateNode(Node node);
	public boolean removeNodeById(long nodeId);
	public void removeAllNodes();
}
