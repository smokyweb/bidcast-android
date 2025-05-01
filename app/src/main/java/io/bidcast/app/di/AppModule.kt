package io.bidcast.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.bidcast.app.network.ApiInterface
import io.bidcast.app.network.RetrofitService
import io.bidcast.app.network.repository.AuthRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

	@Singleton
	@Provides
	fun provideRetrofit(@ApplicationContext mCtx : Context) : ApiInterface = RetrofitService(mCtx).build()

	@Singleton
	@Provides
	fun provideAppRepository(api : ApiInterface) : AuthRepository = AuthRepository(api)

}