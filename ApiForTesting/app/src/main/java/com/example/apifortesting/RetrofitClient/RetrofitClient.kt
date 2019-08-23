package com.example.apifortesting.RetrofitClient

import com.example.apifortesting.Model.ModelResponse
import retrofit2.Call
import retrofit2.http.GET

interface RetrofitMaps {

    @GET("route?start_lng=104.8912&start_lat=11.5684&end_lng=104.8834&end_lat=11.5040&route=osrm")
    fun getDistanceDuration(): Call<ModelResponse>

}