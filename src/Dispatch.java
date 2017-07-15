import java.util.*;

/**
 * Created by s213463695 on 2016/07/24.
 */
public class Dispatch {

    private Integer dispatchID;
    private Double totalDistance;
    private String dispatchName;
    private Block rightBlock;
    private Block centreBlock;
    private Block leftBlock;
    private Boolean empty = false;
    private ArrayList<Double> paths = new ArrayList<>();
    private ArrayList<Integer> removedClusters = new ArrayList<>();
    protected ArrayList<Cluster> clusters;
    protected Map<Integer, ArrayList<ArrayList<Order>>> simpleStructures;
    protected Map<Integer, ArrayList<ArrayList<Order>>> complexStructure;
    private ArrayList<Double> complexFinalDistances;
    private final Double clusterDistance = 15.0;
    private Dispatch_Junction junction = null;
    private Integer clusterCapacity;

    public Dispatch(Integer dispatchID, String dispatchName, Block rightBlock, Block centreBlock, Block leftBlock, ArrayList<Cluster> clusters, Integer clusterC) {
        this.dispatchID = dispatchID;
        this.dispatchName = dispatchName;
        this.rightBlock = rightBlock;
        this.centreBlock = centreBlock;
        this.leftBlock = leftBlock;
        this.clusters = clusters;
        this.clusterCapacity = clusterC;
    }

    public void setJunction(Dispatch_Junction junction) {
        this.junction = junction;
    }

    //Clustering Functions
    public Boolean addOrder(Order o, Boolean shared, String dispatch) throws InterruptedException {
        Boolean broken = false;
        if (clusters.size() == 0) {
            buildCluster(o);
        } else {
            Integer matchRecord = -1;
            broken = false;
            Double horizontal;
            Double vertical;
            Integer oName;
            Integer orName;
            Boolean set = false;
            outer:
            for (int i = 0; i < clusters.size(); i++) {
                inner:
                for (Order or : clusters.get(i).getOrders()) {
                    if (removedClusters.contains(i))
                        break inner;
                    if (o.getBlock().getName().equals(or.getBlock().getName())) {
                        horizontal = getHorizontalSameBlock(o, or);
                    } else {
                        oName = Integer.valueOf(o.getBlock().getName());
                        orName = Integer.valueOf(or.getBlock().getName());
                        if (oName - 2 == orName) {
                            horizontal = getHorizontalDifferentBlock(o, or, 2);
                        } else if (orName - 2 == oName) {
                            horizontal = getHorizontalDifferentBlock(or, o, 2);
                        } else if (oName - 1 == orName) {
                            horizontal = getHorizontalDifferentBlock(o, or, 1);
                        } else {
                            horizontal = getHorizontalDifferentBlock(or, o, 1);
                        }
                    }
                    vertical = getVerticalSameBlock(o, or);
                    Double distance = Math.sqrt(Math.pow(horizontal, 2.0) + Math.pow(vertical, 2.0));
                    if (distance <= clusterDistance) {
                        o.addLink(or);
                        or.addLink(o);
                        if (matchRecord == -1 && !junction.getProcessing()) {
                            matchRecord = i;
                            if (clusters.get(i).getQuantity() >= clusterCapacity) {
                                deleteClusters();
                                clusters.get(i).update(o);
                                junction.optimalPath(this, o);
                                broken = true;
                                break outer;
                            }
                        } else if (matchRecord >= 0 && matchRecord != i && !junction.getProcessing()) {
                            Integer k;
                            int time1 = (int) clusters.get(i).get(0).getDate().getTime();
                            int time2 = (int) clusters.get(matchRecord).get(0).getDate().getTime();
                            if (time1 <= time2) {
                                ArrayList<Order> orders = clusters.get(matchRecord).getOrders();
                                for (int j = 0; j < clusters.get(matchRecord).getOrders().size(); j++) {
                                    clusters.get(i).update(orders.get(j));
                                }
                                removedClusters.add(matchRecord);
                                k = i;
                            } else {
                                ArrayList<Order> orders = clusters.get(i).getOrders();
                                for (int j = 0; j < clusters.get(i).getOrders().size(); j++) {
                                    clusters.get(matchRecord).update(orders.get(j));
                                }
                                removedClusters.add(i);
                                k = matchRecord;
                            }
                            if (clusters.get(i).getQuantity() >= clusterCapacity || clusters.get(matchRecord).getQuantity() >= clusterCapacity) {
                                clusters.get(k).update(o);
                                deleteClusters();
                                junction.optimalPath(this, clusters.get(k).get(0));
                                broken = true;
                                break outer;
                            } else if (k == i) {
                                matchRecord = i;
                            }
                            break inner;
                        }
                    }
                }
            }
            if (!broken && matchRecord != -1) {
                clusters.get(matchRecord).update(o);
            }
            if (!broken && set) {
                Collections.sort(clusters.get(matchRecord).getOrders(), new BlockNameComparator());
            }
            if (matchRecord == -1) {
                buildCluster(o);
            }
            if (!broken)
                deleteClusters();
        }
        return broken;
    }

