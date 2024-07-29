package com.example.currency_converter_app

import com.google.gson.annotations.SerializedName


data class ExchangeRatesResponse(
    @SerializedName("base") val base: String = "",
    @SerializedName("date") val date: String = "",
    @SerializedName("rates") val rates: Map<String, Double > = emptyMap()

)
