
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Application class simulates a basic order matching engine for limit orders
 * in a trading system. It maintains an order book with buy and sell orders,
 * matches incoming orders, and executes trades when conditions are met.
 */
public class Application {

    private final TreeMap<Integer, LinkedList<Order>> buyOrders = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Integer, LinkedList<Order>> sellOrders = new TreeMap<>();
    private final Map<String, Order> orderMap = new ConcurrentHashMap<>();
    private int orderIdCounter = 0;

    /**
     * Places a new limit order and attempts to match it against the order book.
     *
     * @param side     The side of the order ("B" for buy, "S" for sell).
     * @param quantity The quantity of the order.
     * @param price    The price of the order.
     * @return The unique ID of the newly placed order.
     */
    public synchronized String placeLimitOrder(String side, int quantity, int price,String orderId) {
        Order order = new Order(orderId, side, quantity, price);
        orderMap.put(order.orderId, order);
        if (side.equalsIgnoreCase("B")) {
            this.matchLimitOrder(order, sellOrders, buyOrders);
        } else {
            this.matchLimitOrder(order, buyOrders, sellOrders);
        }
        return order.orderId;
    }

    /**
     * Matches an incoming limit order with orders in the opposite order book.
     * If no matches are found, the order is added to the appropriate order book.
     *
     * @param incomingOrder The incoming order to be matched.
     * @param oppositeBook  The order book for the opposite side (sell orders for buy orders and vice versa).
     * @param sameBook      The order book for the same side as the incoming order.
     */
    private void matchLimitOrder(Order incomingOrder, TreeMap<Integer, LinkedList<Order>> oppositeBook, TreeMap<Integer, LinkedList<Order>> sameBook) {
        Iterator<Map.Entry<Integer, LinkedList<Order>>> iterator = oppositeBook.entrySet().iterator();

        while (iterator.hasNext() && incomingOrder.quantity > 0) {
            Map.Entry<Integer, LinkedList<Order>> entry = iterator.next();
            int matchPrice = entry.getKey();

            if ((incomingOrder.side.equalsIgnoreCase("B") && incomingOrder.price >= matchPrice) ||
                    (incomingOrder.side.equalsIgnoreCase("S") && incomingOrder.price <= matchPrice)) {

                LinkedList<Order> orderLinkedList = entry.getValue();

                while (!orderLinkedList.isEmpty() && incomingOrder.quantity > 0) {
                    Order matchedOrder = orderLinkedList.getFirst();
                    int tradeQuantity = Math.min(incomingOrder.quantity, matchedOrder.quantity);
                    incomingOrder.quantity -= tradeQuantity;
                    matchedOrder.quantity -= tradeQuantity;

                    System.out.println("Trade  "+incomingOrder.orderId+","+matchedOrder.orderId+"," + tradeQuantity + "," + matchPrice);

                    if (matchedOrder.quantity == 0) {
                        orderLinkedList.removeFirst();
                        orderMap.remove(matchedOrder.orderId);
                    }
                }

                if (orderLinkedList.isEmpty()) {
                    iterator.remove();
                }

            } else {
                break;
            }
        }

        if (incomingOrder.quantity > 0) {
            sameBook.putIfAbsent(incomingOrder.price, new LinkedList<>());
            sameBook.get(incomingOrder.price).add(incomingOrder);
        }
    }

    /**
     * Displays the current state of the order book for buy and sell orders.
     */
    public synchronized void displayOrderBook() {
        //System.out.println("Buy Orders:");
        buyOrders.forEach((price, orders) -> System.out.print(price + "," + orders.stream().mapToInt(o -> o.quantity).sum()));

        System.out.print("|");
        sellOrders.forEach((price, orders) -> System.out.print(price + "," + orders.stream().mapToInt(o -> o.quantity).sum()));
    }

    public static void main(String[] args) {
        Application application = new Application();

        String orderId1 = application.placeLimitOrder("B", 10, 100,"1000");
        String orderId2 = application.placeLimitOrder("S", 20, 100,"1001");
        String orderId3 = application.placeLimitOrder("S", 8, 101,"1002");

        String orderId4 = application.placeLimitOrder("B", 15, 100,"1003");

        application.displayOrderBook();
    }

    /**
     * Represents a limit order in the trading system.
     */
    static class Order {
        String orderId;
        String side;
        int quantity;
        int price;

        /**
         * Constructs a new Order object.
         *
         * @param orderId  The unique identifier for the order.
         * @param side     The side of the order ("B" for buy, "S" for sell).
         * @param quantity The quantity of the order.
         * @param price    The price of the order.
         */
        Order(String orderId, String side, int quantity, int price) {
            this.orderId = orderId;
            this.quantity = quantity;
            this.price = price;
            this.side = side;
        }
    }
}
