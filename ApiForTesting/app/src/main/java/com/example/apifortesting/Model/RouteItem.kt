package com.example.apifortesting.Model

import com.google.gson.annotations.SerializedName

data class RouteItem(

	@field:SerializedName("coordinates")
	val coordinates: List<List<Double?>?>? = null
)