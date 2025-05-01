package io.bidcast.app.utils

import android.Manifest
import android.os.Build

object Const {

	const val BASE_URL = ""
	
	//NOTIFICATION CONST
	const val CHANNEL_NAME = "Base Project"
	const val CHANNEL_ID = "base_project"
	
	const val SERVER_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
	const val DD_MMMM_YYYY = "dd MMMM yyyy"
	
	//APP PERMISSIONS
	private val COMMON_PERMS = arrayOf(
		Manifest.permission.INTERNET,
		Manifest.permission.CAMERA,
	)
	
	val VERSION_PERMS = when {
		Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
			Manifest.permission.POST_NOTIFICATIONS,
		)
		
		else -> arrayOf()
	}
	
	val STR_PERMS = when {
		Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
			Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
			Manifest.permission.READ_MEDIA_IMAGES
		)
		
		Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
			Manifest.permission.READ_MEDIA_VIDEO,
			Manifest.permission.READ_MEDIA_IMAGES
		)
		
		Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> arrayOf(
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE
		
		)
		
		else -> arrayOf(
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE
		)
	}
	
	val LOC_PERMS = arrayOf(
		Manifest.permission.ACCESS_FINE_LOCATION,
		Manifest.permission.ACCESS_COARSE_LOCATION,
	)
	
	val CAMERA_PERMS = arrayOf(
		Manifest.permission.CAMERA,
	)
	
	val PERMISSIONS = COMMON_PERMS + VERSION_PERMS + STR_PERMS
	
}
