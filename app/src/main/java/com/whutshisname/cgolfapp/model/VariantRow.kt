package com.whutshisname.cgolfapp.model

data class VariantRow(
    val id: String,
    val clubPid: String,
    val productName: String,
    val clubSet: String,
    val club: String,
    val loft: String,
    val shaftType: String,
    val shaftFlex: String,
    val length: String,
    val outletPrice: String,   val outletUrl: String?,
    val likeNewPrice: String,  val likeNewUrl: String?,
    val veryGoodPrice: String, val veryGoodUrl: String?,
    val goodPrice: String,     val goodUrl: String?,
    val averagePrice: String,  val averageUrl: String?
)

// Lowest numeric price across all available conditions, or null if none available.
fun VariantRow.bestPrice(): Double? = listOf(
    outletPrice, likeNewPrice, veryGoodPrice, goodPrice, averagePrice
).mapNotNull { price ->
    if (price != "-" && price.isNotBlank()) price.trimStart('$').toDoubleOrNull() else null
}.minOrNull()

// Number of conditions that have a real price (not "-").
fun VariantRow.availableConditionCount(): Int = listOf(
    outletPrice, likeNewPrice, veryGoodPrice, goodPrice, averagePrice
).count { it != "-" && it.isNotBlank() }