    private void deleteClusters() {
        for (Integer j : removedClusters) {
            Cluster remove = clusters.get(j);
            clusters.remove(remove);
        }
        removedClusters.clear();
    }

    public void removeOrder(Order o) {
        Integer clusterIndex = -1;
        loop:
        for (int x = 0; x < clusters.size(); x++) {
            Cluster c = clusters.get(x);
            if (c.getOrders().contains(o)) {
                clusterIndex = x;
                break loop;
            }
        }
        if (clusterIndex != -1) {
            clusters.get(clusterIndex).remove(o);
            if (clusters.get(clusterIndex).size() == 0) {
                clusters.remove(clusters.get(clusterIndex));
            }
        }
    }

    private Double getVerticalSameBlock(Order o, Order or) {
        Double rowCount = Double.valueOf(o.getBlock().getRows().size());
        Double fractionO = o.getRowNumber() / rowCount;
        Double fractionOr = or.getRowNumber() / rowCount;
        if (o.getRowNumber() < or.getRowNumber())
            return ((fractionOr - fractionO) * o.getBlock().getPathLength());
        else if (o.getRowNumber() > or.getRowNumber())
            return ((fractionO - fractionOr) * o.getBlock().getPathLength());
        else
            return 0.0;
    }

    private Double getHorizontalDifferentBlock(Order fi, Order se, int multiplier) {
        Integer seatCountFirst = se.getBlock().getRows().get(se.getRowNumber() - 1).getSeatCount();
        Double l1 = (seatCountFirst - se.getSeat()) * se.getBlock().getSeatWidth();
        Double l2 = fi.getSeat() * fi.getBlock().getSeatWidth();
        Double l3 = centreBlock.getRows().get(se.getRowNumber() - 1).getSeatCount() * centreBlock.getSeatWidth();
        Double l4 = se.getBlock().getPathWidth();
        if (multiplier == 2)
            return l1 + l2 + l3 + l4 * 2.0;
        else
            return l4 + l1 + l2;
    }

    private Double getHorizontalSameBlock(Order o, Order or) {
        if (o.getSeat() < or.getSeat())
            return Double.valueOf(or.getSeat() - o.getSeat()) * o.getBlock().getSeatWidth();
        else if (o.getSeat() > or.getSeat())
            return Double.valueOf(o.getSeat() - or.getSeat()) * or.getBlock().getSeatWidth();
        else
            return 0.0;
    }

    private void buildCluster(Order o) {
        Cluster newC = new Cluster(new ArrayList<>(), o.getQuantity(), o.getTotal());
        o.setLinks(new ArrayList<Order>());
        newC.add(o);
        clusters.add(newC);
    }

