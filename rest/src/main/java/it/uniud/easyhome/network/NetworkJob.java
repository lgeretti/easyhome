package it.uniud.easyhome.network;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "NetworkJob")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class NetworkJob implements Serializable {

	private static final long serialVersionUID = -5044126796040723474L;

	@Id
	private int id;
	
	@Enumerated
	@Column(nullable = false)
	private NetworkJobType type;
	
	@Column(nullable = false)
	private byte gatewayId;
	
	@Column(nullable = false)
	private long nuid;
	
	@Column(nullable = false)
	private short address;
	
	@Column(nullable = false)
	private byte endpoint;
	
	@Temporal(TemporalType.TIME)
	@Column(nullable = false)
	private Date timestamp;
	
	@SuppressWarnings("unused")
	private NetworkJob() { }
	
	public NetworkJob(int id, NetworkJobType type, byte gatewayId, long nuid, short address, byte endpoint) {
		
		this.id = id;
		this.type = type;
		this.gatewayId = gatewayId;
		this.nuid = nuid;
		this.address = address;
		this.endpoint = endpoint;
		this.timestamp = new Date(System.currentTimeMillis());
	}
	
	public int getId() {
		return id;
	}
	
	public NetworkJobType getType() {
		return type;
	}
	
	public byte getGatewayId() {
		return gatewayId;
	}
	
	public long getNuid() {
		return nuid;
	}
	
	public short getAddress() {
		return address;
	}
	
	public byte getEndpoint() {
		return endpoint;
	}
	
	public Date getDate() {
		return timestamp;
	}
	
	public void resetDate() {
		timestamp = new Date(System.currentTimeMillis());
	}
}
