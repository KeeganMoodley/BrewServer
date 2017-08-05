import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by s213463695 on 2016/07/23.
 */
public class Dispatch_Junction {
    private ArrayList<Dispatch> dispatches;
    private ReentrantLock lock;
    private Condition con;
    private Main main = null;
    private Boolean await = false;
    private Boolean processing = false;
    private Integer threadQueue = 0;

    public Dispatch_Junction(ArrayList<Dispatch> dispatches, Main main) {
        this.dispatches = dispatches;
        this.lock = new ReentrantLock();
        this.con = lock.newCondition();
        this.main = main;
        setJunction();
    }

    private void setJunction() {
        for (Dispatch d : dispatches)
            d.setJunction(this);
    }

    public void setThreadQueue() {
        this.threadQueue++;
    }

    public void removeFromQueue() {
        this.threadQueue--;
    }

    public Boolean getAwait() {
        return await;
    }

    public void setAwait(boolean await) {
        this.await = await;
    }

    public Boolean getProcessing() {
        return processing;
    }

    public String insertOrder(Order o) {
        while (await || threadQueue > 0 || processing) {
        }
        await = true;
        lock.lock();
        try {
            for (int x = 0; x <= dispatches.size() - 1; x++) {
                Dispatch current = dispatches.get(x);
                if (current.getRightBlock().getName() == o.getBlock().getName()) {
                    Dispatch previous = null;
                    if (x == 0) {
                        previous = dispatches.get(dispatches.size() - 1);
                    } else {
                        previous = dispatches.get(x - 1);
                    }
                    Boolean executed = current.addOrder(o, false, "left");
                    if (!executed) {
                        previous.addOrder(o, true, "right");
                    }
                    String concat = "";
                    concat = concat.concat(current.getDispatchName()).concat(",").concat(previous.getDispatchName());
                    await = false;
                    return concat;
                } else if (current.getCentreBlock().getName() == o.getBlock().getName()) {
                    current.addOrder(o, false, "centre");
                    await = false;
                    return current.getDispatchName();
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            lock.unlock();
            await = false;
        }
        return "NoDispatch";
    }

    public void removeOrder(String username, Integer orderID, String blockName) {
        while (await || processing) {
        }
        await = true;
        lock.lock();
        try {
            Order o = null;
            search:
            for (Dispatch d : dispatches) {
                if (d.getCentreBlock().getName().equals(blockName) || d.getRightBlock().getName().equals(blockName)) {
                    o = d.getOrder(username, orderID);
                    break search;
                }
            }
            removal:
            for (int x = 0; x <= dispatches.size() - 1; x++) {
                Dispatch current = dispatches.get(x);
                if (current.getRightBlock().getName().equals(o.getBlock().getName())) {
                    Dispatch previous = null;
                    if (x == 0) {
                        previous = dispatches.get(dispatches.size() - 1);
                    } else {
                        previous = dispatches.get(x - 1);
                    }
                    current.removeOrder(o);
                    previous.removeOrder(o);
                    break removal;
                } else if (current.getCentreBlock().getName().equals(o.getBlock().getName())) {
                    current.removeOrder(o);
                    break removal;
                }
            }
        } catch (Exception e) {
        } finally {
            lock.unlock();
            await = false;
        }
    }

    public void optimalPath(Dispatch dispatch, Order order) {
        processing = true;
        Boolean centreOnly = true;
        int index = -1;
        try {
            Integer position = -1;
            search:
            for (int i = 0; i < dispatches.size(); i++) {
                Dispatch d = dispatches.get(i);
                if (d.getDispatchName().equals(dispatch.getDispatchName())) {
                    position = i;
                    break search;
                }
            }
            if (position != -1) {
                Dispatch sharedLeft = null;
                Dispatch sharedRight = null;
                if (position == (dispatches.size() - 1)) {
                    sharedLeft = dispatches.get(0);
                } else {
                    sharedLeft = dispatches.get(position + 1);
                }
                if (position != 0) {
                    sharedRight = dispatches.get(position - 1);
                } else {
                    sharedRight = dispatches.get(dispatches.size() - 1);
                }
                try {
                    index = dispatch.returnIndex(order);
                } catch (Exception e) {
                }
                if (index != -1) {
                    removeLinks(index, dispatch);
                    for (Order removeO : dispatch.clusters.get(index).getOrders()) {
                        if (removeO.getBlock().getName().equals(sharedRight.getLeftBlock().getName())) {
                            removeFromClusters(sharedRight, removeO, order);
                            centreOnly = false;
                        } else if (removeO.getBlock().getName().equals(sharedLeft.getRightBlock().getName())) {
                            removeFromClusters(sharedLeft, removeO, order);
                            centreOnly = false;
                        }
                    }
                    Integer optimalIndex;
                    if (centreOnly) {
                        Double leftDistance = dispatch.clusters.get(index).get(0).getBlock().getLeftEntryDistance();
                        Double rightDistance = dispatch.clusters.get(index).get(0).getBlock().getRightEntryDistance();
                        optimalIndex = dispatch.determineSimplePath(dispatch.clusters.get(index).getOrders(), null, true, "na", leftDistance, rightDistance, -1, "na", false, false);
                        if (optimalIndex != -1) {
                            main.sendOptimalPath(dispatch.getSimpleStructure(optimalIndex), optimalIndex, centreOnly, dispatch.clusters.get(index), dispatch, sharedLeft, sharedRight, dispatch.getPathsDistance(optimalIndex));
                        }
                    } else {
                        optimalIndex = dispatch.determineComplexPath(dispatch.clusters.get(index).getOrders(), sharedLeft, sharedRight);
                        if (optimalIndex != -1) {
                            main.sendOptimalPath(dispatch.getComplexStructure(optimalIndex), optimalIndex, centreOnly, dispatch.clusters.get(index), dispatch, sharedLeft, sharedRight, dispatch.getComplexFinalDistance(optimalIndex));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            if (index != -1) {
                if (centreOnly) {
                    dispatch.clearSimpleStructures();
                    dispatch.clearPaths();
                } else {
                    dispatch.clearComplexStructure();
                    dispatch.clearFinalComplexDistances();
                }
                Cluster remove = dispatch.clusters.get(index);
                dispatch.clusters.remove(remove);
            }
            await = false;
            processing = false;
        }
    }

    private void removeLinks(int index, Dispatch dispatch) {
        for (Order removeLink : dispatch.clusters.get(index).getOrders()) {
            for (Order or : removeLink.getLinks()) {
                try {
                    or.getLinks().remove(removeLink);
                } catch (Exception e) {
                }
            }
        }
    }

    private void removeFromClusters(Dispatch shared, Order removeO, Order order) {
        for (int x = 0; x < shared.clusters.size(); x++) {
            ArrayList<Order> cluster = shared.clusters.get(x).getOrders();
            ArrayList<Integer> indices = new ArrayList<>();
            try {
                for (int i = 0; i < cluster.size(); i++) {
                    Order or = cluster.get(i);
                    if (or == removeO) {
                        indices.add(i);
                    }
                }
                Collections.sort(indices, new ListSort());
                for (Integer j : indices) {
                    Order remove = cluster.get(j);
                    shared.clusters.get(x).getOrders().remove(remove);
                }
                if (cluster.size() == 0)
                    shared.clusters.remove(shared.clusters.get(x).getOrders());
            } catch (Exception e) {
            }
        }
    }

    public ArrayList<Dispatch> getDispatches() {
        return dispatches;
    }

    public class ListSort implements Comparator<Integer> {
        @Override
        public int compare(Integer o1, Integer o2) {
            if (o2 == o1)
                return 0;
            else if (o2 < o1)
                return -1;
            else
                return 1;

        }
    }
}
