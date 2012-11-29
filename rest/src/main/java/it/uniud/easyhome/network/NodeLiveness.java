package it.uniud.easyhome.network;

public enum NodeLiveness {

	OK, // After the liveness has been checked
	CHECKING, // After issuing a check
	UNRESPONSIVE // If one check has failed
}
