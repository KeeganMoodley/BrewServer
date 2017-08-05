import java.util.ArrayList;

/**
 * Created by s213463695 on 2016/08/04.
 */
public class Cluster {

    private ArrayList<Order> orders;
    private Integer quantity;
    private Double income;

    public Cluster(ArrayList<Order> orders, Integer quantity, Double income) {
        this.orders = orders;
        this.quantity = quantity;
        this.income = income;
    }

    public void addAmount(Double amount) {
        this.income += amount;
    }

    public void addQuantity(Integer i) {
        this.quantity += i;
    }

    public void add(Order order) {
        orders.add(order);
    }

    public ArrayList<Order> getOrders() {
        return orders;
    }

    public Order get(int i) {
        return orders.get(i);
    }

    public void remove(Order i) {
        orders.remove(i);
        income -= i.getTotal();
        quantity -= i.getQuantity();
    }

    public int size() {
        return orders.size();
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Double getIncome() {
        return income;
    }

    public void update(Order link) {
        addAmount(link.getTotal());
        addQuantity(link.getQuantity());
        add(link);
    }
}
