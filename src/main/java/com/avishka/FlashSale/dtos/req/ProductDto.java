package com.avishka.FlashSale.dtos.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDto {
    private String name;
    private double price;
    private int stock;
    private String imageUrl;
}
