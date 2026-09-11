package com.avishka.FlashSale.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.avishka.FlashSale.dtos.req.CreateOrderRequest;
import com.avishka.FlashSale.entity.Order;
import com.avishka.FlashSale.entity.Product;
import com.avishka.FlashSale.repositories.OrderRepository;
import com.avishka.FlashSale.repositories.ProductRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Order placeOrder(CreateOrderRequest createOrderRequest) {
        System.out.println(
                "START placeOrder - customer: "
                        + createOrderRequest.getCustomerId()
                        + " - thread: "
                        + Thread.currentThread().getName()
        );

        // Resolve requested product (or fallback to first available product in DB)
        Product product = productRepository.findById(createOrderRequest.getProductId())
                .orElseGet(() -> productRepository.findAll().stream().findFirst().orElse(null));

        if (product == null) {
            throw new RuntimeException("No products available in database.");
        }

        // Execute Atomic SQL Update: UPDATE products SET stock = stock - qty WHERE id = ? AND stock >= qty
        int rowsUpdated = productRepository.decreaseStockAtomic(product.getId(), createOrderRequest.getQuantity());

        String status;
        if (rowsUpdated > 0) {
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

        Order savedOrder = orderRepository.save(order);

        System.out.println(
                "END placeOrder - customer: "
                        + createOrderRequest.getCustomerId()
                        + " - status: " + status
        );

        return savedOrder;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByIdDesc();
    }

    @Transactional
    public void clearAllOrders() {
        orderRepository.deleteAllInBatch();
    }
}
