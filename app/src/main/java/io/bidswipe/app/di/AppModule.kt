package io.bidswipe.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.bidswipe.app.network.ApiInterface
import io.bidswipe.app.network.RetrofitService
import io.bidswipe.app.network.repository.AuthRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideRetrofit(@ApplicationContext mCtx: Context): ApiInterface =
        RetrofitService(mCtx).build()

    @Singleton
    @Provides
    fun provideAppRepository(api: ApiInterface): AuthRepository = AuthRepository(api)

}