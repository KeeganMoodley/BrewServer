import javafx.util.Pair;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by s213463695 on 2016/03/22.
 */
public class Main {

    protected HashMap<String, Connection> spectatorConnections;
    protected HashMap<String, Connection> dispatchConnections;
    protected HashMap<String, String> blockConversions;
    protected HashMap<String, Pair<String, Double>> dispatchConversion;
    protected HashMap<String, Integer> dispatchIds;
    private Dispatch_Junction junction;
    private Lock connectionLock = new ReentrantLock();
    private Lock dispatchLock = new ReentrantLock();
    private Lock optimalOutputLock = new ReentrantLock();
    private Boolean event = true;
    private Boolean doNotAdd = false;
    private int dispatchNum = 0;
    public Double price = 0.0;
    private Double shift = 0.0;
    private Integer time = 0;
    private TimerThread timer = null;
    private Integer capacity = 0;
    private Boolean test = false;
    private Pair<String, Block>[] merge = new Pair[2];

    public void addUser(String username, String password, String email, Connection connection) {
        if (username.equals(""))
            connection.notifySaving(false, "empty", event);
        connectionLock.lock();
        String saved;
        try {
            saved = DB_Controller.insertUser(username, email, password);
            if (saved.equals("both")) {
                spectatorConnections.put(username, connection);
                System.out.println("User: " + username + " has been stored");
                connection.notifySaving(true, "nothing", event);
            } else if (saved.equals("email")) {
                connection.notifySaving(false, "duplicateE", event);
            } else if (saved.equals("username")) {
                connection.notifySaving(false, "duplicateU", event);
            } else {
                connection.notifySaving(false, "duplicateEU", event);
            }
        } catch (SQLException e) {
            System.out.println("Unable to store connection...");
            connection.notifySaving(false, "database", event);
        } finally {
            connectionLock.unlock();
        }
    }

    public void checkUser(String password, String email, Connection connection) {
        connectionLock.lock();
        System.out.println("Verifying user...");
        String username = "";
        try {
            username = DB_Controller.verifyCredentials(password, email);
            if (!username.equals("")) {
                spectatorConnections.put(username, connection);
                connection.username = username;
                System.out.println("User " + username + " is active now");
                connection.notifyVerification(true, username, event);
            } else {
                System.out.println("Unable to verify user ...");
                connection.notifyVerification(false, username, event);
            }
        } catch (Exception e) {
            System.out.println("Unable to verify user ...");
            connection.notifyVerification(false, "#dbFail#", event);
        } finally {
            connectionLock.unlock();
        }
    }

    public void removeUser(String username, String context, Connection connection) {
        connectionLock.lock();
        try {
            if (username != null && !context.contains("@")) {
                spectatorConnections.remove(username);
                System.out.println("User " + username + " logout successfully");
            } else if (context.contains("@")) {
                DB_Controller.deleteUser(username, context);
                System.out.println("User " + username + " removal successfully");
            }
            connection.notifyLogout(true, context);
        } catch (Exception e) {
            System.out.println("Unable to logout/remove ...");
            connection.notifyLogout(false, context);
        } finally {
            connectionLock.unlock();
        }
    }

    public void returnPrice(Connection connection) {
        connectionLock.lock();
        try {
            connection.returnPrice(price);
        } catch (Exception e) {
        } finally {
            connectionLock.unlock();
        }
    }

