package com.avishka.FlashSale.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.avishka.FlashSale.dtos.req.ProductDto;
import com.avishka.FlashSale.entity.Order;
import com.avishka.FlashSale.entity.Product;
import com.avishka.FlashSale.service.OrderService;
import com.avishka.FlashSale.service.ProductService;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/api/v1/product")
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final OrderService orderService;

    public ProductController(ProductService productService, OrderService orderService) {
        this.productService = productService;
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseBody
    public Product addProduct(@RequestBody ProductDto productDto) {
        return productService.createProduct(productDto);
    }
    
    @GetMapping("/{productId}")
    @ResponseBody
    public Product getProduct(@PathVariable int productId) {
        return productService.getProduct(productId);
    }

    @GetMapping
    public String getAllProducts(Model model) {
        List<Product> products = productService.getAllProducts();
        List<Order> orders = orderService.getAllOrders();
        Product testingProduct = products.isEmpty() ? null : products.get(0);

        long succeededCount = orders.stream().filter(o -> "SUCCEEDED".equalsIgnoreCase(o.getStatus())).count();
        long failedCount = orders.stream().filter(o -> "FAILED_OUT_OF_STOCK".equalsIgnoreCase(o.getStatus())).count();
        
        // Calculate oversold count (if succeeded orders exceed initial stock capacity)
        long totalInitialStock = 100; // default benchmark stock per product
        long oversoldQuantity = Math.max(0, succeededCount - totalInitialStock - (testingProduct != null ? testingProduct.getStock() : 0));

        model.addAttribute("products", products);
        model.addAttribute("orders", orders);
        model.addAttribute("totalOrders", orders.size());
        model.addAttribute("succeededCount", succeededCount);
        model.addAttribute("failedCount", failedCount);
        model.addAttribute("oversoldQuantity", oversoldQuantity);

        log.info("Loaded {} products and {} orders for Admin Dashboard", products.size(), orders.size());
        return "allProducts";
    }

    @PostMapping("/reset-stock")
    public String resetStock() {
        productService.resetAllProductStock();
        orderService.clearAllOrders();
        return "redirect:/api/v1/product";
    }
}
