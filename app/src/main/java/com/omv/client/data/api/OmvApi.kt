package com.omv.client.data.api

import com.omv.client.data.model.RpcRequest
import com.omv.client.data.model.RpcResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface OmvApi {

    @POST("rpc.php")
    suspend fun rpcCall(@Body request: RpcRequest): Response<RpcResponse>
}
