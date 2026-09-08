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

    @Transactional(noRollbackFor = RuntimeException.class)
    public Order placeOrder(CreateOrderRequest createOrderRequest) {
        System.out.println(
                "START placeOrder - customer: "
                        + createOrderRequest.getCustomerId()
                        + " - thread: "
                        + Thread.currentThread().getName()
        );
        // Acquire Pessimistic Write Lock (SELECT ... FOR UPDATE) on the product row
        Product product = productRepository.findByIdWithPessimisticLock(createOrderRequest.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + createOrderRequest.getProductId()));

        String status;
        // Protected purchase check under pessimistic write lock
        if (product.getStock() >= createOrderRequest.getQuantity()) {
            product.setStock(product.getStock() - createOrderRequest.getQuantity());
            productRepository.save(product);
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

        Order savedOrder  = orderRepository.save(order);
        System.out.println(
                "END placeOrder - customer: "
                        + createOrderRequest.getCustomerId()
        );
        if(status.equals("SUCCEEDED")){
            return savedOrder;
        }else{
            throw new RuntimeException("Order failed due to insufficient stock.");
        }
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByIdDesc();
    }

    @Transactional
    public void clearAllOrders() {
        orderRepository.deleteAll();
    }
}
