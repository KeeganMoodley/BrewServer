import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by s213463695 on 2016/03/22.
 */
public class Connection extends Thread {

    private Main server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Lock outLock = new ReentrantLock();
    public String username;
    private String password;
    private String email;

    private static final String SIGN_UP = "#SIGN_UP";
    private static final String LOG_IN = "#VERIFY_LOGIN_DATA";
    private static final String QUIT_COMMAND = "#QUIT";
    private static final String PRICE_COMMAND = "#GET_PRICE";
    private static final String ORDER_INSERT_COMMAND = "#INSERT_ORDER";
    private static final String ORDER_REMOVAL_COMMAND = "#REMOVE_ORDER";
    private static final String STORE_USER_PROFILE = "#STORE_PROFILE";
    private static final String GET_LOCATIONS = "#RETRIEVE_LOCATIONS";
    private static final String GET_UPDATE = "#RETRIEVE_UPDATE";
    private static final String CHANGE_PASSWORD = "#CHANGE_PASSWORD";
    private static final String DISPATCH_REGISTRATION = "#DISPATCH_REGISTRATION";
    private static final String FETCH_HISTORY = "#FETCH_HISTORY";
    private static final String DEREGISTER_DISPATCH = "#DEREGISTER";
    private static final String CHANGE_LOCATION = "#CHANGE_LOCATION";
//    private static final String TEST="#TEST_RUN";

    private static final String GET_STOCK = "#GET_STOCK";

