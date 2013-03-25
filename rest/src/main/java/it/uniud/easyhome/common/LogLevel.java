package it.uniud.easyhome.common;

public enum LogLevel {

	NONE(0),
	INFO(1),
	FINE(2),
	ULTRAFINE(3),
	DEBUG(4);
	
	private int level;
	
	private LogLevel(int level) {
		this.level = level;
	}
	
	public boolean acceptsLogOf(LogLevel logLevel) {
		if (logLevel.level == 0)
			return false;
		return (logLevel.level <= this.level);
	}
}
