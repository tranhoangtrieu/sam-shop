# syntax=docker/dockerfile:1

FROM quay.io/keycloak/keycloak:26.6.2 AS builder

WORKDIR /opt/keycloak

# Must match runtime options when using: start --optimized
ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

COPY conf/ /opt/keycloak/conf/
COPY providers/ /opt/keycloak/providers/
COPY themes/ /opt/keycloak/themes/

RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:26.6.2

COPY --from=builder /opt/keycloak/ /opt/keycloak/

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]

# Override in compose: start-dev (local) or start --optimized (with Postgres)
CMD ["start", "--optimized"]
