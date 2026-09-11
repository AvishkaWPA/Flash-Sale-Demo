package com.avishka.FlashSale.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
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
    private final RedissonClient redissonClient;

    @Value("${server.port:8080}")
    private String serverPort;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        RedissonClient redissonClient) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.redissonClient = redissonClient;
    }

    public Order placeOrder(CreateOrderRequest createOrderRequest) {
        System.out.println(
                "START placeOrder [Port " + serverPort + "] - customer: "
                        + createOrderRequest.getCustomerId()
                        + " - thread: "
                        + Thread.currentThread().getName()
        );

        // Resolve requested product (or fallback to first available product in DB)
        Product targetProduct = productRepository.findById(createOrderRequest.getProductId())
                .orElseGet(() -> productRepository.findAll().stream().findFirst().orElse(null));

        if (targetProduct == null) {
            throw new RuntimeException("No products available in database.");
        }

        // Redis Distributed Shared Lock key across all instances (Port 8080, Port 8081, etc.)
        String lockKey = "lock:product:" + targetProduct.getId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Acquire Redis Shared Lock (wait up to 10s, watchdog auto-renewal enabled via -1)
            boolean isAcquired = lock.tryLock(10, -1, TimeUnit.SECONDS);

            if (!isAcquired) {
                System.out.println("Could not acquire Redis lock for product #" + targetProduct.getId());
                return recordOrderInternal(targetProduct, createOrderRequest.getCustomerId(), createOrderRequest.getQuantity(), "FAILED_CONCURRENCY_CONFLICT");
            }

            try {
                // Process order under cross-instance Redis distributed lock
                return processOrderUnderLock(targetProduct.getId(), createOrderRequest);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Redis lock acquisition interrupted", e);
        }
    }

    @Transactional
    public Order processOrderUnderLock(int productId, CreateOrderRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        String status;
        if (product.getStock() >= request.getQuantity()) {
            product.setStock(product.getStock() - request.getQuantity());
            productRepository.save(product);
            status = "SUCCEEDED";
        } else {
            status = "FAILED_OUT_OF_STOCK";
        }

        return recordOrderInternal(product, request.getCustomerId(), request.getQuantity(), status);
    }

    @Transactional
    public Order recordOrderInternal(Product product, int customerId, int quantity, String status) {
        LocalDateTime now = LocalDateTime.now();
        Order order = Order.builder()
                .productId(product != null ? product.getId() : 0)
                .productName(product != null ? product.getName() : "Unknown Product")
                .customerId(customerId)
                .quantity(quantity)
                .price(product != null ? product.getPrice() * quantity : 0.0)
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
        orderRepository.deleteAllInBatch();
    }
}