    public Connection(Main server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new DataOutputStream(socket.getOutputStream());
            out.flush();
            in = new DataInputStream(socket.getInputStream());

            String command = "";
            while (!command.equals(QUIT_COMMAND)) {
                try {
                    command = in.readUTF();
                    switch (command) {
                        case SIGN_UP:
                            email = in.readUTF();
                            password = in.readUTF();
                            username = in.readUTF();
                            server.addUser(username, password, email, this);
                            break;
                        case LOG_IN:
                            email = in.readUTF();
                            password = in.readUTF();
                            server.checkUser(password, email, this);
                            break;
                        case PRICE_COMMAND:
                            server.returnPrice(this);
                            break;
                        case ORDER_INSERT_COMMAND:
                            String quantity = in.readUTF();
                            String block = in.readUTF();
                            String row = in.readUTF();
                            String seat = in.readUTF();
                            //String setting = in.readUTF();
                            String user1 = in.readUTF();
                            String androidIndex = String.valueOf(new Date());

                            //My code
                            double total = in.readDouble();
                            int size = in.readInt();
                            ArrayList<FoodUpdate> foodUpdates = new ArrayList<>();
                            for (int i = 0; i < size; i++) {
                                int id = in.readInt();
                                int q = in.readInt();
                                foodUpdates.add(new FoodUpdate(id, q));
                            }
                            boolean cash = in.readBoolean();
                            server.triggerOrderInsert(quantity, block, row, seat, user1, androidIndex, this, false, foodUpdates, total, cash);
                            break;
                        case CHANGE_LOCATION:
                            String block1 = in.readUTF();
                            String row1 = in.readUTF();
                            String seat1 = in.readUTF();
                            String user4 = in.readUTF();
                            server.changeLocation(block1, row1, seat1, user4, this);
                            break;
                        case ORDER_REMOVAL_COMMAND:
                            String time = in.readUTF();
                            String user3 = in.readUTF();
                            server.removeOrder(time, user3, this);
                            break;
                        case STORE_USER_PROFILE:
                            String tempUser = in.readUTF();
                            String tempEmail = in.readUTF();
                            String user2 = in.readUTF();
                            String email = in.readUTF();
                            String cell = in.readUTF();
                            String phone = in.readUTF();
                            server.storeUserProfile(tempUser, tempEmail, user2, email, cell, phone, this);
                            break;
                        case GET_LOCATIONS:
                            server.getLocations(this);
                            break;
                        case GET_UPDATE:
                            server.getUpdate(this);
                            break;
                        case QUIT_COMMAND:
                            String context = in.readUTF();
                            server.removeUser(username, context, this);
                            break;
                        case CHANGE_PASSWORD:
                            String password = in.readUTF();
                            String e_mail = in.readUTF();
                            String user = in.readUTF();
                            server.updatePassword(password, e_mail, user, this);
                            break;
                        case DISPATCH_REGISTRATION:
                            String dispatchName = in.readUTF();
                            server.registerDispatch(dispatchName, this);
                            break;
                        case FETCH_HISTORY:
                            String date = in.readUTF();
                            server.fetchHistory(date, this);
                            break;
                        case GET_STOCK:
                            System.out.println("Received #GET_STOCK command");
                            server.sendFood();
                            sendFood();
                            break;
                        case DEREGISTER_DISPATCH:
                            String key = in.readUTF();
                            server.deregisterDispatch(key, this);
                            out.writeUTF("#DEREGISTERED");
                            command = "#QUIT";
                            break;
//                        case TEST:
//                            server.Even_Test1();
//                            server.Even_Test2();
//                            server.presentationTester();
//                            //server.Even_Test3();
//                            //server.Even_Test4();
//                            if(server.getMerge()!=null){
//                                server.Odd_Test5();
//                                server.Odd_Test6();
//                                server.Odd_Test7();
//                            }
//                            break;

                        //Request food
                        default:
                            System.out.printf("Unknown command '%s' received from user with username '%s'/n", command, username);
                            break;
                    }
                } catch (IOException e) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null)
                    in.close();
                if (out != null)
                    out.close();
                socket.close();
                if (username != null)
                    System.out.println("Socket for user: " + username + " closed.");
                else
                    System.out.println("Socket closed.");
            } catch (Exception e) {
            }
        }
    }

    private void sendFood() {
        outLock.lock();
        //Solid food = (Solid) DB_Controller.foods.get(0);
        try {
            out.writeUTF(GET_STOCK);
            out.writeInt(DB_Controller.foods.size()); //number of food items to be sent
            for (Food food : DB_Controller.foods) {
                int id = food.getId();
                int type = food.getType(); //1 is solid, 2 is liquid, 3 is packaged
                byte[] pic = food.getImage();

                double price = food.getPrice();
                String title = food.getTitle();
                String nutrition = food.getNutrition();
                String dietary = food.getDietary();
                boolean halaal = food.isHalaal();
                int quantityAvailable = food.getQuantityAvailable();
                System.out.println(id);
                /*String sql1 = "SELECT * FROM [SOLID] WHERE stock_ID ='" + id + "'";
                ResultSet rSet = stat.executeQuery(sql1);
                double length = rSet.getFloat("length");
                double width = rSet.getFloat("width");
                double height = rSet.getFloat("height");*/
                double length = food.getLength();
                double width = food.getWidth();
                double height = food.getHeight();
                double volume = food.getVolume();

                out.writeInt(id);
                out.writeInt(type);


                //out.write(pic);
                out.writeInt(pic.length); //Send byte[] length
                out.write(pic, 0, pic.length);


                out.writeDouble(price);
                out.writeUTF(title);
                out.writeUTF(nutrition);
                out.writeUTF(dietary);
                out.writeBoolean(halaal);
                out.writeInt(quantityAvailable);
                out.writeDouble(length);
                out.writeDouble(width);
                out.writeDouble(height);
                out.writeDouble(volume);
            }
            out.flush();
            System.out.println("Food is sent to phone");
            DB_Controller.foods.clear();
            //objectOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            outLock.unlock();
        }
    }

    public void notifyVerification(boolean choice, String username, Boolean event) {
        outLock.lock();
        try {
            if (choice) {
                out.writeUTF("#SUCCESSFUL_VERIFICATION");
                out.writeUTF(username);
                if (event)
                    out.writeUTF("true");
                else
                    out.writeUTF("false");
            } else {
                out.writeUTF("#UNSUCCESSFUL_VERIFICATION");
                if (username.equals("#dbFail#"))
                    out.writeUTF("#dbFail#");
                else
                    out.writeUTF("verification");
            }
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            outLock.unlock();
        }
    }

    public void notifySaving(boolean choice, String indicator, Boolean event) {
        outLock.lock();
        try {
            if (choice) {
                out.writeUTF("#SUCCESSFUL_SAVE");
                if (event)
                    out.writeUTF("true");
                else
                    out.writeUTF("false");
            } else {
                out.writeUTF("#UNSUCCESSFUL_SAVE");
                if (indicator.equals("duplicateE")) {
                    out.writeUTF("duplicateE");
                } else if (indicator.equals("duplicateU")) {
                    out.writeUTF("duplicateU");
                } else if (indicator.equals("duplicateEU")) {
                    out.writeUTF("duplicateEU");
                } else {
                    out.writeUTF("database");
                }
            }
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            outLock.unlock();
        }
    }

    public void notifyLogout(boolean choice, String context) {
        outLock.lock();
        try {
            if (choice) {
                out.writeUTF("#SUCCESSFUL_LOGOUT");
                out.writeUTF(context);
            } else {
                out.writeUTF("#UNSUCCESSFUL_LOGOUT");
                out.writeUTF(context);
            }
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            outLock.unlock();
        }
    }

    public void returnPrice(Double price) {
        outLock.lock();
        try {
            out.writeUTF("#PRICE_RETURNED");
            out.writeUTF(price.toString());
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            outLock.unlock();
        }
    }

    public void notifyOrderInsertion(String time, String date, String quantity, Double price, String androidIndex) {
        outLock.lock();
        try {
            String priceString = String.valueOf(price);
            out.writeUTF("#ORDER_INSERTED");
            out.writeUTF(time);
            out.writeUTF(date);
            out.writeUTF(quantity);
            out.writeUTF(priceString);
            out.writeUTF(androidIndex);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            outLock.unlock();
        }
    }

    public void notifyOrderRemoval(boolean b, String time) {
        outLock.lock();
        try {
            if (b) {
                out.writeUTF("#ORDER_REMOVED");
                out.writeUTF(time);
            } else {
                out.writeUTF("#ORDER_REMOVAL_FAILURE");
            }
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            outLock.unlock();
        }
    }

    public void notifyUpdating(boolean choice, String indicator, String tempUser, String tempEmail) {
        outLock.lock();
        try {
            if (choice) {
                out.writeUTF("#SUCCESSFUL_UPDATE");
            } else {
                out.writeUTF("#UNSUCCESSFUL_UPDATE");
                if (indicator.equals("duplicateE")) {
                    out.writeUTF("duplicateE");
                    out.writeUTF(tempUser);
                    out.writeUTF(tempEmail);
                } else if (indicator.equals("duplicateU")) {
                    out.writeUTF("duplicateU");
                    out.writeUTF(tempUser);
                    out.writeUTF(tempEmail);
                } else if (indicator.equals("duplicateEU")) {
                    out.writeUTF("duplicateEU");
                    out.writeUTF(tempUser);
                    out.writeUTF(tempEmail);
                } else {
                    out.writeUTF("database");
                    out.writeUTF(tempUser);
                    out.writeUTF(tempEmail);
                }
            }
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            outLock.unlock();
        }
    }

    public void sendLocations(ArrayList<Block> blocks, boolean b) {
        outLock.lock();
        try {
            Collections.sort(blocks, new DB_Controller.blockNameComparator());
            if (b) {
                boolean merge = false;
                if (server.getMerge() != null) {
                    merge = true;
                }
                out.writeUTF("#SUCCESSFUL_LOCATION_RETRIEVAL");
                if (merge)
                    out.writeUTF(String.valueOf(blocks.size() + 1));
                else
                    out.writeUTF(String.valueOf(blocks.size()));
                Block curB = null;
                for (int x = 0; x <= blocks.size() - 1; x++) {
                    if (merge && x == blocks.size() - 1) {
                        curB = server.sendMerge()[1].getValue();
                        out.writeUTF(curB.getRealName());
                        ArrayList<Block.Row> rows = curB.getRows();
                        out.writeUTF(String.valueOf(rows.size()));
                        for (int y = 0; y <= rows.size() - 1; y++) {
                            out.writeUTF(rows.get(y).getNumber().toString());
                            out.writeUTF(rows.get(y).getSeatCount().toString());
                        }
                        curB = server.sendMerge()[0].getValue();
                    } else {
                        curB = blocks.get(x);
                    }
                    out.writeUTF(curB.getRealName());
                    ArrayList<Block.Row> rows = curB.getRows();
                    out.writeUTF(String.valueOf(rows.size()));
                    for (int y = 0; y <= rows.size() - 1; y++) {
                        out.writeUTF(rows.get(y).getNumber().toString());
                        out.writeUTF(rows.get(y).getSeatCount().toString());
                    }
                }
                out.flush();
            } else {
                out.writeUTF("#UNSUCCESSFUL_LOCATION_RETRIEVAL");
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            outLock.unlock();
        }
    }

    public void sendUpdate(String date) {
        outLock.lock();
        try {
            out.writeUTF("#UPDATE");
            out.writeUTF(date);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            outLock.unlock();
        }
    }

    public void notifyLocationUpdate() {
        outLock.lock();
        try {
            out.writeUTF("#SUCCESSFUL_LOCATION_UPDATE");
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            outLock.unlock();
        }
    }

    public void notifyPasswordUpdate(boolean b, String password) {
        outLock.lock();
        try {
            if (b) {
                out.writeUTF("#SUCCESSFUL_PASSWORD_UPDATE");
                out.writeUTF(password);
            } else {
                out.writeUTF("#UNSUCCESSFUL_PASSWORD_UPDATE");
            }
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            outLock.unlock();
        }
    }

    public void triggerPrintOut(ArrayList<ArrayList<Order>> structure, ArrayList<String> outputStream) {
        outLock.lock();
        try {
            out.writeUTF("#PRINT_OUT");
            out.writeUTF(String.valueOf(outputStream.size()));
            for (String s : outputStream) {
                out.writeUTF(s);
            }
            out.flush();
        } catch (Exception e) {
        } finally {
            outLock.unlock();
        }
    }

    public void addToDesktop(Integer orderID, String block, String row, String seat, String user, String quantity, Double total, String time) {
        outLock.lock();
        try {
            out.writeUTF("#ADD_TO_DESKTOP");
            out.writeUTF(String.valueOf(orderID));
            out.writeUTF(block);
            out.writeUTF(row);
            out.writeUTF(seat);
            out.writeUTF(user);
            out.writeUTF(quantity);
            out.writeUTF(String.valueOf(total));
            out.writeUTF(time);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            outLock.unlock();
        }
    }

    public void removeFromDesktop(Integer orderID, String username) {
        outLock.lock();
        try {
            out.writeUTF("#REMOVE_FROM_DESKTOP");
            out.writeUTF(String.valueOf(orderID));
            out.writeUTF(username);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            outLock.unlock();
        }
    }

    public void sendHistory(String history) {
        outLock.lock();
        try {
            out.writeUTF("#HISTORY_STRING");
            out.writeUTF(history);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            outLock.unlock();
        }
    }

    public void notifyOrderDispatch(Order or) {
        outLock.lock();
        try {
            out.writeUTF("#ORDER_DISPATCHED");
            out.writeUTF(or.getAndroidIndex());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            outLock.unlock();
        }
    }

    public void notifyDispatchDown() {
        outLock.lock();
        try {
            out.writeUTF("#DISPATCH_DOWN");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            outLock.unlock();
        }
    }
}
