import java.util.ArrayList;
import java.util.Date;

/**
 * Created by s213463695 on 2016/07/23.
 */
public class Order {

    private Integer orderID;
    private Integer quantity;
    private Double total;
    private String user;
    private String androidIndex;
    private Block block=null;
    private Block realBlock;
    private Integer rowNumber;
    private Integer seat=null;
    private Integer realSeat;
    private Date date;
    private ArrayList<Order> links=null;

    public Order(Integer orderID, Block realBlock, Integer rowNumber, Integer quantity, Double total, String user, Integer realSeat, String androidIndex, Date time) {
        this.orderID = orderID;
        this.realBlock = realBlock;
        this.rowNumber = rowNumber;
        this.date =time;
        this.quantity = quantity;
        this.total = total;
        this.user = user;
        this.androidIndex=androidIndex;
        this.realSeat = realSeat;
        this.links=new ArrayList<>();
    }

    public String getAndroidIndex() {
        return androidIndex;
    }

    public Block getRealBlock() {
        return realBlock;
    }

    public Integer getRealSeat() {
        return realSeat;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public void setSeat(Integer seat) {
        this.seat = seat;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Double getTotal() {
        return total;
    }

    public Integer getOrderID() {
        return orderID;
    }

    public String getUser() {
        return user;
    }

    public ArrayList<Order> getLinks() {
        return links;
    }

    public void setLinks(ArrayList<Order> links) {
        this.links = links;
    }

    public void addLink(Order link){
        links.add(link);
    }

    public Date getDate() {
        return date;
    }

    public Block getBlock() {
        return block;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public Integer getSeat() {
        return seat;
    }

    public void setRealSeat(int realSeat) {
        this.realSeat = realSeat;
    }
}
