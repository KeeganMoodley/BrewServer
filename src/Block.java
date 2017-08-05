import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by s213463695 on 2016/07/02.
 */
public class Block implements Serializable {

    private String name;
    private ArrayList<Row> rows;
    private Integer blockID;
    private String realName;
    private Double leftEntryDistance;
    private Double rightEntryDistance;
    private Double pathLength;
    private Double pathWidth;
    private Double seatWidth;
    private Integer leftEntryRow;
    private Integer rightEntryRow;

    public Block(String name, String realName, ArrayList<Row> rows, Integer blockID, Double leftEntryDistance, Double rightEntryDistance, Double pathLength, Double pathWidth, Double seatWidth, Integer leftEntryRow, Integer rightEntryRow) {
        this.name = name;
        this.rows = rows;
        this.realName = realName;
        this.blockID = blockID;
        this.leftEntryDistance = leftEntryDistance;
        this.rightEntryDistance = rightEntryDistance;
        this.pathLength = pathLength;
        this.pathWidth = pathWidth;
        this.seatWidth = seatWidth;
        this.leftEntryRow = leftEntryRow;
        this.rightEntryRow = rightEntryRow;
    }

    public String getRealName() {
        return realName;
    }

    public Double getSeatWidth() {
        return seatWidth;
    }

    public Double getPathLength() {
        return pathLength;
    }

    public Double getPathWidth() {
        return pathWidth;
    }

    public Double getLeftEntryDistance() {
        return leftEntryDistance;
    }

    public Integer getRightEntryRow() {
        return rightEntryRow;
    }

    public Double getRightEntryDistance() {
        return rightEntryDistance;
    }

    public Integer getLeftEntryRow() {
        return leftEntryRow;
    }

    public Integer getBlockID() {
        return blockID;
    }

    public String getName() {
        return name;
    }

    public ArrayList<Row> getRows() {
        return rows;
    }

    public void addRow(Integer i, Integer seatC) {
        Row newR = new Row(i, seatC);
        rows.add(newR);
    }

    public class Row implements Serializable {
        private Integer number;
        private Integer seatCount;

        public Row(Integer number, Integer seatCount) {
            this.number = number;
            this.seatCount = seatCount;
        }

        public Integer getNumber() {
            return number;
        }

        public void setNumber(Integer number) {
            this.number = number;
        }

        public Integer getSeatCount() {
            return seatCount;
        }

        public void setSeatCount(Integer seatCount) {
            this.seatCount = seatCount;
        }
    }
}