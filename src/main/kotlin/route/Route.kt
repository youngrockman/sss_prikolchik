package com.example.rout

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.route.DataRepository
import com.example.route.generateJWTToken
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Sneaker(
    val id: Int,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val category: String,
    val isPopular: Boolean = false
)

@Serializable
data class User(
    val userId: Int,
    val userName: String,
    val email: String,
    val password: String,
    val favoriteSneakerIds: List<Int> = emptyList()
)

@Serializable
data class RegistrationResponse(
    val token: String,
    val userId: Int,
    val userName: String,
    val email: String
)

@Serializable
data class CreateUserRequest(
    val userName: String,
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: Int,
    val userName: String,
    val email: String
)

@Serializable
data class ErrorResponse(
    val message: String,
    val errorCode: Int
)

val userList = mutableListOf<User>()

fun Route.authRoute() {
    post("/authorization") {
        try {
            val request = call.receive<LoginRequest>()


            val user = DataRepository.userList.firstOrNull {
                it.email == request.email && it.password == request.password
            } ?: run {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("Invalid email or password", HttpStatusCode.Unauthorized.value)
                )
                return@post
            }
            val token = generateJWTToken(call.application, user)
            call.respond(
                HttpStatusCode.OK,
                AuthResponse(
                    token = token,
                    userId = user.userId,
                    userName = user.userName,
                    email = user.email
                )
            )

        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Invalid request format", HttpStatusCode.BadRequest.value)
            )
        }
    }

    post("/registration") {
        try {
            val request = call.receive<CreateUserRequest>()

            if (userList.any { it.email == request.email }) {
                call.respond(
                    HttpStatusCode.Conflict,
                    ErrorResponse("Email already exists", HttpStatusCode.Conflict.value)
                )
                return@post
            }

            val newUser = User(
                userId = userList.size + 1,
                userName = request.userName,
                email = request.email,
                password = request.password
            )

            userList.add(newUser)
            val token = generateJWTToken(call.application, newUser)

            call.respond(
                HttpStatusCode.Created,
                AuthResponse(
                    token = token,
                    userId = newUser.userId,
                    userName = newUser.userName,
                    email = newUser.email
                )
            )

        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Invalid request format", HttpStatusCode.BadRequest.value)
            )
        }
    }

    authenticate("auth-jwt") {
        get("/profile/{userId}") {
            val userId = call.parameters["userId"]?.toIntOrNull()
            val user = userId?.let { id -> userList.firstOrNull { it.userId == id } }

            if (user != null) {
                call.respond(user)
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("User not found", HttpStatusCode.NotFound.value)
                )
            }
        }
    }
}

fun Route.sneakersRoute() {
    get("/allSneakers") {
        call.respond(DataRepository.sneakerList)
    }

    get("/sneakers/popular") {
        val popularSneakers = DataRepository.sneakerList.filter { it.isPopular }
        call.respond(popularSneakers)
    }

    get("/sneakers/{category}") {
        val category = call.parameters["category"] ?: return@get call.respond(
            HttpStatusCode.BadRequest,
            ErrorResponse("Category parameter is required", 400)
        )

        val filtered = DataRepository.sneakerList.filter {
            it.category.equals(category, ignoreCase = true)
        }
        call.respond(filtered)
    }
}




