package com.avishka.FlashSale.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.avishka.FlashSale.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {


}