    //Optimal Path Functions
    protected Integer determineComplexPath(ArrayList<Order> orders, Dispatch sharedLeft, Dispatch sharedRight) {
        ArrayList<Order> farLeftOrders = new ArrayList<>();
        ArrayList<Order> farRightOrders = new ArrayList<>();
        ArrayList<Order> centre = new ArrayList<>();

        populateSplitStructures(orders, farLeftOrders, farRightOrders, centre);
        complexFinalDistances = new ArrayList<>();
        complexStructure = new HashMap<>();

        Double rightDistance = sharedRight.centreBlock.getLeftEntryDistance();
        Double leftDistance = sharedLeft.centreBlock.getRightEntryDistance();
        Integer leftEntry = sharedLeft.centreBlock.getRightEntryRow();
        Integer rightEntry = sharedRight.centreBlock.getLeftEntryRow();

        Integer minPosition = -1;
        calculateOuter(farLeftOrders, farRightOrders, centre, "rightToLeft", 0, leftDistance, rightDistance, rightEntry, "right");
        if (empty) {
            minPosition = initializeMinimum();
            return minPosition;
        }
        calculateInner(farRightOrders, "leftToRight", "rightToLeft", centre, farLeftOrders, 1, leftDistance, rightDistance, "right");
        if (empty) {
            minPosition = initializeMinimum();
            return minPosition;
        }
        calculateInner(farLeftOrders, "rightToLeft", "leftToRight", centre, farRightOrders, 2, leftDistance, rightDistance, "left");
        if (empty) {
            minPosition = initializeMinimum();
            return minPosition;
        }
        calculateOuter(farRightOrders, farLeftOrders, centre, "leftToRight", 3, leftDistance, rightDistance, leftEntry, "left");

        minPosition = initializeMinimum();
        return minPosition;
    }

    private Integer initializeMinimum() {
        empty = false;
        Integer minPosition = determineMinimum(complexFinalDistances);
        return minPosition;
    }

    private void calculateInner(ArrayList<Order> single, String directionSingle, String directionDouble, ArrayList<Order> doubleInner, ArrayList<Order> doubleOuter, Integer structureIndex, Double finalLeft, Double finalRight, String startBlock) {
        totalDistance = 0.0;
        Double leftDistance = centreBlock.getLeftEntryDistance();
        Double rightDistance = centreBlock.getRightEntryDistance();
        Integer entry = centreBlock.getRightEntryRow();

        Integer startOptimum = determineSimplePath(single, null, false, directionSingle, leftDistance, rightDistance, entry, startBlock, true, true);
        ArrayList<ArrayList<Order>> innerPath;
        if (startOptimum != -1) {
            innerPath = getSimpleStructure(startOptimum);
            totalDistance += paths.get(startOptimum);
            paths.clear();
        } else {
            innerPath = new ArrayList<>();
        }
        Integer rowExit = -1;
        if (innerPath.size() == 0) {
            rowExit = entry;
        } else {
            rowExit = innerPath.get(innerPath.size() - 1).get(innerPath.get(innerPath.size() - 1).size() - 1).getRowNumber();
        }
        clearSimpleStructures();

        Integer centreOptimum = determineSimplePath(doubleInner, doubleOuter, false, directionDouble, 0.0, 0.0, rowExit, "centre", false, true);
        if (empty) {
            finalize(innerPath, structureIndex);
            return;
        }

        for (ArrayList<Order> row : getSimpleStructure(centreOptimum)) {
            innerPath.add(row);
        }
        if (innerPath.size() == 0) {
            rowExit = entry;
        } else {
            rowExit = innerPath.get(innerPath.size() - 1).get(innerPath.get(innerPath.size() - 1).size() - 1).getRowNumber();
        }
        totalDistance += paths.get(centreOptimum);
        clearSimpleStructures();
        paths.clear();
        Integer endOptimum;
        leftDistance = finalLeft;
        rightDistance = finalRight;

        if (leftDistance == 999.0)
            leftDistance = -1.0;
        else if (rightDistance == 999.0)
            rightDistance = -1.0;
        endOptimum = determineFinalPath(startBlock, doubleOuter, directionDouble, leftDistance, rightDistance, rowExit);

        addToGlobalPathDistance(endOptimum, innerPath);
        clearSimpleStructures();
        finalize(innerPath, structureIndex);
    }

