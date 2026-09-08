package com.avishka.FlashSale.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.avishka.FlashSale.dtos.req.ProductDto;
import com.avishka.FlashSale.entity.Product;
import com.avishka.FlashSale.repositories.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public Product createProduct(ProductDto productDto) {
        Product product = Product.builder()
                .name(productDto.getName())
                .price(productDto.getPrice())
                .stock(productDto.getStock())
                .imageUrl(productDto.getImageUrl() != null && !productDto.getImageUrl().isBlank() 
                        ? productDto.getImageUrl() 
                        : "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80")
                .build();
        return productRepository.save(product);
    }

    public Product getProduct(int productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
    }

    public List<Product> getAllProducts() {
        if (productRepository.count() == 0) {
            seedDefaultProducts();
        }
        return productRepository.findAll();
    }

    @Transactional
    public void seedDefaultProducts() {
        if (productRepository.count() > 0) return;

        List<Product> defaultProducts = List.of(
            Product.builder()
                .name("UltraBook Pro Gaming Laptop")
                .price(1299.99)
                .stock(100)
                .imageUrl("https://images.unsplash.com/photo-1603302576837-37561b2e2302?auto=format&fit=crop&w=600&q=80")
                .build(),
            Product.builder()
                .name("Wireless Noise-Canceling Headphones")
                .price(249.99)
                .stock(50)
                .imageUrl("https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80")
                .build(),
            Product.builder()
                .name("SmartWatch Fitness Tracker v2")
                .price(199.99)
                .stock(75)
                .imageUrl("https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80")
                .build(),
            Product.builder()
                .name("Flagship Smartphone 5G")
                .price(899.99)
                .stock(30)
                .imageUrl("https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=600&q=80")
                .build()
        );

        productRepository.saveAll(defaultProducts);
    }

    @Transactional
    public void resetAllProductStock() {
        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            product.setStock(100);
        }
        productRepository.saveAll(products);
    }
}
