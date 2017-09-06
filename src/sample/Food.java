package sample;

import java.io.Serializable;


/**
 * Created by s214079694 on 2017/07/01.
 */
public class Food implements Serializable {
    private int quantity, quantityAvailable, id, type, prepTime;
    private double price, length, width, height, volume = 0;
    private String title, nutrition, dietary;
    private boolean halaal;
    private byte[] image;
    private static final long serialVersionUID = 9140667418784579444L;

    public Food(int id, int type, byte[] image, double price, String title, String nutrition, String dietary, boolean halaal, int quantityAvailable, double length, double width, double height, double volume, int prepTime) {
        this.image = image;
        this.price = price;
        this.title = title;
        this.halaal = halaal;
        quantity = 0;
        this.quantityAvailable = quantityAvailable;
        this.nutrition = nutrition;
        this.dietary = dietary;
        this.id = id;
        this.type = type;
        this.length = length;
        this.width = width;
        this.height = height;
        this.volume = volume;
        this.prepTime = prepTime;
    }

    public byte[] getImage() {
        return image;
    }

    public int getPrepTime() {
        return prepTime;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }

    public void setQuantityAvailable(int quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNutrition() {
        return nutrition;
    }

    public void setNutrition(String nutrition) {
        this.nutrition = nutrition;
    }

    public String getDietary() {
        return dietary;
    }

    public void setDietary(String dietary) {
        this.dietary = dietary;
    }

    public boolean isHalaal() {
        return halaal;
    }

    public void setHalaal(boolean halaal) {
        this.halaal = halaal;
    }

    public double getSize() {
        return volume == 0 ? length * width * height : volume;
    }
}