    private void calculateOuter(ArrayList<Order> end, ArrayList<Order> start, ArrayList<Order> centre, String direction, Integer structureIndex, Double leftDistance, Double rightDistance, Integer entry, String startBlock) {

        totalDistance = 0.0;
        Integer startOptimum = determineSimplePath(start, null, false, direction, leftDistance, rightDistance, entry, startBlock, false, true);
        ArrayList<ArrayList<Order>> outerPath = getSimpleStructure(startOptimum);
        Integer rowExit = -1;
        if (outerPath.size() == 0) {
            rowExit = entry;
        } else {
            rowExit = outerPath.get(outerPath.size() - 1).get(outerPath.get(outerPath.size() - 1).size() - 1).getRowNumber();
        }
        totalDistance += paths.get(startOptimum);
        clearSimpleStructures();
        paths.clear();

        Integer centreOptimum = determineSimplePath(centre, end, false, direction, 0.0, 0.0, rowExit, "centre", false, true);
        if (empty) {
            finalize(outerPath, structureIndex);
            return;
        }
        for (ArrayList<Order> row : getSimpleStructure(centreOptimum)) {
            outerPath.add(row);
        }
        if (outerPath.size() == 0) {
            rowExit = entry;
        } else {
            rowExit = outerPath.get(outerPath.size() - 1).get(outerPath.get(outerPath.size() - 1).size() - 1).getRowNumber();
        }
        totalDistance += paths.get(centreOptimum);
        clearSimpleStructures();
        paths.clear();

        Integer endOptimum;
        if (leftDistance == 999.0)
            leftDistance = -1.0;
        else if (rightDistance == 999.0)
            rightDistance = -1.0;
        endOptimum = determineFinalPath(startBlock, end, direction, leftDistance, rightDistance, rowExit);

        addToGlobalPathDistance(endOptimum, outerPath);
        clearSimpleStructures();
        finalize(outerPath, structureIndex);
    }

    private void finalize(ArrayList<ArrayList<Order>> path, Integer structureIndex) {
        clearSimpleStructures();
        paths.clear();
        Double distance = totalDistance;
        ArrayList<ArrayList<Order>> copiedPath = (ArrayList<ArrayList<Order>>) path.clone();
        totalDistance = 0.0;
        path.clear();
        complexFinalDistances.add(distance);
        complexStructure.put(structureIndex, copiedPath);
    }

    private void addToGlobalPathDistance(Integer endOptimum, ArrayList<ArrayList<Order>> path) {
        if (endOptimum != -1) {
            for (ArrayList<Order> row : getSimpleStructure(endOptimum)) {
                path.add(row);
            }
            totalDistance += paths.get(endOptimum);
            paths.clear();
        }
    }

    private Integer determineFinalPath(String startBlock, ArrayList<Order> doubleOuter, String directionDouble, Double leftDistance, Double rightDistance, Integer rowExit) {
        if (startBlock.equals("right")) {
            if (doubleOuter.size() != 0)
                rightDistance = doubleOuter.get(0).getBlock().getRightEntryDistance();
            return determineSimplePath(doubleOuter, null, true, directionDouble, leftDistance, rightDistance, rowExit, "left", false, true);
        } else {
            if (doubleOuter.size() != 0)
                leftDistance = doubleOuter.get(0).getBlock().getLeftEntryDistance();
            return determineSimplePath(doubleOuter, null, true, directionDouble, leftDistance, rightDistance, rowExit, "right", false, true);
        }
    }

    private void populateSplitStructures(ArrayList<Order> orders, ArrayList<Order> farLeftOrders, ArrayList<Order> farRightOrders, ArrayList<Order> centre) {
        for (Order or : orders) {
            if (or.getBlock().equals(leftBlock))
                farLeftOrders.add(or);
        }
        for (Order or : orders) {
            if (or.getBlock().equals(centreBlock))
                centre.add(or);
        }
        for (Order or : orders) {
            if (or.getBlock().equals(rightBlock))
                farRightOrders.add(or);
        }
    }