    public void triggerOrderInsert(String quantity, String block, String row, String seat, String setting, String user, String androidIndex, Connection connection, Boolean test) {
        connectionLock.lock();
        doNotAdd = false;
        try {
            if (setting.equals("no_setting")) {
                String initialBlock = block;
                String initialSeat = seat;
                String oddCase = null;
                String mappedOdd = "";
                try {
                    oddCase = merge[0].getKey();
                    mappedOdd = blockConversions.get(oddCase);
                } catch (NullPointerException e) {
                }
                String blockKey = "";
                if (oddCase != null) {
                    if (block.equals(mappedOdd)) {
                        blockKey = merge[1].getKey();
                        block = blockConversions.get(blockKey);
                        seat = String.valueOf(merge[1].getValue().getRows().get(Integer.parseInt(row)).getSeatCount() + Integer.parseInt(seat));
                    }
                }
                Integer quant = Integer.parseInt(quantity);
                Double total = quant * price;
                Block B = DB_Controller.returnRealBlock(block);
                Block virtual = mapToVirtual(seat, B, null, row);

                boolean active = false;
                int virtualInt = Integer.parseInt(virtual.getName());
                if (dispatchConnections.get(virtual.getName()) != null) {
                    active = true;
                } else if (virtualInt % 2 != 0) {
                    Integer nextDispatchInt = virtualInt + 1;
                    Integer previousDispatchInt = virtualInt - 1;
                    String nextDispatch = "";
                    if (nextDispatchInt.equals(DB_Controller.getBlocks().size())) {
                        nextDispatch = "0";
                    } else {
                        nextDispatch = String.valueOf(nextDispatchInt);
                    }
                    String previousDispatch = String.valueOf(previousDispatchInt);
                    if (dispatchConnections.get(nextDispatch) != null && dispatchConnections.get(previousDispatch) != null) {
                        active = true;
                    }
                }

                if (active) {
                    Integer orderID = DB_Controller.insertOrder(quantity, total, virtual.getName(), row, seat, user);
                    Date dateStamp = new Date();
                    Order newO = new Order(orderID, B, Integer.parseInt(row), quant, total, user, Integer.parseInt(seat), androidIndex, dateStamp);
                    mapToVirtual(seat, B, newO, row);
                    junction.insertOrder(newO);

                    String time = DB_Controller.getTimeString();
                    String date = DB_Controller.getDateString();
                    if (!doNotAdd) {
                        Integer blockName = Integer.parseInt(virtual.getName());
                        if (blockName % 2 == 0) {
                            Connection con = dispatchConnections.get(virtual.getName());
                            con.addToDesktop(orderID, initialBlock, row, initialSeat, user, quantity, total, time);
                        } else {
                            String prevDispatch = "";
                            String nextDispatch = "";
                            if (blockName != 0 && blockName != DB_Controller.getBlocks().size() - 1) {
                                prevDispatch = String.valueOf(blockName - 1);
                                nextDispatch = String.valueOf(blockName + 1);
                            } else if (blockName != DB_Controller.getBlocks().size() - 1) {
                                nextDispatch = String.valueOf(blockName + 1);
                                prevDispatch = String.valueOf(DB_Controller.getBlocks().size() - 1);
                            } else if (blockName != 0) {
                                prevDispatch = String.valueOf(blockName - 1);
                                nextDispatch = "0";
                            } else {
                                prevDispatch = String.valueOf(DB_Controller.getBlocks().size() - 1);
                                nextDispatch = "0";
                            }
                            Connection con1 = dispatchConnections.get(prevDispatch);
                            Connection con2 = dispatchConnections.get(nextDispatch);
                            con1.addToDesktop(orderID, initialBlock, row, initialSeat, user, quantity, total, time);
                            con2.addToDesktop(orderID, initialBlock, row, initialSeat, user, quantity, total, time);
                        }
                        if (!test)
                            connection.notifyOrderInsertion(time, date, quantity, total, androidIndex);
                    } else {
                        doNotAdd = false;
                        if (!test) {
                            connection.notifyOrderInsertion(time, date, quantity, total, androidIndex);
                            connection.notifyOrderDispatch(newO);
                        }
                    }
                } else {
                    connection.notifyDispatchDown();
                }
            } else {
                DB_Controller.initiateUpdate(block, row, seat, user);
                connection.notifyLocationUpdate();
            }
        } catch (Exception e) {
            System.out.println("Insertion Error");
        } finally {
            connectionLock.unlock();
        }
    }

