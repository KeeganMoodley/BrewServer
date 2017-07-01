import javafx.util.Pair;

import java.sql.Connection;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * Created by s213463695 on 2016/06/20.
 */
public class DB_Controller {
    private static ArrayList<User> users = new ArrayList<>();
    private static Connection connect = null;
    private static Statement stat = null;
    private static Statement innerStat = null;
    private static String dateString;
    private static String deliveryString;
    private static String timeString;
    private static ArrayList<Block> blocks;

    /*
    The below method opens the connection to the database and creates a statement
    which enables the other methods to communicate with the database.
     */
    public static void OpenConnection() throws Exception {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        String url = "jdbc:sqlserver://postgrad.nmmu.ac.za;databaseName=Brew";   //database specific url.
        String user = "brewuser";
        String password = "Fdde9345Kfg";

        connect = DriverManager.getConnection(url, user, password);
        stat = connect.createStatement();//Statement enabling methods to communicate with database.
        innerStat = connect.createStatement();
    }

    /*
    The below method closes the connection to the database
    */
    public static void CloseConnection() throws Exception {
        stat.close();
        innerStat.close();
        connect.close();
    }

    public static String insertUser(String username, String email, String password) throws SQLException {
        try {
            OpenConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }

        String sql = "SELECT * FROM [User]";
        ResultSet thisR = stat.executeQuery(sql);

        String Name, Email;
        while (thisR.next()) {
            Name = thisR.getString("user_name");
            Email = thisR.getString("user_email");
            users.add(new User(Email, Name));
        }

        for (User lo : users) {
            if (lo.getUsername().equals(username) && lo.getEmail().equals(email)) {
                System.out.println("Username and Email duplication violation detected");
                users.clear();
                try {
                    CloseConnection();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "UsernameAndEmail";
            } else if (lo.getUsername().equals(username)) {
                System.out.println("Username duplication violation detected");
                users.clear();
                try {
                    CloseConnection();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "username";
            } else if (lo.getEmail().equals(email)) {
                System.out.println("Email duplication violation detected");
                users.clear();
                try {
                    CloseConnection();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "email";
            }
        }
        users.clear();
        String sql2 = "INSERT [User] VALUES('" + username + "','" + email + "','0','0','" + password + "',0,0)";
        stat.execute(sql2);
        try {
            CloseConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "both";
    }

    public static String verifyCredentials(String password, String email) throws SQLException {
        try {
            OpenConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String username = "";
        //executing the below SQL statement to add the above information.
        String sql = "SELECT * FROM [User]";
        ResultSet thisR = stat.executeQuery(sql);

        String Name, Email, Password;
        while (thisR.next()) {
            Name = thisR.getString("user_name");
            Email = thisR.getString("user_email");
            Password = thisR.getString("user_password");
            users.add(new User(Name, Password, Email));
        }
        for (User lo : users) {
            if (lo.getEmail().equals(email)) {
                if (lo.getPassword().equals(password)) {
                    username = lo.getUsername();
                    System.out.println("User " + username + " verified");
                    users.clear();
                    try {
                        CloseConnection();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return username;
                }
            }
        }
        users.clear();
        try {
            CloseConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return username;
    }

    public static void deleteUser(String username, String email) throws SQLException {
        try {
            OpenConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String sql2 = "DELETE FROM [User] WHERE user_name='" + username + "' AND user_email='" + email + "'";
        stat.execute(sql2);
        try {
            CloseConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Integer initiateUpdate(String block, String row, String seat, String user) throws SQLException {
        try {
            OpenConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Integer ID = updateLocation(block, row, seat, user);
        try {
            CloseConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ID;
    }

    public static Integer insertOrder(String quantity, Double price, String block, String row, String seat, String user) throws SQLException {
        try {
            OpenConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        dateString = dateFormat.format(date);

        DateFormat dateFormat2 = new SimpleDateFormat("HH:mm:ss");
        Date time = new Date();
        timeString = dateFormat2.format(time);

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MINUTE, 3);
        deliveryString = dateFormat2.format(cal.getTime());

        Integer ID = updateLocation(block, row, seat, user);

        String insertOrder = "INSERT INTO [Order] VALUES (" + Integer.parseInt(quantity) + ",'" + dateString + "','" + timeString + "','" + deliveryString + "'," + price + "," + ID + ")";
        stat.execute(insertOrder);

        String getID = "SELECT order_ID FROM [Order] WHERE time='" + timeString + "' AND user_ID='" + ID + "'";
        ResultSet result = stat.executeQuery(getID);
        Integer id = -1;
        while (result.next()) {
            id = result.getInt("order_ID");
        }
        try {
            CloseConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }

    public static Integer getOrderID(String time, String username) throws SQLException {
        try {
            OpenConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String getUserID = "SELECT user_ID FROM [User] WHERE user_name='" + username + "'";
        ResultSet result1 = stat.executeQuery(getUserID);
        Integer userID = 0;
        while (result1.next()) {
            userID = result1.getInt("user_ID");
        }
        String getOrderID = "SELECT * FROM [Order] WHERE user_ID=" + userID + " AND time='" + time + "'";
        ResultSet result2 = stat.executeQuery(getOrderID);
        Integer orderID = 0;
        while (result2.next()) {
            orderID = result2.getInt("order_ID");
        }
        try {
            CloseConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return orderID;
    }

    public static void removeOrder(String time, String username) throws SQLException {
        try {
            OpenConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String getUserID = "SELECT user_ID FROM [User] WHERE user_name='" + username + "'";
        ResultSet result1 = stat.executeQuery(getUserID);
        Integer userID = 0;
        while (result1.next()) {
            userID = result1.getInt("user_ID");
        }
        String removal = "DELETE FROM [Order] WHERE time='" + time + "' AND user_ID='" + userID + "'";
        stat.execute(removal);
        try {
            CloseConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String updateUser(String tempUser, String tempEmail, String user, String email, String cell, String phone) throws SQLException {
        try {
            OpenConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String sql = "SELECT * FROM [User]";
        ResultSet thisR = stat.executeQuery(sql);

        String Name, Email;
        while (thisR.next()) {
            Name = thisR.getString("user_name");
            Email = thisR.getString("user_email");
            users.add(new User(Email, Name));
        }

        for (User lo : users) {
            if (lo.getUsername().equals(user) && lo.getEmail().equals(email) && !tempEmail.equals(email) && !tempUser.equals(user)) {
                System.out.println("Username and Email duplication violation detected");
                users.clear();
                String updatePhones = "UPDATE [User] SET user_cell_number='" + cell + "',user_phone_number='" + phone + "' WHERE user_name='" + tempUser + "'";
                stat.execute(updatePhones);
                try {
                    CloseConnection();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "UsernameAndEmail";
            } else if (lo.getUsername().equals(user) && !tempUser.equals(user)) {
                System.out.println("Username duplication violation detected");
                users.clear();
                String updateEmailAndPhone = "UPDATE [User] SET user_email='" + email + "',user_cell_number='" + cell + "',user_phone_number='" + phone + "' WHERE user_name='" + tempUser + "'";
                stat.execute(updateEmailAndPhone);
                try {
                    CloseConnection();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "username";
            } else if (lo.getEmail().equals(email) && !tempEmail.equals(email)) {
                System.out.println("Email duplication violation detected");
                users.clear();
                String updateNameAndPhone = "UPDATE [User] SET user_name='" + user + "',user_cell_number='" + cell + "',user_phone_number='" + phone + "' WHERE user_name='" + tempUser + "'";
                stat.execute(updateNameAndPhone);
                try {
                    CloseConnection();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "email";
            }
        }
        users.clear();

        String update = "UPDATE [User] SET user_name='" + user + "',user_email='" + email + "',user_cell_number='" + cell + "',user_phone_number='" + phone + "' WHERE user_name='" + tempUser + "'";
        stat.execute(update);
        try {
            CloseConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "both";
    }

    public static void retrieveLocation(Main main) throws SQLException {
        try {
            OpenConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        blocks = new ArrayList<>();
        String blockData = "SELECT * FROM [Block]";
        ResultSet result1 = stat.executeQuery(blockData);
        String merge = main.getMerge();

        while (result1.next()) {
            Integer id = result1.getInt("block_ID");
            String name = result1.getString("block_name");
            Integer numRows = result1.getInt("block_rows");
            Double leftEntryDist = Double.valueOf(result1.getFloat("left_entry_distance"));
            Double rightEntryDist = Double.valueOf(result1.getFloat("right_entry_distance"));
            Integer leftEntry = result1.getInt("entry_left_row");
            Integer rightEntry = result1.getInt("entry_right_row");
            Double pathLength = Double.valueOf(result1.getFloat("path_length"));
            Double pathWidth = Double.valueOf(result1.getFloat("path_width"));
            Double seatWidth = Double.valueOf(result1.getFloat("seat_width"));

            String realName = main.getMapAssociationBlock(name);
            Block newBlock = new Block(name, realName, new ArrayList(), id, leftEntryDist, rightEntryDist, pathLength, pathWidth, seatWidth, leftEntry, rightEntry);
            for (int x = 1; x <= numRows; x++) {
                String sql = "SELECT * FROM [Row] WHERE row_number=" + x + " AND block_ID=" + id + "";
                ResultSet result2 = innerStat.executeQuery(sql);
                Integer seatC = 0;
                while (result2.next()) {
                    seatC = result2.getInt("row_seats");
                }
                newBlock.addRow(x, seatC);
            }
            blocks.add(newBlock);
        }
        Collections.sort(blocks, new blockNameComparator());
        if (merge != null)
            mergeBlock(merge, main);
        try {
            CloseConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void mergeBlock(String merge, Main main) {
        Block rightMerge = null, leftMerge = null;
        boolean found = false;
        Block duplicate = null;
        find:
        for (int i = 0; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            if (b.getName().equals(merge)) {
                rightMerge = getDuplicate(b);
                duplicate = b;
                leftMerge = blocks.get(i + 1);
                found = true;
                break find;
            }
        }
        if (found) {
            String leftName = leftMerge.getName();
            String rightName = rightMerge.getName();

            Pair<String, Block> leftPair = new Pair<>(leftName, leftMerge);
            Pair<String, Block> rightPair = new Pair<>(rightName, rightMerge);
            main.setPair(leftPair, 0);
            main.setPair(rightPair, 1);
        }
        mergeBlocks(duplicate, leftMerge);
    }

    private static Block getDuplicate(Block b) {
        String name = b.getName();
        ArrayList<Block.Row> rows = new ArrayList<>();
        Integer blockID = b.getBlockID();
        String realName = b.getRealName();
        Double leftEntryDistance = b.getLeftEntryDistance();
        Double rightEntryDistance = b.getRightEntryDistance();
        Double pathLength = b.getPathLength();
        Double pathWidth = b.getPathWidth();
        Double seatWidth = b.getSeatWidth();
        Integer leftEntryRow = b.getLeftEntryRow();
        Integer rightEntryRow = b.getRightEntryRow();

        Block duplicate = new Block(name, realName, rows, blockID, leftEntryDistance, rightEntryDistance, pathLength, pathWidth, seatWidth, leftEntryRow, rightEntryRow);
        for (Block.Row r : b.getRows()) {
            Integer seatCount = r.getSeatCount();
            Integer number = r.getNumber();
            duplicate.addRow(number, seatCount);
        }
        return duplicate;
    }

    private static void mergeBlocks(Block rightMerge, Block leftMerge) {
        Block.Row curLeft;
        Integer newSeatCount;
        for (int i = 0; i < rightMerge.getRows().size(); i++) {
            curLeft = leftMerge.getRows().get(i);
            newSeatCount = rightMerge.getRows().get(i).getSeatCount() + curLeft.getSeatCount();
            rightMerge.getRows().get(i).setSeatCount(newSeatCount);
        }
        blocks.remove(blocks.get(blocks.size() - 1));
        blocks.remove(blocks.get(blocks.size() - 1));
        blocks.add(rightMerge);
    }

    public static Integer updateLocation(String block, String row, String seat, String user) throws SQLException {
        String blockIDQ = "SELECT block_ID FROM [Block] WHERE block_name='" + block + "'";
        ResultSet result1 = stat.executeQuery(blockIDQ);
        Integer blockID = 0;
        while (result1.next()) {
            blockID = result1.getInt("block_ID");
        }
        String rowIDQ = "SELECT row_ID FROM [Row] WHERE block_ID='" + blockID + "' AND row_number='" + row + "'";
        ResultSet result2 = stat.executeQuery(rowIDQ);
        Integer rowID = 0;
        while (result2.next()) {
            rowID = result2.getInt("row_ID");
        }

        String getUser = "SELECT * FROM [User] WHERE user_name='" + user + "'";
        ResultSet thisR = stat.executeQuery(getUser);
        Integer ID = 0;
        while (thisR.next()) {
            ID = thisR.getInt("user_ID");
        }

        Integer seatNum = Integer.parseInt(seat);

        String update = "UPDATE [User] SET seat=" + seatNum + ",row_ID=" + rowID + " WHERE user_name='" + user + "'";
        stat.execute(update);
        return ID;
    }

    public static void updatePassword(String password, String e_mail, String username) throws SQLException {
        try {
            OpenConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String update = "UPDATE [User] SET user_password='" + password + "' WHERE user_name='" + username + "' AND user_email='" + e_mail + "'";
        stat.execute(update);
        try {
            CloseConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Dispatch> buildDispatches(Dispatch_Junction junction, Main main) throws SQLException {
        try {
            OpenConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ArrayList<Dispatch> dispatches = new ArrayList<>();

        String fetchDispatch = "SELECT * FROM [Dispatch_Point]";
        ResultSet result1 = stat.executeQuery(fetchDispatch);

        Integer dID = 0;
        while (result1.next()) {
            String name = result1.getString("dispatch_name");

            String fetchRelations = "SELECT * FROM [Block_Dispatch_Relation] WHERE dispatch_name='" + name + "'";
            ResultSet result2 = innerStat.executeQuery(fetchRelations);

            String rightBlock = "";
            String centreBlock = "";
            String leftBlock = "";
            int count = 0;
            while (result2.next()) {
                if (count == 0) {
                    rightBlock = result2.getString("block_name");
                } else if (count == 1) {
                    centreBlock = result2.getString("block_name");
                } else {
                    leftBlock = result2.getString("block_name");
                    count = -1;
                }
                count++;
            }
            Block rightB = null, centreB = null, leftB = null;
            for (Block b : blocks) {
                if (b.getName().equals(rightBlock))
                    rightB = b;
                else if (b.getName().equals(leftBlock))
                    leftB = b;
                else if (b.getName().equals(centreBlock))
                    centreB = b;
            }

            Dispatch newD = new Dispatch(dID, name, rightB, centreB, leftB, new ArrayList<>(), main.getCapacity());
            dID++;
            dispatches.add(newD);
        }
        try {
            CloseConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dispatches;
    }

    public static Block returnBlock(String block) {
        for (Block b : blocks) {
            if (b.getName().equals(block))
                return b;
        }
        return null;
    }

    public static Block returnRealBlock(String block) {
        for (Block b : blocks) {
            if (b.getRealName().equals(block))
                return b;
        }
        return null;
    }

    public static String getBlockName(Integer orderID) throws SQLException {
        try {
            OpenConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String user = "SELECT * FROM [Order] WHERE order_ID=" + orderID + "";
        ResultSet result = stat.executeQuery(user);
        Integer id = 0;
        while (result.next()) {
            id = result.getInt("user_ID");
        }
        String row = "SELECT * FROM [User] WHERE user_ID=" + id + "";
        ResultSet result1 = stat.executeQuery(row);
        Integer rowIndex = 0;
        while (result1.next()) {
            rowIndex = result1.getInt("row_ID");
        }
        String block = "SELECT * FROM [Row] WHERE row_ID=" + rowIndex + "";
        ResultSet result2 = innerStat.executeQuery(block);
        Integer blockID = 0;
        while (result2.next()) {
            blockID = result2.getInt("block_ID");
        }
        String block_name = "SELECT * FROM [Block] WHERE block_ID=" + blockID + "";
        ResultSet result3 = innerStat.executeQuery(block_name);
        String blockName = "";
        while (result3.next()) {
            blockName = result3.getString("block_name");
        }
        try {
            CloseConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return blockName;
    }

    public static void insertOrderHistory(ArrayList<ArrayList<Order>> structure, String dispatch) throws SQLException {
        try {
            OpenConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String deliveryID = "SELECT deliveryID FROM [Order_History] WHERE OrderID=(SELECT MAX(OrderID) FROM [Order_History])";
        ResultSet result2 = stat.executeQuery(deliveryID);
        Integer lastID = 0;
        while (result2.next()) {
            lastID = result2.getInt("deliveryID");
        }
        Integer nextID = lastID + 1;
        for (ArrayList<Order> ar : structure) {
            for (Order o : ar) {
                String getUserID = "SELECT user_ID FROM [User] WHERE user_name='" + o.getUser() + "'";
                ResultSet result1 = stat.executeQuery(getUserID);
                Integer userID = 0;
                while (result1.next()) {
                    userID = result1.getInt("user_ID");
                }

                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
                Date date = new Date();
                dateString = dateFormat.format(date);

                String insertHistory = "INSERT INTO [Order_History] VALUES (" + o.getQuantity() + "," + o.getTotal() + ",'" + dateString + "','" + o.getBlock().getName() + "','" + dispatch + "'," + nextID + ")";
                stat.execute(insertHistory);

                String removal = "DELETE FROM [Order] WHERE order_ID=" + o.getOrderID() + " AND user_ID='" + userID + "'";
                stat.execute(removal);
            }
        }
        try {
            CloseConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String buildHistoryList(String date, int dispatchNum, Main main) throws SQLException {
        try {
            OpenConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String historyString = "SELECT * FROM [Order_History] WHERE date='" + date + "'";
        ResultSet result = stat.executeQuery(historyString);

        Integer[] dispatchCount = new Integer[dispatchNum];
        Double[] dispatchIncome = new Double[dispatchNum];
        for (int i = 0; i < dispatchCount.length; i++) {
            dispatchCount[i] = 0;
            dispatchIncome[i] = 0.0;
        }
        int blockSize = 0;
        if (main.getMerge() == null) {
            blockSize = blocks.size();
        } else {
            blockSize = blocks.size() + 1;
        }
        Integer[] blockCount = new Integer[blockSize];
        Double[] blockIncome = new Double[blockSize];
        for (int i = 0; i < blockCount.length; i++) {
            blockCount[i] = 0;
            blockIncome[i] = 0.0;
        }
        int totalCount = 0, orderCount = 0;
        double totalIncome = 0.0;
        Integer curID = 0;
        int deliveryCount = 0;
        while (result.next()) {
            Integer quantity = result.getInt("quantity");
            Double income = Double.valueOf(result.getFloat("income"));
            String blockName = result.getString("blockName");
            String dispatch = result.getString("dispatchName");
            Integer deliveryID = result.getInt("deliveryID");
            if (curID != deliveryID) {
                curID = deliveryID;
                deliveryCount++;
            }

            Integer dispatchID = main.getDispatchID(dispatch);
            Integer blockNum = Integer.parseInt(blockName);
            blockCount[blockNum] = blockCount[blockNum] + quantity;
            blockIncome[blockNum] = blockIncome[blockNum] + income;
            dispatchCount[dispatchID] = dispatchCount[dispatchID] + quantity;
            dispatchIncome[dispatchID] = dispatchIncome[dispatchID] + income;
            totalCount += quantity;
            orderCount++;
            totalIncome += income;
        }
        beerCountComparator compareBeer = new beerCountComparator();
        incomeComparator compareIncome = new incomeComparator();

        Arrays.sort(blockCount, compareBeer);
        Arrays.sort(dispatchCount, compareBeer);
        Arrays.sort(blockIncome, compareIncome);
        Arrays.sort(dispatchIncome, compareIncome);

        String maxBeerBlock = String.valueOf(blockCount[blockCount.length - 1]);
        String minBeerBlock = String.valueOf(blockCount[0]);
        String maxIncomeBlock = String.valueOf(blockIncome[blockIncome.length - 1]);
        String minIncomeBlock = String.valueOf(blockIncome[0]);
        String maxBeerDispatch = String.valueOf(dispatchCount[dispatchCount.length - 1]);
        String minBeerDispatch = String.valueOf(dispatchCount[0]);
        String maxIncomeDispatch = String.valueOf(dispatchIncome[dispatchIncome.length - 1]);
        String minIncomeDispatch = String.valueOf(dispatchIncome[0]);

        Double avgBeerPerOrder;
        Double avgBeerPerDelivery;
        if (orderCount == 0) {
            avgBeerPerOrder = 0.0;
            avgBeerPerDelivery = 0.0;
        } else {
            avgBeerPerOrder = totalCount * 1.0 / orderCount * 1.0;
            avgBeerPerDelivery = totalCount * 1.0 / deliveryCount * 1.0;
        }
        Double avgBeerPerDispatch = totalCount * 1.0 / dispatchNum * 1.0;
        Double avgBeerPerBlock = totalCount * 1.0 / blockSize * 1.0;

        Double avgIncomePerOrder;
        Double avgIncomePerDelivery;
        if (orderCount == 0) {
            avgIncomePerOrder = 0.0;
            avgIncomePerDelivery = 0.0;
        } else {
            avgIncomePerOrder = totalIncome * 1.0 / orderCount * 1.0;
            avgIncomePerDelivery = totalIncome * 1.0 / deliveryCount * 1.0;
        }
        Double avgIncomePerDispatch = totalIncome * 1.0 / dispatchNum * 1.0;
        Double avgIncomePerBlock = totalIncome * 1.0 / blockSize * 1.0;

        String finalHistory = "";
        finalHistory = finalHistory.concat(date).concat("@").concat(String.valueOf(totalCount)).concat("@").concat(String.valueOf(avgBeerPerDelivery)).
                concat("@").concat(String.valueOf(avgBeerPerOrder)).concat("@").concat(String.valueOf(avgBeerPerDispatch)).
                concat("@").concat(String.valueOf(avgBeerPerBlock)).concat("@").concat(maxBeerBlock).concat("@").concat(maxBeerDispatch).concat("@").
                concat(minBeerBlock).concat("@").concat(minBeerDispatch).concat("@").concat(String.valueOf(totalIncome)).concat("@").
                concat(String.valueOf(avgIncomePerDelivery)).concat("@").concat(String.valueOf(avgIncomePerOrder))
                .concat("@").concat(String.valueOf(avgIncomePerDispatch)).concat("@").concat(String.valueOf(avgIncomePerBlock)).concat("@")
                .concat(maxIncomeBlock).concat("@").concat(maxIncomeDispatch).concat("@").concat(minIncomeBlock).concat("@").concat(minIncomeDispatch);
        try {
            CloseConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return finalHistory;
    }

    public static ArrayList<Block> getBlocks() {
        return blocks;
    }

    public static String getDateString() {
        return dateString;
    }

    public static String getTimeString() {
        return timeString;
    }

    public static class beerCountComparator implements Comparator<Integer> {

        @Override
        public int compare(Integer integer, Integer t1) {
            return integer.compareTo(t1);
        }
    }

    public static class incomeComparator implements Comparator<Double> {

        @Override
        public int compare(Double integer, Double t1) {
            return integer.compareTo(t1);
        }
    }

    public static class blockNameComparator implements Comparator<Block> {
        @Override
        public int compare(Block b1, Block b2) {
            Integer i1 = Integer.parseInt(b1.getName());
            Integer i2 = Integer.parseInt(b2.getName());
            return (i1.compareTo(i2));
        }
    }
}
