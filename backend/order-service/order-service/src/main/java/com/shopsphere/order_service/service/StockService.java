package com.shopsphere.order_service.service;

import com.shopsphere.order_service.dto.ProductResponse;
import com.shopsphere.order_service.entity.Cart;
import com.shopsphere.order_service.entity.CartItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final RestTemplate restTemplate;

    private static final String PRODUCT_SERVICE_URL = "http://localhost:8082/api/products/";

    public void validateStock(Cart cart) {
        for (CartItem item : cart.getItems()) {
            ProductResponse product = restTemplate.getForObject(
                    PRODUCT_SERVICE_URL + item.getProductId(),
                    ProductResponse.class);

            if (product == null) {
                throw new RuntimeException("Product not found: " + item.getProductId());
            }
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException(
                        "Insufficient stock for " + item.getProductName()
                                + " | Available: " + product.getStockQuantity()
                                + " | Requested: " + item.getQuantity());
            }
        }
    }
}
