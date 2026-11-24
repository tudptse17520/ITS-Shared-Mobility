package vn.edu.ut.its.mobility.network

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import vn.edu.ut.its.mobility.BuildConfig
import java.io.EOFException
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

object RetrofitClient {

    @Volatile
    var authToken: String? = null

    @Volatile
    var onUnauthorized: (() -> Unit)? = null

    private val nullOnEmptyConverterFactory = object : Converter.Factory() {
        override fun responseBodyConverter(
            type: Type,
            annotations: Array<out Annotation>,
            retrofit: Retrofit
        ): Converter<ResponseBody, *> {
            val delegate = retrofit.nextResponseBodyConverter<Any?>(this, type, annotations)
            return Converter<ResponseBody, Any?> { body ->
                if (body.contentLength() == 0L || !body.source().request(1L)) {
                    body.close()
                    null
                } else {
                    try {
                        delegate.convert(body)
                    } catch (_: EOFException) {
                        null
                    }
                }
            }
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
            authToken?.let { token ->
                builder.addHeader("Authorization", "Bearer $token")
            }
            val response = chain.proceed(builder.build())
            val path = original.url.encodedPath
            if (response.code == 401 && !path.endsWith("/auth/login") && !path.endsWith("/auth/register")) {
                onUnauthorized?.invoke()
            }
            response
        }
        .addInterceptor(loggingInterceptor)
        .build()

    val api: ItsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(nullOnEmptyConverterFactory)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ItsApiService::class.java)
    }
}