    private Block mapToVirtual(String seat, Block b, Order newO, String row) {
        Integer seatCount = b.getRows().get(Integer.parseInt(row) - 1).getSeatCount();
        Integer shiftSeats = (int) (seatCount * shift);
        Block virtualBlock;
        Integer virtualSeat;
        if (Integer.parseInt(seat) + shiftSeats > seatCount) {
            virtualSeat = Integer.parseInt(seat) + shiftSeats - seatCount;
            String lastBlock = DB_Controller.getBlocks().get(DB_Controller.getBlocks().size() - 1).getName();
            if (b.getName().equals(lastBlock)) {
                virtualBlock = DB_Controller.returnBlock("0");
            } else {
                Integer virtualBlockInt = Integer.parseInt(b.getName()) + 1;
                virtualBlock = DB_Controller.returnBlock(String.valueOf(virtualBlockInt));
            }
        } else {
            virtualSeat = Integer.parseInt(seat) + shiftSeats;
            virtualBlock = b;
        }
        if (newO != null) {
            newO.setBlock(virtualBlock);
            newO.setSeat(virtualSeat);
        }
        return virtualBlock;
    }

    public void removeOrder(String time, String username, Connection connection) {
        connectionLock.lock();
        try {
            Integer orderID = DB_Controller.getOrderID(time, username);
            String block = DB_Controller.getBlockName(orderID);
            DB_Controller.removeOrder(time, username);
            junction.removeOrder(username, orderID, block);
            removeFromDesktop(block, orderID, username);
            connection.notifyOrderRemoval(true, time);
        } catch (Exception e) {
            connection.notifyOrderRemoval(false, time);
        } finally {
            connectionLock.unlock();
        }
    }

    private void removeFromDesktop(String block, Integer orderID, String username) {
        Integer blockName = Integer.valueOf(block);
        if (blockName % 2 == 0) {
            try {
                Connection con = dispatchConnections.get(block);
                con.removeFromDesktop(orderID, username);
            } catch (Exception e) {
                Connection con1 = dispatchConnections.get(String.valueOf(blockName - 2));
                Connection con2 = dispatchConnections.get("0");
                con1.removeFromDesktop(orderID, username);
                con2.removeFromDesktop(orderID, username);
            }
        } else {
            String prevDispatch = assignPrev(blockName);
            String nextDispatch = assignNext(blockName);

            Connection con1 = dispatchConnections.get(prevDispatch);
            Connection con2 = dispatchConnections.get(nextDispatch);
            con1.removeFromDesktop(orderID, username);
            con2.removeFromDesktop(orderID, username);
        }
    }

    private String assignNext(Integer blockName) {
        String nextDispatch = "";
        if (blockName != DB_Controller.getBlocks().size() - 1)
            nextDispatch = String.valueOf(blockName + 1);
        else
            nextDispatch = "0";
        return nextDispatch;
    }

    private String assignPrev(Integer blockName) {
        String prevDispatch = "";
        if (blockName != 0)
            prevDispatch = String.valueOf(blockName - 1);
        else
            prevDispatch = String.valueOf(DB_Controller.getBlocks().size() - 1);
        return prevDispatch;
    }

    public void storeUserProfile(String tempUser, String tempEmail, String user, String email, String cell, String phone, Connection connection) {
        connectionLock.lock();
        if (user.equals("") && email.equals("")) {
            user = tempUser;
            email = tempEmail;
        } else if (user.equals("")) {
            user = tempUser;
        } else if (email.equals("")) {
            email = tempEmail;
        }
        String updated;
        try {
            updated = DB_Controller.updateUser(tempUser, tempEmail, user, email, cell, phone);
            if (updated.equals("both")) {
                spectatorConnections.remove(tempUser);
                spectatorConnections.put(user, connection);
                System.out.println("User: " + tempUser + " has been updated to user: " + user);
                System.out.println("Email: " + tempEmail + " has been updated to email: " + email);
                System.out.println("Cell-Phone: has been updated to: " + cell);
                System.out.println("Home-Phone: has been updated to: " + phone);
                connection.notifyUpdating(true, "nothing", tempUser, tempEmail);
            } else if (updated.equals("email")) {
                spectatorConnections.remove(tempUser);
                spectatorConnections.put(user, connection);
                System.out.println("User: " + tempUser + " has been updated to user: " + user);
                System.out.println("Cell-Phone: has been updated to: " + cell);
                System.out.println("Home-Phone: has been updated to: " + phone);
                connection.notifyUpdating(false, "duplicateE", tempUser, tempEmail);
            } else if (updated.equals("username")) {
                System.out.println("Email: " + tempEmail + " has been updated to email: " + email);
                System.out.println("Cell-Phone: has been updated to: " + cell);
                System.out.println("Home-Phone: has been updated to: " + phone);
                connection.notifyUpdating(false, "duplicateU", tempUser, tempEmail);
            } else {
                System.out.println("Cell-Phone: has been updated to: " + cell);
                System.out.println("Home-Phone: has been updated to: " + phone);
                connection.notifyUpdating(false, "duplicateEU", tempUser, tempEmail);
            }
        } catch (SQLException e) {
            System.out.println("Unable to update connection...");
            connection.notifyUpdating(false, "database", tempUser, tempEmail);
        } finally {
            connectionLock.unlock();
        }
    }

