package sample;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by s214079694 on 2017/08/05.
 */
public class Box implements Serializable {
    private ArrayList<Food> items;
    private double size;
    private final double CAPACITY;
    private static final long serialVersionUID = 9140667418784579444L;
    private boolean full = false;

    /**
     * Going to be using the bin packaging problem, first fit heuristic
     */
    public Box() {
        this.items = new ArrayList<>();
        this.size = 0;
        this.CAPACITY = 300000; //Volume = 100×50×30 = 150000 centimeters^3
    }

    public boolean add(Food food) {
        if ((size + food.getSize()) <= CAPACITY) {
            items.add(food);
            size += food.getSize();
            return true;
        }
        full = true;
        return false;
    }

    public boolean isFull() {
        return full;
    }

    public void remove(Food food) {
        items.remove(food);
        size -= food.getSize();
    }

    public int numberOfFoodItems() {
        return items.size();
    }

    public ArrayList<Food> getItems() {
        return items;
    }
}
