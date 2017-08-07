import java.util.ArrayList;

/**
 * Created by s214079694 on 2017/08/05.
 */
public class Box {
    private ArrayList<Food> items;
    private double size;
    private final double CAPACITY;

    /**
     * Going to be using the bin packaging problem, first fit heuristic
     */
    public Box() {
        this.items = new ArrayList<>();
        this.size = 0;
        this.CAPACITY = 100;
    }

    public boolean add(Food food) {
        if ((size + (food.getSize()) * food.getQuantity()) <= CAPACITY) {
            items.add(food);
            size += (food.getSize() * food.getQuantity());
            return true;
        }
        return false;
    }

    public void remove(Food food) {
        items.remove(food);
        size -= food.getSize();
    }

    public int numberOfFoodItems() {
        return items.size();
    }
}