    public void getLocations(Connection connection) {
        connectionLock.lock();
        try {
            ArrayList<Block> blocks = DB_Controller.getBlocks();
            if (blocks.size() > 0)
                connection.sendLocations(blocks, true);
            else
                connection.sendLocations(blocks, false);
        } catch (Exception e) {
        } finally {
            connectionLock.unlock();
        }
    }

    public void getUpdate(Connection connection) {
        connectionLock.lock();
        try {
            InputStream in = Main.class.getResourceAsStream("/Update");
            Scanner scan = new Scanner(in);

            String year = "";
            String month = "";
            String day = "";
            while (scan.hasNext()) {
                year = scan.nextLine();
                month = scan.nextLine();
                day = scan.nextLine();
            }
            String date = year + "/" + month + "/" + day;
            connection.sendUpdate(date);
        } catch (Exception e) {
        } finally {
            connectionLock.unlock();
        }
    }

    public void updatePassword(String password, String e_mail, String username, Connection connection) {
        connectionLock.lock();
        try {
            DB_Controller.updatePassword(password, e_mail, username);
            connection.notifyPasswordUpdate(true, password);
        } catch (Exception e) {
            connection.notifyPasswordUpdate(false, password);
        } finally {
            connectionLock.unlock();
        }
    }

    public void registerDispatch(String dispatchName, Connection connection) {
        dispatchLock.lock();
        try {
            dispatchConnections.put(dispatchName, connection);
            System.out.println("Dispatch: " + dispatchName + " registered");
        } catch (Exception e) {
        } finally {
            dispatchLock.unlock();
        }
    }

    public void fetchHistory(String date, Connection connection) {
        dispatchLock.lock();
        try {
            String history = DB_Controller.buildHistoryList(date, dispatchNum, this);
            connection.sendHistory(history);
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            dispatchLock.unlock();
        }
    }

    public Integer getDispatchID(String name) {
        return dispatchIds.get(name);
    }

    public static void main(String[] args) throws Exception {
        new Main();
    }

