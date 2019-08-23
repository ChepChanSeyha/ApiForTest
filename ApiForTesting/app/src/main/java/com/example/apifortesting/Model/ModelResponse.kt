package com.example.apifortesting.Model

import com.google.gson.annotations.SerializedName

data class ModelResponse(

	@field:SerializedName("route")
	val route: List<RouteItem?>? = null
)