package com.construccion.marketplace.di

import com.construccion.marketplace.data.api.AppConfig
import com.construccion.marketplace.data.api.OdooApiService
import com.construccion.marketplace.session.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * CookieJar en memoria — persiste las cookies de sesión de Odoo
 * (session_id) entre peticiones dentro de la misma ejecución de la app.
 */
class InMemoryCookieJar : CookieJar {
    private val store = mutableMapOf<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        store.getOrPut(host) { mutableListOf() }.apply {
            cookies.forEach { newCookie ->
                removeAll { it.name == newCookie.name }
                add(newCookie)
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        store[url.host] ?: emptyList()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideCookieJar(): InMemoryCookieJar = InMemoryCookieJar()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cookieJar: InMemoryCookieJar,
        sessionManager: SessionManager
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(AppConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(AppConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // Interceptor: añade session_id de Odoo como header Y cookie en todas las peticiones.
            // Odoo auth="user" solo acepta la sesión vía cookie "session_id".
            // Sin esto, al reiniciar la app el InMemoryCookieJar está vacío y Odoo
            // devuelve HTML en lugar de JSON → IllegalStateException en Gson.
            .addInterceptor { chain ->
                val sessionId = sessionManager.getSessionId()
                val reqBuilder = chain.request().newBuilder()
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                if (sessionId.isNotBlank()) {
                    reqBuilder
                        .addHeader("X-Openerp-Session-Id", sessionId)
                        // Forzar cookie de sesión para que auth="user" funcione
                        .addHeader("Cookie", "session_id=$sessionId")
                }
                chain.proceed(reqBuilder.build())
            }

        if (AppConfig.ENABLE_HTTP_LOGS) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(AppConfig.BASE_URL + "/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
            .build()

    @Provides
    @Singleton
    fun provideOdooApiService(retrofit: Retrofit): OdooApiService =
        retrofit.create(OdooApiService::class.java)
}
