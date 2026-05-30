package com.whutshisname.cgolfapp.model

data class VariantRow(
    val id: String,
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
