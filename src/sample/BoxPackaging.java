package sample;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by s214079694 on 2017/08/06.
 */
public class BoxPackaging implements Serializable {
    private ArrayList<Food> foods;
    int boxSize;
    private ArrayList<Box> boxes = new ArrayList<>();
    private boolean full = false;

    public BoxPackaging(ArrayList<Food> foods) {
        this.foods = foods;
    }

    public void setFoods(ArrayList<Food> foods) {
        this.foods = foods;
    }

    public void firstFitAlgorithm() {
        if (boxes.size() == 0)
            boxes.add(new Box());
        for (Food food : foods) {
            int currentBox = 0;
            for (int i = 0; i < food.getQuantity(); i++) {
                boolean foodInserted = false;
                while (!foodInserted) {
                    if (boxes.get(currentBox).add(food)) {
                        foodInserted = true;
                    } else if (boxes.size() - 1 == currentBox) {
                        full = true;
                        Box box = new Box();
                        box.add(food);
                        boxes.add(box);
                        foodInserted = true;
                        currentBox++;
                    } else {
                        currentBox++;
                    }
                }
            }
        }
    }

    public boolean isFull() {
        if (full)
            System.out.println("The first box is full");
        return full;
    }

    public ArrayList<Box> getBoxes() {
        return boxes;
    }

    public void setBoxes(ArrayList<Box> boxes) {
        this.boxes = boxes;
    }
}
