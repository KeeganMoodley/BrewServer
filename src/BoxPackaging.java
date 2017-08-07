import java.util.ArrayList;

/**
 * Created by s214079694 on 2017/08/06.
 */
public class BoxPackaging {
    ArrayList<Food> foods;
    int boxSize;
    ArrayList<Box> boxes = new ArrayList<>();

    public BoxPackaging(ArrayList<Food> foods) {
        this.foods = foods;
    }

    public void firstFitAlgorithm() {
        boxes.add(new Box());
        for (Food food : foods) {
            boolean foodInserted = false;
            int currentBox = 0;
            while (!foodInserted) {
                if (currentBox == boxes.size()) {
                    Box box = new Box();
                    box.add(food);
                    boxes.add(box);
                    foodInserted = true;
                } else if (boxes.get(currentBox).add(food)) {
                    foodInserted = true;
                } else {
                    currentBox++;
                }
            }
        }
    }
}