    protected Integer determineSimplePath(ArrayList<Order> orders, ArrayList<Order> end, Boolean complexEnd, String complexDirection, Double leftEntryDistance, Double rightEntryDistance, Integer complexEntry, String startBlock, Boolean innerStart, Boolean complex) {
        populate_simpleStructures();

        if (orders.size() != 0) {
            Order initiate = orders.get(0);
            Block block = initiate.getBlock();
            Double leftPathUp = leftEntryDistance;
            Double rightPathUp = rightEntryDistance;
            Double leftPathDown = leftEntryDistance;
            Double rightPathDown = rightEntryDistance;

            Integer leftEntry = -1;
            Integer rightEntry = -1;
            if (!complex) {
                leftEntry = block.getLeftEntryRow();
                rightEntry = block.getRightEntryRow();
            }
            TreeMap<Integer, Order> uniqueRowsAsc;
            TreeMap<Integer, Order> uniqueRowsDsc;

            if (!complexDirection.equals("rightToLeft")) {
                if (complex) {
                    leftEntry = complexEntry;
                }
                RowComparator sortAsc = new RowComparator(true);
                Collections.sort(orders, sortAsc);
                uniqueRowsAsc = getCount(false, orders);
                for (Order o : orders) {
                    uniqueRowsAsc.put(o.getRowNumber(), o);
                }
                leftPathDown = sumUpSimplePath(leftPathDown, block, initiate, uniqueRowsAsc, orders, 0, leftEntry, leftEntryDistance, rightEntryDistance, true, complexEnd, innerStart);
                paths.add(leftPathDown);
                uniqueRowsAsc.clear();

                RowComparator sortDsc = new RowComparator(false);
                Collections.sort(orders, sortDsc);
                uniqueRowsDsc = getCount(true, orders);
                for (Order o : orders) {
                    uniqueRowsDsc.put(o.getRowNumber(), o);
                }
                leftPathUp = sumUpSimplePath(leftPathUp, block, initiate, uniqueRowsDsc, orders, 1, leftEntry, leftEntryDistance, rightEntryDistance, true, complexEnd, innerStart);
                paths.add(leftPathUp);
                uniqueRowsDsc.clear();
                if (complex) {
                    paths.add(999999999.0);
                    paths.add(999999999.0);
                }
            }

            if (!complexDirection.equals("leftToRight")) {
                if (complex) {
                    paths.add(999999999.0);
                    paths.add(999999999.0);
                }
                if (complex) {
                    rightEntry = complexEntry;
                }
                RowComparator sortAsc = new RowComparator(true);
                Collections.sort(orders, sortAsc);
                uniqueRowsAsc = getCount(false, orders);
                for (Order o : orders) {
                    uniqueRowsAsc.put(o.getRowNumber(), o);
                }
                rightPathDown = sumUpSimplePath(rightPathDown, block, initiate, uniqueRowsAsc, orders, 2, rightEntry, leftEntryDistance, rightEntryDistance, false, complexEnd, innerStart);
                paths.add(rightPathDown);
                uniqueRowsAsc.clear();

                RowComparator sortDsc = new RowComparator(false);
                Collections.sort(orders, sortDsc);
                uniqueRowsDsc = getCount(true, orders);
                for (Order o : orders) {
                    uniqueRowsDsc.put(o.getRowNumber(), o);
                }
                rightPathUp = sumUpSimplePath(rightPathUp, block, initiate, uniqueRowsDsc, orders, 3, rightEntry, leftEntryDistance, rightEntryDistance, false, complexEnd, innerStart);
                paths.add(rightPathUp);
                uniqueRowsDsc.clear();
            }
        } else {
            if (startBlock.equals("right")) {
                if (rightEntryDistance != -1) {
                    if (!innerStart)
                        paths.add(rightBlock.getRows().get(complexEntry).getSeatCount() * rightBlock.getSeatWidth());
                    totalDistance += rightEntryDistance;
                } else {
                    totalDistance += leftEntryDistance;
                }
            } else if (startBlock.equals("centre")) {
                if (end != null && end.size() == 0) {
                    empty = true;
                    if (complexDirection.equals("leftToRight")) {
                        totalDistance += leftEntryDistance;
                        Integer exit = centreBlock.getLeftEntryRow();
                        if (exit < complexEntry)
                            totalDistance += ((complexEntry - exit) / centreBlock.getRows().size()) * centreBlock.getPathLength();
                        else
                            totalDistance += ((exit - complexEntry) / centreBlock.getRows().size()) * centreBlock.getPathLength();
                    } else {
                        totalDistance += rightEntryDistance;
                        Integer exit = centreBlock.getRightEntryRow();
                        if (exit < complexEntry)
                            totalDistance += ((complexEntry - exit) / centreBlock.getRows().size()) * centreBlock.getPathLength();
                        else
                            totalDistance += ((exit - complexEntry) / centreBlock.getRows().size()) * centreBlock.getPathLength();
                    }
                } else {
                    paths.add(centreBlock.getRows().get(complexEntry).getSeatCount() * centreBlock.getSeatWidth());
                }
            } else {
                if (rightEntryDistance != -1) {
                    if (!innerStart)
                        paths.add(leftBlock.getRows().get(complexEntry).getSeatCount() * leftBlock.getSeatWidth());
                    totalDistance += leftEntryDistance;
                } else {
                    totalDistance += rightEntryDistance;
                }
            }
        }

        Integer optimalPosition = determineMinimum(paths);
        return optimalPosition;
    }

