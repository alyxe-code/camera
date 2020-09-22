package com.p2lem8dev.testcameraapp

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.p2lem8dev.testcameraapp.common.livenavigation.LiveNavigation
import com.p2lem8dev.testcameraapp.common.livenavigation.LiveNavigationImpl

class MainViewModel : ViewModel() {

    private var _permission: Permission = Permission(camera = false, storage = false)
        set(value) {
            field = value
            (permissions as MutableLiveData).postValue(value)
        }
    val permissions: LiveData<Permission> = MutableLiveData(_permission)

    private val _navigation = LiveNavigationImpl<Navigation>()
    val navigation: LiveNavigation<Navigation> = _navigation

    init {
        permissions.observeForever(this::onPermissionsChanged)
    }

    override fun onCleared() {
        permissions.removeObserver(this::onPermissionsChanged)
        super.onCleared()
    }

    private fun onPermissionsChanged(permission: Permission?) {
        permission ?: return
        when {
            !permission.camera -> _navigation.call { requestPermissionCamera() }
            !permission.storage -> _navigation.call { requestPermissionStorage() }
            permission.camera && permission.storage -> _navigation.call { startCamera() }
        }
    }

    fun onPermissionReceived(
        camera: Boolean? = null,
        storage: Boolean? = null
    ) {
        _permission = Permission(
            camera = camera ?: _permission.camera,
            storage = storage ?: _permission.storage
        )
    }

    fun onTakePictureRequested() = _navigation.call { takePicture() }

    fun onTakePictureSucceeded(uri: Uri) = Unit

    fun onTakePictureFailure(t: Throwable) = Unit


    class Permission(
        val camera: Boolean,
        val storage: Boolean,
    )

    interface Navigation {
        fun requestPermissionCamera()
        fun requestPermissionStorage()
        fun startCamera()
        fun takePicture()
    }
}