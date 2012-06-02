package it.uniud.easyhome.jsf;

public class Car {

    private String model;
    
    private int year;
    
    private String manufacturer;
    
    private String color;
    
    public Car(String model, int year, String manufacturer,
            String color) {
        this.model = model;
        this.year = year;
        this.manufacturer = manufacturer;
        this.color = color;
    }

    public String getModel() {
        return this.model;
    }
    
    public int getYear() {
        return this.year;
    }
    
    public String getManufacturer() {
        return this.manufacturer;
    }
    
    public String getColor() {
        return this.color;
    }
    
}
