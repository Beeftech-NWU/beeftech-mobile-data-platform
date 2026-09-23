package com.beeftech.authentication.fakes

import com.beeftech.authentication.data.DeviceIdProvider

class FakeDeviceIdProvider(private val deviceId: String = "TEST_DEVICE_123") : DeviceIdProvider {
    override fun getDeviceId(): String {
        return deviceId
    }
}
