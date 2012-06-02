package it.uniud.easyhome.jsf.carousel;

import it.uniud.easyhome.jsf.common.CarList;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.icefaces.application.PushRenderer;

@ManagedBean
@ViewScoped
public class CarouselController {
    
    private static final String PUSH_GROUP = "carousel";
    
    @ManagedProperty(value="#{carList}")
    private CarList cars;
    
    @PostConstruct
    public void init() {
        PushRenderer.addCurrentView(PUSH_GROUP);
    }
    
    public CarList getCars() {
        return cars;
    }
    
    public void setCars(CarList cars) {
        this.cars = cars;
    }
    
    public void doIncreaseCars() {
        this.cars.doIncrease();
        PushRenderer.render(PUSH_GROUP);
    }
    
    public void doDecreaseCars() {
        this.cars.doDecrease();
        PushRenderer.render(PUSH_GROUP);
    }
}
                