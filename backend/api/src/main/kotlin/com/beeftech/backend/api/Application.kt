package com.beeftech.backend.api

import com.beeftech.backend.api.auth.AuthService
import com.beeftech.backend.api.auth.DevUserSeeder
import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.auth.UserRepository
import com.beeftech.backend.api.auth.UserAdminService
import com.beeftech.backend.api.auth.authRoutes
import com.beeftech.backend.api.auth.userAdminRoutes
import com.beeftech.backend.api.feedcrib.FeedCribRepository
import com.beeftech.backend.api.feedcrib.FeedCribService
import com.beeftech.backend.api.feedcrib.feedCribRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking

fun main() {

    val port =
        System.getenv("PORT")
            ?.toIntOrNull()
            ?: 8081

    embeddedServer(
        factory = Netty,
        port = port,
        host = "0.0.0.0"
    ) {
        module()
    }.start(
        wait = true
    )
}

fun Application.module() {

    val jdbcUrl =
        System.getenv(
            "BEEFTECH_DB_URL"
        )
            ?.trim()
            ?.takeIf {
                it.isNotBlank()
            }
            ?: System.getProperty(
                "beeftech.db.url"
            )
            ?: "jdbc:sqlite:./data/beeftech-backend.db"

    DatabaseFactory.init(
        jdbcUrl
    )

    install(ContentNegotiation) {
        json()
    }

    val userRepository = UserRepository()
    val jwtService = JwtService()
    val authService = AuthService(userRepository, jwtService)
    val userAdminService = UserAdminService(userRepository)

    val seedDevUsers =
        System.getenv(
            "BEEFTECH_SEED_DEV"
        )
            ?.equals(
                "true",
                ignoreCase = true
            )
            ?: (
                System.getProperty(
                    "beeftech.seed.dev"
                ) == "true"
            )

    if (seedDevUsers) {
        runBlocking {
            DevUserSeeder.seed(userRepository)
        }
    }

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

    val farmerSalesNotificationService =
        createFarmerSalesNotificationServiceFromEnvironment()

    val farmerService =
        FarmerService(
            repository = farmerRepository,
            salesNotificationService =
                farmerSalesNotificationService
        )

    /*
     * Feed Crib
     */
    val feedCribService =
        FeedCribService(FeedCribRepository())

    routing {

        get("/health") {
            call.respondText("OK")
        }

        /*
         * Authentication routes
         */
        authRoutes(
            authService,
            jwtService
        )

        userAdminRoutes(
            jwtService,
            userAdminService
        )

        mortalityRoutes(
            jwtService,
            MortalityService(MortalityRepository())
        )

        voidRoutes(
            jwtService,
            VoidService(UserRepository(), VoidRepository())
        )

        auditRoutes(
            jwtService,
            AuditService(UserRepository(), AuditRepository())
        )

        costRoutes(
            jwtService,
            CostService(CostRepository())
        )

        dashboardRoutes(
            jwtService,
            DashboardService()
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
