package com.project.models

data class Product(
    val _id: String,
    val code: String,
    val name: String,
    val price: Double,
    val discountPercent: Int = 0,
    val stock: Int,
    val description: String,
//    val images: List<ProductImage>? = null,
    val rating: Double = 0.0,
    val reviewCount: Int = 0
)