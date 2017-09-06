package sample;

/**
 * Created by s214079694 on 2017/07/29.
 */
public class FoodUpdate {
    private int id, quantity;

    public FoodUpdate(int id, int quantity) {
        this.id = id;
        this.quantity = quantity;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