    public TreeMap<Integer, Order> getCount(Boolean up, ArrayList<Order> orders) {
        if (up) {
            return new TreeMap<>((Integer o1, Integer o2) -> o1.compareTo(o2));
        } else {
            return new TreeMap<>((Integer o2, Integer o1) -> o1.compareTo(o2));
        }
    }

    private Integer determineMinimum(ArrayList<Double> paths) {
        Integer index = -1;
        Double min = 99999999999.0;
        Double curDistance;
        for (int i = 0; i < paths.size(); i++) {
            curDistance = paths.get(i);
            if (curDistance < min) {
                min = curDistance;
                index = i;
            }
        }
        return index;
    }

    private void populate_simpleStructures() {
        simpleStructures = new HashMap<>();

        for (int i = 0; i < 4; i++) {
            ArrayList<ArrayList<Order>> newStructure = new ArrayList<>();
            simpleStructures.put(i, newStructure);
        }
    }

    private Double sumUpSimplePath(Double path, Block block, Order initiate, TreeMap<Integer, Order> uniqueRows, ArrayList<Order> orders, Integer structureIndex, Integer entry, Double leftEntryDistance, Double rightEntryDistance, boolean initialDirection, boolean complexEnd, boolean innerStart) {

        ArrayList<ArrayList<Order>> structure = simpleStructures.get(structureIndex);
        Double seatWidth = block.getSeatWidth();
        Double rowCount = Double.valueOf(block.getRows().size());
        Double pathLength = initiate.getBlock().getPathLength();
        Boolean reversed = initialDirection;
        ArrayList<Integer> values = new ArrayList<Integer>();
        int rowIndicator = 0;
        Order curO;

        for (Integer i : uniqueRows.keySet())
            values.add(i);

        //add up all vertical distances of the path.
        if (values.get(0) < entry) {
            if (values.get(values.size() - 1) < entry) {
                path += ((entry - values.get(0)) / rowCount) * 2.0 * pathLength;
            } else {
                path += ((entry - values.get(0)) / rowCount) * 2.0 * pathLength;
                path += ((values.get(values.size() - 1) - entry) / rowCount) * 2.0 * pathLength;
            }
        } else {
            if (values.get(values.size() - 1) > entry) {
                path += ((values.get(values.size() - 1) - entry) / rowCount) * 2.0 * pathLength;
            } else {
                path += ((entry - values.get(values.size() - 1)) / rowCount) * 2.0 * pathLength;
                path += ((values.get(0) - entry) / rowCount) * 2.0 * pathLength;
            }
        }

        SeatComparator activeSort = new SeatComparator(reversed);
        Double newLength2, decisionVar1, prevLength1;
        Double newLength1, decisionVar2, prevLength2;

        for (int x = 0; x < uniqueRows.size(); x++) {
            Integer i = values.get(x);
            curO = orders.get(rowIndicator);

            ArrayList<Order> newRow = new ArrayList<>();
            newRow.add(curO);
            Integer validate = i;

            while (curO.getRowNumber().equals(validate)) {
                if (!newRow.contains(curO))
                    newRow.add(curO);
                rowIndicator++;
                if (rowIndicator != orders.size())
                    curO = orders.get(rowIndicator);
                else
                    validate++;
            }

            if (reversed != activeSort.getDescending())
                activeSort.setDirection(reversed);
            if (newRow.size() > 1)
                Collections.sort(newRow, activeSort);

            Double seatCount = Double.valueOf(block.getRows().get(i - 1).getSeatCount());
            if (reversed) {
                newLength2 = (seatCount - newRow.get(newRow.size() - 1).getSeat()) * seatWidth;
            } else {
                newLength2 = newRow.get(newRow.size() - 1).getSeat() * seatWidth;
            }
            if (uniqueRows.size() == 1) {
                path += determineLastRow(path, complexEnd, reversed, leftEntryDistance, rightEntryDistance, newLength2, seatWidth, seatCount, initialDirection, innerStart);
                structure.add(newRow);
                return path;
            }

            if (x != 0) {
                ArrayList<Order> prevR = structure.get(x - 1);
                Double prevSeatCount = Double.valueOf(block.getRows().get(values.get(x - 1) - 1).getSeatCount());

                if (reversed) {
                    prevLength2 = (prevSeatCount - prevR.get(prevR.size() - 1).getSeat()) * seatWidth;
                    newLength1 = newRow.get(newRow.size() - 1).getSeat() * seatWidth;
                    prevLength1 = prevR.get(prevR.size() - 1).getSeat() * seatWidth;
                } else {
                    prevLength2 = prevR.get(prevR.size() - 1).getSeat() * seatWidth;
                    newLength1 = (seatCount - newRow.get(0).getSeat()) * seatWidth;
                    prevLength1 = (prevSeatCount - prevR.get(prevR.size() - 1).getSeat()) * seatWidth;
                }
                decisionVar1 = prevLength1 + newLength1;
                decisionVar2 = prevLength2 + newLength2;

                if (decisionVar1 < decisionVar2) {
                    reversed = !reversed;
                    activeSort.setDirection(reversed);
                    Collections.sort(newRow, activeSort);
                    path += prevSeatCount * seatWidth;
                } else {
                    path += prevLength2;
                }
                structure.add(newRow);
                if (x == uniqueRows.size() - 1) {
                    path += determineLastRow(path, complexEnd, reversed, leftEntryDistance, rightEntryDistance, newLength2, seatWidth, seatCount, initialDirection, innerStart);
                }
            } else {
                structure.add(newRow);
            }
        }
        return path;
    }

