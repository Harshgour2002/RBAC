package com.university.auth.service;

import com.university.auth.dto.ProductCreateRequest;
import com.university.auth.dto.ProductResponse;
import com.university.auth.dto.ProductUpdateRequest;
import java.util.List;
import java.util.UUID;

public interface ProductService {

    ProductResponse createProduct(ProductCreateRequest request);

    ProductResponse getProductById(UUID id);

    List<ProductResponse> getAllProducts();

    ProductResponse updateProduct(UUID id, ProductUpdateRequest request);

    void deleteProduct(UUID id);
}