    private void readConversion() {
        blockConversions = new HashMap<>();
        dispatchConversion = new HashMap<>();
        try {
            InputStream in = Main.class.getResourceAsStream("/GenericData.txt");
            Scanner scan = new Scanner(in);

            String input;
            String command = "";
            String[] curInfo = new String[3];
            int mergeCount = 0;
            while (scan.hasNext()) {
                input = scan.nextLine();
                if (input.equals("shift") || input.equals("dispatch_#")
                        || input.equals("block_#") || input.equals("merge_#")
                        || input.equals("oddBlock") || input.equals("timer")
                        || input.equals("cluster_capacity") || input.equals("beer_price")) {
                    command = input;
                } else {
                    switch (command) {
                        case "shift":
                            shift = Double.parseDouble(input);
                            break;
                        case "dispatch_#":
                            curInfo = input.split(";");
                            Pair<String, Double> newPair = new Pair<>(curInfo[1], Double.parseDouble(curInfo[2]));
                            dispatchConversion.put(curInfo[0], newPair);
                            break;
                        case "block_#":
                            curInfo = input.split(";");
                            blockConversions.put(curInfo[0], curInfo[1]);
                            break;
                        case "merge_#":
                            Pair<String, Block> newP = new Pair(input, null);
                            merge[mergeCount] = newP;
                            mergeCount++;
                            break;
                        case "timer":
                            time = Integer.parseInt(input);
                            break;
                        case "cluster_capacity":
                            capacity = Integer.parseInt(input);
                            break;
                        case "beer_price":
                            price = Double.parseDouble(input);
                        default:
                            break;
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    public String getMapAssociationBlock(String key) {
        return blockConversions.get(key);
    }

    public Main() throws Exception {
        readConversion();
        DB_Controller.retrieveLocation(this);

        spectatorConnections = new HashMap();
        dispatchConnections = new HashMap<>();
        dispatchIds = new HashMap<>();
        ArrayList<Dispatch> dispatches = DB_Controller.buildDispatches(junction, this);
        for (Dispatch d : dispatches) {
            dispatchIds.put(d.getDispatchName(), d.getDispatchID());
        }
        dispatchNum = dispatches.size();
        this.junction = new Dispatch_Junction(dispatches, this);
        ServerSocket server = null;
        timer = new TimerThread();
        timer.start();
        int size = DB_Controller.getBlocks().size();
        try {
            System.out.println("Initiating Server...");
            server = new ServerSocket(8050);

            while (true) {
                Socket socket = server.accept();
                System.out.println("Connection has been established");

                Connection connection = new Connection(this, socket);
                connection.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Thread.sleep(100);
        } finally {
            server.close();
        }
    }

    public void sendOptimalPath(ArrayList<ArrayList<Order>> structure, Integer optimum, Boolean centreOnly, Cluster cluster, Dispatch d, Dispatch l, Dispatch r, Double totalDistance) {
        optimalOutputLock.lock();

        ArrayList<String> outputStream = new ArrayList<>();
        String dispatch = "";
        String virtualDispatch = "";
        String entrance = "";
        Pair<String, Double> curP = null;
        Block firstB = structure.get(0).get(0).getBlock();
        Block firstBR = structure.get(0).get(0).getRealBlock();
        if (!centreOnly) {
            switch (optimum) {
                case 0:
                    entrance = "left";
                    virtualDispatch = r.getDispatchName();
                    curP = dispatchConversion.get(virtualDispatch);
                    totalDistance += curP.getValue();
                    dispatch = curP.getKey();
                    break;
                case 1:
                    entrance = "right";
                    virtualDispatch = d.getDispatchName();
                    curP = dispatchConversion.get(virtualDispatch);
                    totalDistance += curP.getValue();
                    dispatch = curP.getKey();
                    break;
                case 2:
                    entrance = "left";
                    virtualDispatch = d.getDispatchName();
                    curP = dispatchConversion.get(virtualDispatch);
                    totalDistance += curP.getValue();
                    dispatch = curP.getKey();
                    break;
                case 3:
                    entrance = "right";
                    virtualDispatch = l.getDispatchName();
                    curP = dispatchConversion.get(virtualDispatch);
                    totalDistance += curP.getValue();
                    dispatch = curP.getKey();
                    break;
            }
            int difference = minDistance(structure, virtualDispatch);
            if (shift != 0.0 && difference != 0) {
                totalDistance -= curP.getValue();
                if (difference == -1) {
                    dispatch = dispatchConversion.get(String.valueOf(Integer.parseInt(virtualDispatch) - 2)).getKey();
                } else {
                    dispatch = dispatchConversion.get(String.valueOf(Integer.parseInt(virtualDispatch) + 2)).getKey();
                }
                int size = firstB.getRows().size();
                double seatWidth = firstB.getSeatWidth();
                totalDistance -= firstB.getRows().get(size - 1).getSeatCount() * seatWidth;
            }
        } else {
            virtualDispatch = d.getDispatchName();
            curP = dispatchConversion.get(virtualDispatch);
            totalDistance += curP.getValue();
            dispatch = curP.getKey();
            if (optimum == 0 || optimum == 1) {
                entrance = "left";
            } else {
                entrance = "right";
            }
        }
        DecimalFormat df = new DecimalFormat("#0.00");
        DecimalFormat df1 = new DecimalFormat("##.##");
        outputStream.add("Optimal Path:");
        outputStream.add("@");
        String initialReal = firstBR.getRealName();
        String initial = firstB.getName();
        outputStream.add("Grand Total: R" + df.format(cluster.getIncome()));
        outputStream.add("Total Distance: " + df1.format(totalDistance) + "m");
        outputStream.add("Total Beer: x" + cluster.getQuantity());
        outputStream.add("@");
        if (checkExclusiveCase(structure, firstB)) {
            if (shift != 0.0) {
                Integer dispatchIndex = Integer.parseInt(virtualDispatch);
                dispatchIndex += 2;
                dispatch = String.valueOf(dispatchIndex);
            } else {
                Integer dispatchIndex = Integer.parseInt(dispatch);
                dispatchIndex += 2;
                dispatch = String.valueOf(dispatchIndex);
                entrance = "right";
            }
        }
        if (shift != 0.0) {
            entrance = firstBR.getRealName();
        }
        outputStream.add("Dispatch Point: " + dispatch);
        outputStream.add("Entrance: " + entrance);
        outputStream.add("@");
        outputStream.add("Block: " + initialReal);
        Connection curC;
        for (ArrayList<Order> ar : structure) {
            for (int i = 0; i < ar.size(); i++) {
                Order or = ar.get(i);
                String oddCase = null;
                String curBReal = or.getRealBlock().getRealName();
                String curB = or.getBlock().getName();

                if (shift == 0.0) {
                    if (!curB.equals(initial)) {
                        outputStream.add("@");
                        outputStream.add("Block: " + curBReal);
                        initial = curB;
                    }
                } else {
                    if (!curBReal.equals(initialReal)) {
                        outputStream.add("@");
                        outputStream.add("Block: " + curBReal);
                        initialReal = curBReal;
                    }
                }

                try {
                    oddCase = merge[1].getKey();
                } catch (NullPointerException e) {
                }
                if (oddCase != null) {
                    if (curB.equals(oddCase)) {
                        checkPotentialOdd(or, outputStream);
                    }
                }
                outputStream.add("Row: " + or.getRowNumber() + " -> Seat: " + or.getRealSeat() + " | Quantity: " + or.getQuantity() + ", Price: R" + df.format(or.getTotal()));
                curC = spectatorConnections.get(or.getUser());
                if (!test) {
                    if (curC != null) {
                        curC.notifyOrderDispatch(or);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        ArrayList<Order> lastList = structure.get(structure.size() - 1);
        Order lastOr = lastList.get(lastList.size() - 1);
        if (shift == 0.0) {
            writeExitToFile(outputStream, lastOr, l, r, d);
        } else {
            outputStream.add("@");
            outputStream.add("Exit Block: " + lastOr.getRealBlock().getRealName());
        }
        try {
            DB_Controller.insertOrderHistory(structure, virtualDispatch);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Connection con = dispatchConnections.get(virtualDispatch);
        con.triggerPrintOut(structure, outputStream);
        Block originalBlock = null;
        for (ArrayList<Order> ar : structure) {
            for (Order or : ar) {
                try {
                    originalBlock = merge[1].getValue();
                } catch (NullPointerException e) {
                }
                if (originalBlock != null) {
                    if (or.getBlock().getName().equals(originalBlock.getName())) {
                        Integer seatCount = originalBlock.getRows().get(or.getRowNumber() - 1).getSeatCount();
                        if (seatCount < or.getSeat()) {
                            or.setBlock(merge[0].getValue());
                        }
                    }
                }
                removeFromDesktop(or.getBlock().getName(), or.getOrderID(), or.getUser());
            }
        }
        doNotAdd = true;
        optimalOutputLock.unlock();
    }

    private int minDistance(ArrayList<ArrayList<Order>> structure, String dispatch) {
        for (ArrayList<Order> ar : structure) {
            for (Order or : ar) {
                Integer blockIndex = Integer.parseInt(or.getRealBlock().getName());
                Integer dispatchIndex = Integer.parseInt(dispatch);
                int difference = blockIndex - dispatchIndex;
                if (difference <= -2) {
                    return -1;
                } else if (difference >= 2) {
                    return 1;
                }
            }
        }
        return 0;
    }

    private void writeExitToFile(ArrayList<String> outputStream, Order lastOr, Dispatch l, Dispatch r, Dispatch d) {
        Block b = lastOr.getBlock();
        String blockName = b.getRealName();
        Integer seatSpan = b.getRows().get(lastOr.getRowNumber() - 1).getSeatCount() / 2;
        if (lastOr.getSeat() <= seatSpan) {
            outputStream.add("@");
            if (shift != 0.0) {
                Integer blockIndex = Integer.parseInt(blockName);
                Integer decreasedIndex = blockIndex - 1;
                outputStream.add("Exit Block: " + decreasedIndex);
            } else {
                outputStream.add("Exit Block: " + blockName);
                outputStream.add("Direction: right");
            }
        } else {
            outputStream.add("@");
            outputStream.add("Exit Block: " + blockName);
            if (shift == 0.0)
                outputStream.add("Direction: left");
        }
    }

    private boolean checkExclusiveCase(ArrayList<ArrayList<Order>> structure, Block firstB) {
        Integer uniqueID = Integer.parseInt(firstB.getName());
        if (uniqueID % 2 == 0)
            return false;
        String uniqueness = firstB.getName();
        int curHalf = 0;
        for (ArrayList<Order> ar : structure) {
            curHalf = ar.get(0).getBlock().getRows().get(ar.get(0).getRowNumber() - 1).getSeatCount() / 2;
            if (!ar.get(0).getBlock().getName().equals(uniqueness))
                return false;
            for (Order or : ar) {
                if (or.getSeat() <= curHalf)
                    return false;
            }
        }
        return true;
    }

    private void checkPotentialOdd(Order order, ArrayList<String> outputStream) {
        Block originalBlock = merge[1].getValue();
        Integer seatCount = originalBlock.getRows().get(order.getRowNumber()).getSeatCount();
        if (seatCount < order.getSeat()) {
            if (outputStream.get(outputStream.size() - 2).equals("@")) {
                outputStream.remove(outputStream.get(outputStream.size() - 1));
            }
            order.setBlock(merge[0].getValue());
            outputStream.add("Block: " + merge[0].getValue().getRealName());
            if (shift != 0.0) {
                order.setRealSeat(order.getSeat() - seatCount);
            } else {
                order.setRealSeat(order.getRealSeat() - seatCount);
            }
        }
    }

    public void deregisterDispatch(String key, Connection connection) {
        dispatchConnections.remove(connection);
        dispatchConnections.remove(key);
        System.out.println("Dispatch Point: " + key + " successfully de-registered.");
        System.out.println("Dispatch Point: " + key + " successfully removed.");
    }

    public String getMerge() {
        String rightMerge = null;
        try {
            rightMerge = merge[0].getKey();
        } catch (Exception e) {
        }
        return rightMerge;
    }

    public Pair<String, Block>[] sendMerge() {
        return merge;
    }

    public void setPair(Pair<String, Block> newP, int i) {
        merge[i] = newP;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public class TimerThread extends Thread {
        private Date curDate = null;
        private HashMap<Cluster, Dispatch> toRemove;

        @Override
        public void run() {
            toRemove = new HashMap<>();
            while (true) {
                try {
                    Thread.sleep(5000);
                    curDate = new Date();
                    for (Dispatch d : junction.getDispatches()) {
                        while (d.getJunction().getAwait() || d.getJunction().getProcessing()) {
                            Thread.sleep(100);
                        }
                        d.getJunction().setAwait(true);
                        clusterLoop:
                        for (int i = 0; i < d.clusters.size(); i++) {
                            Cluster c = d.clusters.get(i);
                            if (c.getOrders().size() == 0) {
                                toRemove.put(c, d);
                            }
                            orderLoop:
                            for (int j = 0; j < c.getOrders().size(); j++) {
                                Order o = c.get(j);
                                int difference = (int) Math.abs(o.getDate().getTime() - curDate.getTime()) / 1000;
                                if (difference >= time) {
                                    d.getJunction().setThreadQueue();
                                    d.getJunction().setAwait(true);
                                    d.getJunction().optimalPath(d, o);
                                    d.getJunction().setAwait(false);
                                    d.getJunction().removeFromQueue();
                                    break orderLoop;
                                }
                            }

                        }
                        d.getJunction().setAwait(false);
                    }
                    Dispatch curD;
                    for (Cluster remove : toRemove.keySet()) {
                        curD = toRemove.get(remove);
                        curD.clusters.remove(remove);
                    }
                    toRemove.clear();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //    public void Even_Test1() throws InterruptedException {
//        test=true;
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "3", "5", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "30", "28", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "17", "18", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "30", "21", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "30", "15", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "26", "26", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "30", "5", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "9", "18", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "8", "3", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "8", "14", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "17", "15", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "26", "8", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "3", "20", "no_setting", "David", "0", null, true);
//        Thread.sleep(30000);
//    }
//
//    public void Even_Test2() throws InterruptedException {
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "215", "24", "16", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "215", "16", "28", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "215", "24", "21", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "215", "30", "3", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "215", "16", "13", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "15", "16", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "15", "26", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "210", "9", "25", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "210", "9", "12", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "210", "17", "31", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "210", "17", "16", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "210", "9", "5", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "210", "23", "10", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "215", "24", "2", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "213", "19", "8", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "213", "25", "20", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "8", "3", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "11", "10", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "213", "19", "20", "no_setting", "David", "0", null, true);
//        Thread.sleep(30000);
//    }
//
//    public void presentationTester() throws InterruptedException{
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "3", "12", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "25", "16", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "25", "9", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "25", "30", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "16", "18", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "26", "26", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "16", "10", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "20", "15", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "20", "4", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "10", "3", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "214", "10", "8", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        test=false;
//    }
//
//    public void Even_Test3() throws InterruptedException {
//        triggerOrderInsert("2", "213", "32", "4", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("2", "213", "21", "16", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("2", "214", "15", "4", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("2", "213", "32", "26", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("2", "214", "15", "3", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("2", "213", "32", "16", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("2", "213", "18", "19", "no_setting", "David", "0", null, true);
//        Thread.sleep(30000);
//    }
//
//    public void Even_Test4() throws InterruptedException {
//        Thread.sleep(1000);
//        triggerOrderInsert("2", "214", "35", "32", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("2", "215", "20", "2", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("2", "215", "25", "5", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("2", "214", "35", "19", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("2", "212", "2", "1", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("2", "214", "32", "16", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("2", "215", "16", "2", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("2", "215", "25", "10", "no_setting", "David", "0", null, true);
//        test=false;
//        Thread.sleep(30000);
//    }
//
//    public void Odd_Test5() throws InterruptedException {
//        test=true;
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "235", "1", "1", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "235", "8", "8", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "235", "8", "15", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "235", "15", "22", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "235", "15", "30", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "235", "18", "34", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "236", "20", "1", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "236", "20", "8", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "236", "22", "15", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "236", "24", "23", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "236", "24", "30", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "236", "24", "34", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "236", "15", "18", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "200", "26", "1", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "200", "24", "1", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//    }
//
//    public void Odd_Test6() throws InterruptedException {
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "235", "1", "1", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "235", "8", "8", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "235", "8", "15", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "235", "15", "22", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "235", "15", "30", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "235", "18", "34", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "236", "20", "1", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "236", "20", "8", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "236", "22", "15", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "236", "24", "23", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "236", "24", "30", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "236", "24", "34", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "234", "1", "34", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "234", "2", "33", "no_setting", "David", "0", null, true);
//        Thread.sleep(30000);
//    }
//
//    public void Odd_Test7() throws InterruptedException {
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "235", "1", "1", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "235", "8", "8", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "235", "8", "15", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "235", "15", "22", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "236", "15", "30", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "236", "18", "34", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "200", "16", "10", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "234", "18", "30", "no_setting", "David", "0", null, true);
//        Thread.sleep(1000);
//        triggerOrderInsert("1", "236", "20", "1", "no_setting", "David", "0", null, true);
//        test=false;
//        Thread.sleep(30000);
//    }
}