    private Double determineLastRow(Double path, Boolean complexEnd, Boolean reversed, Double leftEntryDistance, Double rightEntryDistance, Double newLength2, Double seatWidth, Double seatCount, Boolean initialDirection, boolean innerStart) {
        if (complexEnd) {
            Double leftExit;
            Double rightExit;
            if (reversed) {
                leftExit = leftEntryDistance + newLength2 * 2.0;
                rightExit = rightEntryDistance + seatCount * seatWidth;
            } else {
                rightExit = rightEntryDistance + newLength2 * 2.0;
                leftExit = leftEntryDistance + seatCount * seatWidth;
            }
            if (rightEntryDistance == -1) {
                path += leftExit + 1;
            } else if (leftEntryDistance == -1) {
                path += rightExit + 1;
            } else {
                if (leftExit >= rightExit)
                    path += rightExit;
                else
                    path += leftExit;
            }
        } else {
            if (innerStart)
                path += (seatCount * seatWidth - newLength2) * 2.0;
            else {
                if ((initialDirection && reversed) || (!initialDirection && !reversed)) {
                    path += (seatCount * seatWidth) - newLength2;
                } else {
                    path += seatCount * seatWidth;
                }
            }
        }
        return path;
    }

    //Getters, Setter, Comparators, Interface Communication, Threading
    public ArrayList<ArrayList<Order>> getSimpleStructure(Integer index) {
        return simpleStructures.get(index);
    }

