package com.beeftech.farmerregistration

object FarmerRegistrationSession {

    var clientDetails = ClientRegistrationData()

    var addressDetails = AddressAndLocationData()

    fun clear() {
        clientDetails = ClientRegistrationData()
        addressDetails = AddressAndLocationData()
    }
}