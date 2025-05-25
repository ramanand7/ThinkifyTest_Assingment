package thinkifytest;

import java.util.*;
import java.math.BigDecimal;

class MenuItem {
    private String name;
    private BigDecimal price;

    public MenuItem(String name, BigDecimal price) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Menu item name cannot be null or empty");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Menu item price must be positive");
        }
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Menu item price must be positive");
        }
        this.price = price;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        MenuItem other = (MenuItem) obj;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}

class Restaurant {
    private String name;
    private int maxOrders;
    private Map<String, MenuItem> menu;
    private double rating;
    private int currentOrderCount;

    public Restaurant(String name, int maxOrders, double rating) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Restaurant name cannot be null or empty");
        }
        if (maxOrders <= 0) {
            throw new IllegalArgumentException("Max orders must be positive");
        }
        if (rating < 0 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 0 and 5");
        }
        this.name = name;
        this.maxOrders = maxOrders;
        this.rating = rating;
        this.menu = new HashMap<>();
        this.currentOrderCount = 0;
    }

    public void addMenuItem(String itemName, BigDecimal price) {
        menu.put(itemName, new MenuItem(itemName, price));
    }

    public void updateMenuItemPrice(String itemName, BigDecimal price) {
        MenuItem item = menu.get(itemName);
        if (item == null) {
            throw new IllegalArgumentException("Menu item not found: " + itemName);
        }
        item.setPrice(price);
    }

    public boolean hasAllItems(Map<String, Integer> orderItems) {
        for (String item : orderItems.keySet()) {
            if (!menu.containsKey(item))
                return false;
        }
        return true;
    }

    public BigDecimal calculateTotalCost(Map<String, Integer> orderItems) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> entry : orderItems.entrySet()) {
            MenuItem item = menu.get(entry.getKey());
            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
        }
        return total;
    }

    public boolean canAcceptOrder() {
        return currentOrderCount < maxOrders;
    }

    public void acceptOrder() {
        if (!canAcceptOrder()) {
            throw new IllegalStateException("Restaurant has reached maximum order capacity");
        }
        currentOrderCount++;
    }

    public void completeOrder() {
        if (currentOrderCount <= 0) {
            throw new IllegalStateException("No orders to complete");
        }
        currentOrderCount--;
    }

    public String getName() {
        return name;
    }

    public int getMaxOrders() {
        return maxOrders;
    }

    public double getRating() {
        return rating;
    }

    public int getCurrentOrderCount() {
        return currentOrderCount;
    }

    public Map<String, MenuItem> getMenu() {
        return new HashMap<>(menu);
    }
}

enum OrderStatus {
    PENDING, ACCEPTED, COMPLETED, REJECTED
}

class Order {
    private static int orderCounter = 1;
    private int orderId;
    private String userName;
    private Map<String, Integer> items;
    private OrderStatus status;
    private Restaurant assignedRestaurant;
    private BigDecimal totalCost;

    public Order(String userName, Map<String, Integer> items) {
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be null or empty");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order items cannot be null or empty");
        }
        for (Integer qty : items.values()) {
            if (qty <= 0)
                throw new IllegalArgumentException("Item quantities must be positive");
        }
        this.orderId = orderCounter++;
        this.userName = userName;
        this.items = new HashMap<>(items);
        this.status = OrderStatus.PENDING;
    }

    public void assignToRestaurant(Restaurant restaurant) {
        this.assignedRestaurant = restaurant;
        this.totalCost = restaurant.calculateTotalCost(items);
        this.status = OrderStatus.ACCEPTED;
        restaurant.acceptOrder();
    }

    public void markCompleted() {
        if (status != OrderStatus.ACCEPTED) {
            throw new IllegalStateException("Only accepted orders can be completed");
        }
        this.status = OrderStatus.COMPLETED;
        if (assignedRestaurant != null) {
            assignedRestaurant.completeOrder();
        }
    }

    public void markRejected() {
        this.status = OrderStatus.REJECTED;
    }

    public int getOrderId() {
        return orderId;
    }

    public String getUserName() {
        return userName;
    }

    public Map<String, Integer> getItems() {
        return new HashMap<>(items);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Restaurant getAssignedRestaurant() {
        return assignedRestaurant;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }
}

interface SelectionStrategy {
    Optional<Restaurant> selectRestaurant(List<Restaurant> eligibleRestaurants, Map<String, Integer> orderItems);
}

