## cobros-payphone-ws

Backend Spring Boot para integrar cobros PayPhone respetando el contexto de empresa tomado desde token.

### Requisitos previos
- Java 21
- Docker y Docker Compose
- PostgreSQL (solo si desea ejecutar la base fuera de contenedores)

### Variables de entorno principales
- `DB_URL`: url JDBC hacia PostgreSQL.
- `DB_USERNAME`: usuario de base de datos.
- `DB_PASSWORD`: clave de base de datos.
- `SPRING_JPA_HIBERNATE_DDL_AUTO`: estrategia DDL (por defecto `update`).
- `CLAVE_TOKEN_DC`: clave simetrica usada para validar la firma del token JWT.

### Compilacion
```bash
./mvnw clean package -DskipTests
```
> Si no dispone del wrapper puede usar `mvn clean package -DskipTests`.

### Docker
```bash
docker build -t cobros-payphone-ws:latest .
docker compose up -d
```

### Endpoints principales
Todas las rutas se exponen bajo `/cobros-payphone`.
- `POST /cobros-payphone/api/v1/pagos`: registra cobro y devuelve `clientTransactionId`.
- `GET /cobros-payphone/api/v1/pagos/estado/{clientTransactionId}`: consulta estado sincronizado contra PayPhone.
- `POST /cobros-payphone/api/v1/pagos/cancelar/{clientTransactionId}`: solicita cancelacion.
- `POST /cobros-payphone/api/v1/pagos/reversar/{clientTransactionId}`: solicita reverso.

Todas las rutas requieren encabezado `Authorization: Bearer <token>` que incluya `axe_codigo` en el payload del JWT.

### Persistencia
El proyecto asume creacion automatica via JPA (`update`). Para entornos productivos defina migraciones controladas.

### Pruebas
```bash
./mvnw test
```

### Seguridad y auditoria
- Filtro `JwtAuthenticationFilter` valida token y expone `axe_codigo`.
- Servicios usan `AxeCodigoContext` para aislar configuraciones por empresa.
- `transaccion_pago` mantiene request y response completos junto a timestamps.
- Perfiles disponibles: `prue` (logging en DEBUG) y `prod` (logging en INFO), definidos en `src/main/resources/application-prue.yaml` y `src/main/resources/application-prod.yaml`.

### Arquitectura interna
- Paquetes alineados a `net.dualcorp.cobrospayphonews`.
- `modelado`: entidades JPA, enums y DTOs.
- `dao`: acceso Spring Data.
- `servicio`: logica de negocio con manejo de errores mediante try-catch.
- `rest`: controladores REST con respuestas uniformes (`codigo`, `mensaje`, `detalle`).

### Flujo recomendado
1. Generar artefacto via Maven.
2. Empaquetar con Docker multi-stage (Temurin JRE 21).
3. Levantar contenedores `app` y `db` ejecutando `docker compose up -d`.
4. Activar el perfil `prue` o `prod` segun corresponda (`-Dspring.profiles.active=prue` o `prod`).

### Ejemplos de consumo (curl)
Suponga que cuenta con un token JWT valido en la variable `TOKEN`:
```bash
TOKEN="eyJhbGciOi..."
```

Crear cobro (la API ajustara `amount` segun las sumatorias). Los campos `idTransaccion`, `numeroDocumento` e `idCobro` son opcionales para enlazar la factura origen.
```bash
curl -X POST http://localhost:8080/cobros-payphone/api/v1/pagos \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
        "phoneNumber": "0984111222",
        "countryCode": "593",
        "amount": 315,
        "amountWithoutTax": 200,
        "amountWithTax": 100,
        "tax": 15,
        "reference": "Motivo de cobro",
        "idTransaccion": 123,
        "numeroDocumento": "FAC-001",
        "idCobro": 789,
        "order": {
          "billTo": {
            "documentId": "0101010101",
            "email": "cliente@example.com",
            "name": "Cliente Demo"
          },
          "lineItems": [
            { "item": "Servicio", "quantity": 1, "amount": 315 }
          ]
        },
        "responseUrl": "https://tu-dominio.com/webhook"
      }'
```

Consultar estado de un cobro:
```bash
curl -X GET "http://localhost:8080/cobros-payphone/api/v1/pagos/estado/{clientTransactionId}" \
  -H "Authorization: Bearer ${TOKEN}"
```

Cancelar un cobro pendiente:
```bash
curl -X POST "http://localhost:8080/cobros-payphone/api/v1/pagos/cancelar/{clientTransactionId}" \
  -H "Authorization: Bearer ${TOKEN}"
```

### Notas relevantes
- Se ajusta `amount` automaticamente para mantener consistencia con sumatorias PayPhone.
- Limite de consultas por minuto: 30 por empresa para evitar bloqueos remotos.
- Registros de auditoria conservan `axe_codigo`, `clientTransactionId` y `transactionIdPayphone`.
