package it.uniud.easyhome.jsf.carousel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;


@ManagedBean(eager= true)
@ApplicationScoped
public class CarList {

    private List<Car> content;
    
    private static String[] colors, manufacturers;
    
    static {
        colors = new String[10];
        colors[0] = "Black";
        colors[1] = "White";
        colors[2] = "Green";
        colors[3] = "Red";
        colors[4] = "Blue";
        colors[5] = "Orange";
        colors[6] = "Silver";
        colors[7] = "Yellow";
        colors[8] = "Brown";
        colors[9] = "Maroon";
        
        manufacturers = new String[10];
        manufacturers[0] = "Mercedes";
        manufacturers[1] = "BMW";
        manufacturers[2] = "Volvo";
        manufacturers[3] = "Audi";
        manufacturers[4] = "Renault";
        manufacturers[5] = "Opel";
        manufacturers[6] = "Volkswagen";
        manufacturers[7] = "Chrysler";
        manufacturers[8] = "Ferrari";
        manufacturers[9] = "Ford";
    }
    
    public CarList() {
        content = new ArrayList<Car>();
        
        populateRandomCars(content, 9);        
    }
    
    public List<Car> getContent() {
        return content;
    }
    
    private void populateRandomCars(List<Car> list, int size) {
        for(int i = 0 ; i < size ; i++)
            list.add(new Car(getRandomModel(), getRandomYear(), getRandomManufacturer(), getRandomColor()));
    }
    
    public void doIncrease() {
        content.add(new Car(getRandomModel(), getRandomYear(), getRandomManufacturer(), getRandomColor()));
    }

    public void doDecrease() {
        if (!content.isEmpty())
            content.remove(content.size()-1);
    }

    private int getRandomYear() {
        return (int) (Math.random() * 50 + 1960);
    }
    
    private String getRandomColor() {
        return colors[(int) (Math.random() * 10)];
    }
    
    private String getRandomManufacturer() {
        return manufacturers[(int) (Math.random() * 10)];
    }
    
    private String getRandomModel() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
    
}
