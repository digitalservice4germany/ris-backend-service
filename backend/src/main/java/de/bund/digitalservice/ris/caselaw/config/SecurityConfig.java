package de.bund.digitalservice.ris.caselaw.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import org.springframework.security.web.server.header.XContentTypeOptionsServerHttpHeadersWriter;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity(useAuthorizationManager = true) // enables @PreAuthorize to work
public class SecurityConfig {

  @Value("${OAUTH2_CLIENT_ISSUER:https://neuris.login.bare.id/auth/realms/development}")
  String issuerUri;

  @Bean
  public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
    return http.authorizeExchange(
            exchanges ->
                exchanges
                    .pathMatchers(
                        "/actuator/**",
                        "/api/v1/open/norms/**",
                        "/admin/webhook",
                        "/api/webjars/**",
                        "/api/docs.*/**")
                    .permitAll()
                    .anyExchange()
                    .authenticated())
        .oauth2Login(Customizer.withDefaults())
        .exceptionHandling(
            handling ->
                handling.authenticationEntryPoint(
                    new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)))
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(
                        customizer ->
                            customizer.policyDirectives(
                                "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-eval'; connect-src 'self' *.sentry.io data:"))
                    .writer(new XContentTypeOptionsServerHttpHeadersWriter())
                    .frameOptions(
                        frameOptions ->
                            frameOptions.mode(XFrameOptionsServerHttpHeadersWriter.Mode.SAMEORIGIN))
                    .referrerPolicy(
                        referrerPolicySpec ->
                            referrerPolicySpec.policy(
                                ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy
                                    .STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    .permissionsPolicy(
                        permissionsPolicySpec ->
                            permissionsPolicySpec.policy(
                                "accelerometer=(), ambient-light-sensor=(), autoplay=(), battery=(), camera=(), cross-origin-isolated=(), "
                                    + "display-capture=(), document-domain=(), encrypted-media=(), execution-while-not-rendered=(), "
                                    + "execution-while-out-of-viewport=(), fullscreen=(), geolocation=(), gyroscope=(), keyboard-map=(), "
                                    + "magnetometer=(), microphone=(), midi=(), navigation-override=(), payment=(), picture-in-picture=(), "
                                    + "publickey-credentials-get=(), screen-wake-lock=(), sync-xhr=(), usb=(), web-share=(), xr-spatial-tracking=(), "
                                    + "clipboard-read=(self), clipboard-write=(self), gamepad=(), speaker-selection=(), conversion-measurement=(), "
                                    + "focus-without-user-activation=(self), hid=(), idle-detection=(), interest-cohort=(), serial=(), sync-script=(), "
                                    + "trust-token-redemption=(), window-placement=(), vertical-scroll=(self)")))
        .oauth2ResourceServer(jwtCustomizer -> jwtCustomizer.jwt(Customizer.withDefaults()))
        .build();
  }

  @Bean
  public ReactiveJwtDecoder jwtDecoder() {
    return ReactiveJwtDecoders.fromIssuerLocation(issuerUri);
  }
}
