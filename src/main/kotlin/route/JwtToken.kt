package com.example.route

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.rout.User
import io.ktor.server.application.*
import java.util.*

public fun generateJWTToken(application: Application, user: User): String {
    val config = application.environment.config
    return JWT.create()
        .withAudience(config.property("jwt.audience").getString())
        .withIssuer(config.property("jwt.issuer").getString())
        .withClaim("userId", user.userId)
        .withClaim("userName", user.userName)
        .withClaim("email", user.email)
        .withExpiresAt(Date(System.currentTimeMillis() + 60000))
        .sign(Algorithm.HMAC256(config.property("jwt.secret").getString()))
}