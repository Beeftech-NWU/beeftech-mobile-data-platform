package com.beeftech.backend.api

import com.beeftech.backend.api.auth.AuthService
import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.auth.authRoutes
import com.beeftech.backend.api.feedcrib.FeedCribService
import com.beeftech.backend.api.feedcrib.feedCribRoutes
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*

fun main() {

    embeddedServer(
        factory = Netty,
        port = 8081,
        host = "0.0.0.0"
    ) {
        module()
    }.start(
        wait = true
    )
}

fun Application.module() {

    val jdbcUrl =
        System.getProperty(
            "beeftech.db.url"
        )
            ?: "jdbc:sqlite:./data/beeftech-backend.db"

    DatabaseFactory.init(
        jdbcUrl
    )

    install(ContentNegotiation) {
        json()
    }

    /*
     * Authentication
     */
    val jwtService =
        JwtService()

    val authService =
        AuthService(
            jwtService
        )

    /*
     * Calf Registration
     */
    val calfRegistrationRepository =
        CalfRegistrationRepository()

    val calfRegistrationService =
        CalfRegistrationService(
            calfRegistrationRepository
        )

    /*
     * Animal Movement
     */
    val animalMovementRepository =
        AnimalMovementRepository()

    val animalMovementService =
        AnimalMovementService(
            animalMovementRepository
        )

    /*
     * Treatment
     */
    val treatmentRepository =
        TreatmentRepository()

    val treatmentService =
        TreatmentService(
            treatmentRepository
        )

    /*
     * Treatment Reference Data
     */
    val treatmentReferenceRepository =
        TreatmentReferenceRepository()

    /*
     * Farmer Registration
     */
    val farmerRepository =
        FarmerRepository()

    val farmerService =
        FarmerService(
            farmerRepository
        )

    /*
     * Feed Crib
     */
    val feedCribService =
        FeedCribService()

    routing {

        /*
         * Authentication routes
         */
        authRoutes(
            authService,
            jwtService
        )

        /*
         * Calf Registration routes
         */
        calfRegistrationRoutes(
            jwtService,
            calfRegistrationService
        )

        /*
         * Animal Movement routes
         */
        animalMovementRoutes(
            jwtService,
            animalMovementService
        )

        /*
         * Treatment routes
         */
        treatmentRoutes(
            jwtService,
            treatmentService,
            treatmentReferenceRepository
        )

        /*
         * Farmer Registration routes
         */
        farmerRoutes(
            jwtService,
            farmerService
        )

        /*
         * Feed Crib routes
         */
        feedCribRoutes(
            jwtService,
            feedCribService
        )

        /*
         * Health check
         */
        get("/") {

            call.respondText(
                "BeefTech Backend API is running"
            )
        }

        get("/api/farm-traceability") {

            call.respondText(
                "Farm Traceability API is running"
            )
        }
    }
}