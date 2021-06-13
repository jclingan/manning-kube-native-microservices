kubectl exec -it keycloak-0 -- \
    keytool \
        -exportcert \
        -rfc \
        -alias server \
        -keystore /opt/jboss/keycloak/standalone/configuration/application.keystore \
        -storepass password \
        -storetype PKCS12
