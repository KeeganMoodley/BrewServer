/**
 * Created by s214079694 on 2017/07/01.
 */

public class Liquid extends Food {
    double volume;

    public Liquid(int image, double price, String title, String nutrition, String dietary, boolean halaal, int quantityAvailable, double volume) {
        super(image, price, title, nutrition, dietary, halaal, quantityAvailable);
        this.volume = volume;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }
}
