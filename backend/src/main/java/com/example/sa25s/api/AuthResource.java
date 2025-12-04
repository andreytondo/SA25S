package com.example.sa25s.api;

import com.example.sa25s.api.dto.*;
import com.example.sa25s.service.AuthService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/register")
    @PermitAll
    @Transactional
    public Response register(@Valid RegisterRequest request) {
        return Response.ok(authService.register(request)).build();
    }

    @POST
    @Path("/login")
    @PermitAll
    @Transactional
    public Response login(@Valid LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Response.ok(response).build();
    }

    @POST
    @Path("/2fa/setup")
    @RolesAllowed("User")
    @Transactional
    public Response setup2fa(@HeaderParam("Authorization") String authHeader) {
        TwoFaSetupResponse setup = authService.setup2fa(authHeader);
        return Response.ok(setup).build();
    }

    @POST
    @Path("/2fa/verify")
    @PermitAll
    @Transactional
    public Response verify(@Valid OtpVerifyRequest request) {
        LoginResponse response = authService.verifyOtp(request);
        return Response.ok(response).build();
    }

    @POST
    @Path("/2fa/disable")
    @RolesAllowed("User")
    @Transactional
    public Response disable(@HeaderParam("Authorization") String authHeader, @Valid Disable2faRequest request) {
        return Response.ok(authService.disable2fa(authHeader, request)).build();
    }
}
