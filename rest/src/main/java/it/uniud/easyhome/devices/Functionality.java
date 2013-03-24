package it.uniud.easyhome.devices;

import javax.persistence.*;
import javax.xml.bind.annotation.*;

@Entity
@Table(name = "Functionality")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Functionality {
	
	@Id
	private long id;
    @Column
    private String name;
    @Column
    private FunctionalityType type;
    @OneToOne
    private PersistentInfo info;
    @Column
    private String imgPath;
    @Column
    private String help;

    @SuppressWarnings("unused")
	private Functionality() {}
    
    public Functionality(long id,  String name, FunctionalityType type, PersistentInfo info, String imgPath, String help) {
    	this.id = id;
    	this.name = name;
    	this.type = type;
    	this.info = info;
    	this.imgPath = imgPath;
    	this.help = help;
    }

	public void setName(String name) {
		this.name = name;
	}
	
	public long getId() {
		return this.id;
	}
	
    public String getName() {
        return this.name;
    }
    
    public FunctionalityType getType() {
    	return this.type;
    }
    
	public PersistentInfo getInfo() {
		return this.info;
	}
	
	public String getImgPath() {
		return this.imgPath;
	}
	
	public String getHelp() {
		return this.help;
	}

    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof Functionality))
            throw new IllegalArgumentException();
        Functionality otherFunctionality = (Functionality) other;
        
        if (!this.name.equals(otherFunctionality.name)) return false;
        if (!this.info.equals(otherFunctionality.info)) return false;
        
        return true;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;

        long result = 1;
        result = prime * result + name.hashCode();
        result = prime * result + info.hashCode();
        return (int)result;
    }
}