class LowestCostStrategy implements SelectionStrategy {
    @Override
    public Optional<Restaurant> selectRestaurant(List<Restaurant> eligibleRestaurants,
            Map<String, Integer> orderItems) {
        Restaurant minCostRestaurant = null;
        BigDecimal minCost = null;
        for (Restaurant r : eligibleRestaurants) {
            BigDecimal cost = r.calculateTotalCost(orderItems);
            if (minCost == null || cost.compareTo(minCost) < 0) {
                minCost = cost;
                minCostRestaurant = r;
            }
        }
        return Optional.ofNullable(minCostRestaurant);
    }
}

class HighestRatingStrategy implements SelectionStrategy {
    @Override
    public Optional<Restaurant> selectRestaurant(List<Restaurant> eligibleRestaurants,
            Map<String, Integer> orderItems) {
        Restaurant best = null;
        double maxRating = -1;
        for (Restaurant r : eligibleRestaurants) {
            if (r.getRating() > maxRating) {
                maxRating = r.getRating();
                best = r;
            }
        }
        return Optional.ofNullable(best);
    }
}

class OrderProcessingException extends Exception {
    public OrderProcessingException(String message) {
        super(message);
    }
}

class RestaurantNotFoundException extends Exception {
    public RestaurantNotFoundException(String message) {
        super(message);
    }
}

class FoodOrderingSystem {
    private Map<String, Restaurant> restaurants;
    private Map<Integer, Order> orders;
    private SelectionStrategy currentStrategy;

    public FoodOrderingSystem() {
        restaurants = new HashMap<>();
        orders = new HashMap<>();
        currentStrategy = new LowestCostStrategy();
    }

    public void setSelectionStrategy(SelectionStrategy strategy) {
        if (strategy == null)
            throw new IllegalArgumentException("Selection strategy cannot be null");
        currentStrategy = strategy;
    }

    public void onboardRestaurant(String name, int maxOrders, double rating) {
        if (restaurants.containsKey(name)) {
            throw new IllegalArgumentException("Restaurant already exists: " + name);
        }
        Restaurant restaurant = new Restaurant(name, maxOrders, rating);
        restaurants.put(name, restaurant);
        System.out.println("Restaurant onboarded successfully: " + name);
    }

    public void addMenuItemToRestaurant(String restaurantName, String itemName, BigDecimal price)
            throws RestaurantNotFoundException {
        Restaurant restaurant = restaurants.get(restaurantName);
        if (restaurant == null) {
            throw new RestaurantNotFoundException("Restaurant not found: " + restaurantName);
        }
        restaurant.addMenuItem(itemName, price);
        System.out.println("Menu item added to " + restaurantName + ": " + itemName + " - " + price);
    }

    public void updateMenuItemPrice(String restaurantName, String itemName, BigDecimal price)
            throws RestaurantNotFoundException {
        Restaurant restaurant = restaurants.get(restaurantName);
        if (restaurant == null) {
            throw new RestaurantNotFoundException("Restaurant not found: " + restaurantName);
        }
        restaurant.updateMenuItemPrice(itemName, price);
        System.out.println("Menu item updated in " + restaurantName + ": " + itemName + " - " + price);
    }

    public Order placeOrder(String userName, Map<String, Integer> items, SelectionStrategy strategy)
            throws OrderProcessingException {
        Order order = new Order(userName, items);
        SelectionStrategy strategyToUse = (strategy != null) ? strategy : currentStrategy;
        List<Restaurant> eligibleRestaurants = new ArrayList<>();
        for (Restaurant r : restaurants.values()) {
            if (r.hasAllItems(items) && r.canAcceptOrder()) {
                eligibleRestaurants.add(r);
            }
        }
        if (eligibleRestaurants.isEmpty()) {
            order.markRejected();
            orders.put(order.getOrderId(), order);
            throw new OrderProcessingException("Cannot assign the order - no eligible restaurants found");
        }
        Optional<Restaurant> selectedRestaurant = strategyToUse.selectRestaurant(eligibleRestaurants, items);
        if (selectedRestaurant.isPresent()) {
            order.assignToRestaurant(selectedRestaurant.get());
            orders.put(order.getOrderId(), order);
            System.out.println("Order " + order.getOrderId() + " assigned to " + selectedRestaurant.get().getName()
                    + " (Total: " + order.getTotalCost() + ")");
            return order;
        } else {
            order.markRejected();
            orders.put(order.getOrderId(), order);
            throw new OrderProcessingException("Cannot assign the order - strategy failed to select restaurant");
        }
    }

    public void markOrderCompleted(int orderId) throws OrderProcessingException {
        Order order = orders.get(orderId);
        if (order == null) {
            throw new OrderProcessingException("Order not found: " + orderId);
        }
        order.markCompleted();
        System.out.println("Order " + orderId + " marked as completed");
    }

