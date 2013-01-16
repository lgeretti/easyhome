package it.uniud.easyhome.network;

import java.io.Serializable;
import java.sql.Time;
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
	
	@Column(nullable = false)
	private NetworkJobType type;
	
	@Column(nullable = false)
	private byte gatewayId;
	
	@Column(nullable = false)
	private short address;
	
	@Column(nullable = false)
	private byte endpoint;
	
	@Column(nullable = false)
	private long timestamp;
	
	@Column(nullable = false)
	private byte tsn;
	
	@SuppressWarnings("unused")
	private NetworkJob() { }
	
	public NetworkJob(int id, NetworkJobType type, byte gatewayId, short address, byte endpoint, byte tsn) {
		
		this.id = id;
		this.type = type;
		this.gatewayId = gatewayId;
		this.address = address;
		this.endpoint = endpoint;
		this.timestamp = System.currentTimeMillis();
		this.tsn = tsn;
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
	
	public short getAddress() {
		return address;
	}
	
	public byte getEndpoint() {
		return endpoint;
	}
	
	public Date getDate() {
		return new Date(timestamp);
	}
	
	public byte getTsn() {
		return tsn;
	}
	
	public boolean isFirst() {
		return (tsn == 0);
	}
}
