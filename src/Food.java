/**
 * Created by s214079694 on 2017/07/01.
 */
public abstract class Food {
    int image, quantity, quantityAvailable;
    double price;
    String title, nutrition, dietary;
    boolean halaal;

    public Food(int image, double price, String title, String nutrition, String dietary, boolean halaal, int quantityAvailable) {
        this.image = image;
        this.price = price;
        this.title = title;
        this.halaal = halaal;
        quantity = 0;
        this.quantityAvailable = quantityAvailable;
        this.nutrition = nutrition;
        this.dietary = dietary;
    }

    public int getImage() {
        return image;
    }

    public void setImage(int image) {
        this.image = image;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isHalaal() {
        return halaal;
    }

    public void setHalaal(boolean halaal) {
        this.halaal = halaal;
    }

    public double getTotal() {
        return quantity * price;
    }
}

