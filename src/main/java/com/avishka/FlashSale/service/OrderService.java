package com.avishka.FlashSale.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.avishka.FlashSale.dtos.req.CreateOrderRequest;
import com.avishka.FlashSale.entity.Order;
import com.avishka.FlashSale.entity.Product;
import com.avishka.FlashSale.repositories.OrderRepository;
import com.avishka.FlashSale.repositories.ProductRepository;

import jakarta.persistence.OptimisticLockException;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    public Order placeOrder(CreateOrderRequest createOrderRequest) {
        System.out.println(
                "START placeOrder - customer: "
                        + createOrderRequest.getCustomerId()
                        + " - thread: "
                        + Thread.currentThread().getName()
        );

        int maxRetries = 10;
        int attempt = 0;
        Order resultOrder = null;

        while (attempt < maxRetries) {
            attempt++;
            try {
                resultOrder = tryPlaceOrderOptimistic(createOrderRequest);
                break; // Order successfully processed without version collision
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                System.out.println("Optimistic Lock Conflict on attempt " + attempt + " for customer " + createOrderRequest.getCustomerId());
                if (attempt >= maxRetries) {
                    resultOrder = recordFailedOrder(createOrderRequest, "FAILED_OUT_OF_STOCK");
                }
            }
        }

        System.out.println(
                "END placeOrder - customer: "
                        + createOrderRequest.getCustomerId()
        );

        if (resultOrder != null && "SUCCEEDED".equalsIgnoreCase(resultOrder.getStatus())) {
            return resultOrder;
        } else {
            throw new RuntimeException("Order failed due to insufficient stock or concurrency conflict.");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = RuntimeException.class)
    public Order tryPlaceOrderOptimistic(CreateOrderRequest createOrderRequest) {
        Product product = productRepository.findById(createOrderRequest.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + createOrderRequest.getProductId()));

        String status;
        // Optimistic check: JPA automatically verifies @Version during saveAndFlush
        if (product.getStock() >= createOrderRequest.getQuantity()) {
            product.setStock(product.getStock() - createOrderRequest.getQuantity());
            productRepository.saveAndFlush(product); // Triggers optimistic version validation
            status = "SUCCEEDED";
        } else {
            status = "FAILED_OUT_OF_STOCK";
        }

        LocalDateTime now = LocalDateTime.now();
        Order order = Order.builder()
                .productId(product.getId())
                .productName(product.getName())
                .customerId(createOrderRequest.getCustomerId())
                .quantity(createOrderRequest.getQuantity())
                .price(product.getPrice() * createOrderRequest.getQuantity())
                .status(status)
                .orderedAt(now)
                .build();

        return orderRepository.save(order);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order recordFailedOrder(CreateOrderRequest createOrderRequest, String status) {
        Product product = productRepository.findById(createOrderRequest.getProductId()).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        Order order = Order.builder()
                .productId(createOrderRequest.getProductId())
                .productName(product != null ? product.getName() : "Unknown Product")
                .customerId(createOrderRequest.getCustomerId())
                .quantity(createOrderRequest.getQuantity())
                .price(product != null ? product.getPrice() * createOrderRequest.getQuantity() : 0.0)
                .status(status)
                .orderedAt(now)
                .build();

        return orderRepository.save(order);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByIdDesc();
    }

    @Transactional
    public void clearAllOrders() {
        orderRepository.deleteAll();
    }
}
