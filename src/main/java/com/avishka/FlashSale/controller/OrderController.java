package com.avishka.FlashSale.controller;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.avishka.FlashSale.entity.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.avishka.FlashSale.dtos.req.CreateOrderRequest;
import com.avishka.FlashSale.service.OrderService;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/api/v1/orders")
@Slf4j
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/buy")
    public ResponseEntity<Order> buyProduct(@RequestBody CreateOrderRequest createOrderRequest) {
        Order savedOrder = orderService.placeOrder(createOrderRequest);
        if ("SUCCEEDED".equalsIgnoreCase(savedOrder.getStatus())) {
            return ResponseEntity.ok(savedOrder);
        } else {
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(savedOrder);
        }
    }

    @PostMapping("/clear")
    public String clearAllOrders() {
        orderService.clearAllOrders();
        return "redirect:/api/v1/product";
    }
}