    public ArrayList<ArrayList<Order>> getComplexStructure(Integer index) {
        return complexStructure.get(index);
    }

    public String getDispatchName() {
        return dispatchName;
    }

    public Block getRightBlock() {
        return rightBlock;
    }

    public Block getCentreBlock() {
        return centreBlock;
    }

    public Block getLeftBlock() {
        return leftBlock;
    }

    public void clearSimpleStructures() {
        this.simpleStructures.clear();
    }

    public void clearComplexStructure() {
        this.complexStructure.clear();
        this.complexFinalDistances.clear();
    }

    public Order getOrder(String username, Integer orderID) {
        for (int i = 0; i < clusters.size(); i++) {
            ArrayList<Order> ar = clusters.get(i).getOrders();
            for (Order o : ar) {
                if (o.getUser().equals(username) && o.getOrderID().equals(orderID))
                    return o;
            }
        }
        return null;
    }

    public int returnIndex(Order order) {
        for (int i = 0; i < clusters.size(); i++) {
            if (clusters.get(i).getOrders().contains(order))
                return i;
        }
        return -1;
    }

    public Dispatch_Junction getJunction() {
        return junction;
    }

    public Double getComplexFinalDistance(Integer optimalIndex) {
        return complexFinalDistances.get(optimalIndex);
    }

    public Double getPathsDistance(Integer optimalIndex) {
        return paths.get(optimalIndex);
    }

    public void clearPaths() {
        paths.clear();
    }

    public void clearFinalComplexDistances() {
        complexFinalDistances.clear();
    }

    public Integer getDispatchID() {
        return dispatchID;
    }

    public class RowComparator implements Comparator<Order> {
        private Boolean descending;

        public RowComparator(Boolean asc) {
            this.descending = asc;
        }

        @Override
        public int compare(Order o1, Order o2) {
            if (descending) {
                if (o2.getRowNumber() == o1.getRowNumber())
                    return 0;
                else if (o2.getRowNumber() < o1.getRowNumber())
                    return -1;
                else
                    return 1;
            } else {
                if (o1.getRowNumber() == o2.getRowNumber())
                    return 0;
                else if (o1.getRowNumber() < o2.getRowNumber())
                    return -1;
                else
                    return 1;
            }
        }
    }

    public class SeatComparator implements Comparator<Order> {
        private Boolean descending;

        public SeatComparator(Boolean asc) {
            this.descending = asc;
        }

        public SeatComparator() {
        }

        @Override
        public int compare(Order s1, Order s2) {
            if (descending) {
                if (s2.getSeat() == s1.getSeat())
                    return 0;
                else if (s2.getSeat() < s1.getSeat())
                    return -1;
                else
                    return 1;
            } else {
                if (s1.getSeat() == s2.getSeat())
                    return 0;
                else if (s1.getSeat() < s2.getSeat())
                    return -1;
                else
                    return 1;
            }
        }

        public void setDirection(Boolean reversed) {
            this.descending = reversed;
        }

        public Boolean getDescending() {
            return descending;
        }
    }

    private class BlockNameComparator implements Comparator<Order> {

        @Override
        public int compare(Order o1, Order o2) {
            return (o1.getBlock().getName().compareTo(o2.getBlock().getName()));
        }
    }
}