    public void displayRestaurantStatus() {
        System.out.println("\n=== Restaurant Status ===");
        for (Restaurant r : restaurants.values()) {
            System.out.println(r.getName() + " - Orders: " + r.getCurrentOrderCount() + "/" + r.getMaxOrders()
                    + ", Rating: " + r.getRating());
        }
    }

    public void displayOrderStatus() {
        System.out.println("\n=== Order Status ===");
        for (Order o : orders.values()) {
            System.out.println("Order " + o.getOrderId() + " - " + o.getUserName() + " - Status: " + o.getStatus()
                    + (o.getAssignedRestaurant() != null ? " - Restaurant: " + o.getAssignedRestaurant().getName()
                            : ""));
        }
    }
}

public class FoodOrderingApp {
    public static void main(String[] args) {
        FoodOrderingSystem system = new FoodOrderingSystem();
        try {
            System.out.println("=== Onboarding Restaurants ===");
            system.onboardRestaurant("R1", 5, 4.5);
            system.addMenuItemToRestaurant("R1", "Veg Biryani", new BigDecimal("100"));
            system.addMenuItemToRestaurant("R1", "Chicken Biryani", new BigDecimal("150"));
            system.onboardRestaurant("R2", 5, 4.0);
            system.addMenuItemToRestaurant("R2", "Idli", new BigDecimal("10"));
            system.addMenuItemToRestaurant("R2", "Dosa", new BigDecimal("50"));
            system.addMenuItemToRestaurant("R2", "Veg Biryani", new BigDecimal("80"));
            system.addMenuItemToRestaurant("R2", "Chicken Biryani", new BigDecimal("175"));
            system.onboardRestaurant("R3", 1, 4.9);
            system.addMenuItemToRestaurant("R3", "Idli", new BigDecimal("15"));
            system.addMenuItemToRestaurant("R3", "Dosa", new BigDecimal("30"));
            system.addMenuItemToRestaurant("R3", "Gobi Manchurian", new BigDecimal("150"));
            system.addMenuItemToRestaurant("R3", "Chicken Biryani", new BigDecimal("175"));
            System.out.println("\n=== Updating Menus ===");
            system.addMenuItemToRestaurant("R1", "Chicken65", new BigDecimal("250"));
            system.updateMenuItemPrice("R2", "Chicken Biryani", new BigDecimal("150"));
            System.out.println("\n=== Placing Orders ===");
            Map<String, Integer> order1Items = new HashMap<>();
            order1Items.put("Idli", 3);
            order1Items.put("Dosa", 1);
            system.placeOrder("Ashwin", order1Items, new LowestCostStrategy());
            Map<String, Integer> order2Items = new HashMap<>();
            order2Items.put("Idli", 3);
            order2Items.put("Dosa", 1);
            system.placeOrder("Harish", order2Items, new LowestCostStrategy());
            Map<String, Integer> order3Items = new HashMap<>();
            order3Items.put("Veg Biryani", 3);
            order3Items.put("Dosa", 1);
            system.placeOrder("Shruthi", order3Items, new HighestRatingStrategy());
            system.displayRestaurantStatus();
            system.displayOrderStatus();
            System.out.println("\n=== Completing Order and Placing New Order ===");
            system.markOrderCompleted(1);
            Map<String, Integer> order4Items = new HashMap<>();
            order4Items.put("Idli", 3);
            order4Items.put("Dosa", 1);
            system.placeOrder("Harish", order4Items, new LowestCostStrategy());
            System.out.println("\n=== Testing Order with Unavailable Item ===");
            Map<String, Integer> order5Items = new HashMap<>();
            order5Items.put("Idli", 3);
            order5Items.put("Paneer Tikka", 1);
            try {
                system.placeOrder("Diya", order5Items, new LowestCostStrategy());
            } catch (OrderProcessingException e) {
                System.out.println("Expected failure: " + e.getMessage());
            }
            system.displayRestaurantStatus();
            system.displayOrderStatus();
            System.out.println("\n=== Testing Validations ===");
            try {
                system.onboardRestaurant("", 5, 4.0);
            } catch (IllegalArgumentException e) {
                System.out.println("Validation passed: " + e.getMessage());
            }
            try {
                system.onboardRestaurant("TestRestaurant", -1, 4.0);
            } catch (IllegalArgumentException e) {
                System.out.println("Validation passed: " + e.getMessage());
            }
            try {
                system.onboardRestaurant("TestRestaurant", 5, 6.0);
            } catch (IllegalArgumentException e) {
                System.out.println("Validation passed: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}