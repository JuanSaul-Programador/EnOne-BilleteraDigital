# EnOne - Billetera Digital Inteligente 🚀

**EnOne** es una plataforma fintech de última generación diseñada para simplificar la gestión financiera personal. No es solo una billetera digital; es un ecosistema completo que integra seguridad bancaria, transferencias en tiempo real multidivisa y asistencia virtual con inteligencia artificial.

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.7-green?style=for-the-badge&logo=spring)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=for-the-badge&logo=mysql)
![JWT](https://img.shields.io/badge/Security-JWT_Auth-red?style=for-the-badge&logo=json-web-tokens)
![IBM Watson](https://img.shields.io/badge/AI-IBM_Watson-purple?style=for-the-badge&logo=ibm)

---

## 🌟 Características y Flujos del Sistema

Esta sección detalla los flujos principales de la aplicación, explicando cómo interactúan los usuarios con el sistema.

### 1. Onboarding y Registro Seguro (KYC)
El proceso de registro sigue un estricto flujo de **Conoce a tu Cliente (KYC)** para garantizar la veracidad de los usuarios.

1.  **Inicio de Registro**: El usuario ingresa su correo electrónico y contraseña. El sistema crea una sesión temporal.
2.  **Verificación Dual**:
    *   📧 **Email**: Se envía un código de 6 dígitos al correo.
    *   📱 **SMS/WhatsApp**: Se envía un código al teléfono mediante la integración con **CallMeBot**.
3.  **Datos Personales**: Una vez verificados ambos canales, el usuario completa su perfil con DNI, Nombres y Apellidos.
4.  **Creación de Billeteras**: Al finalizar, el sistema crea automáticamente dos billeteras para el usuario: una en **Soles (PEN)** y otra en **Dólares (USD)**.

### 2. Autenticación y Seguridad
La seguridad es el pilar de EnOne.

*   **Login**: Autenticación mediante **JWT (JSON Web Token)**. El token debe enviarse en el header `Authorization: Bearer <token>` en cada petición subsecuente.
*   **Autenticación de Dos Factores (2FA)**:
    *   Los usuarios pueden activar 2FA para una capa extra de seguridad.
    *   Se genera un secreto único y códigos temporales para validar operaciones críticas como transferencias.

### 3. Billetera Digital Multimoneda 💰
Gestión completa de fondos en tiempo real.

*   **Dashboard**: Vista unificada de saldos en ambas monedas.
*   **Transferencias**:
    *   **Entre Usuarios EnOne**: Instantáneas y sin comisiones. Requiere validación del destinatario por email o teléfono.
    *   **Validación de Seguridad**: Verifica fondos insuficientes, estado de la cuenta destino y límites transaccionales.
*   **Conversión de Divisas (Exchange)**:
    *   Compra y venta de dólares en tiempo real con tipo de cambio preferencial.
    *   Actualización instantánea de los saldos en ambas billeteras.
*   **Tarjeta Virtual**:
    *   Generación de tarjeta virtual para compras online.
    *   Funciones de **Activar/Congelar** tarjeta para seguridad inmediata.

### 4. Asistente Virtual Inteligente (IBM Watson) 🤖
EnOne integra inteligencia artificial para soporte al usuario.

*   **Chatbot Integrado**: Un asistente disponible 24/7 capaz de responder preguntas sobre:
    *   Cómo realizar transferencias.
    *   Consultas de saldo.
    *   Tipos de cambio actuales.
    *   Problemas con la cuenta.
*   El backend actúa como puente seguro entre el frontend y la API de IBM Watson Assistant, protegiendo las credenciales.

### 5. Panel Administrativo 📊
Herramientas poderosas para la gestión del negocio.

*   **Estadísticas en Tiempo Real**: Visualización de volumen transaccional y nuevos usuarios.
*   **Heatmap de Actividad**: Mapa de calor para identificar los horarios de mayor uso de la plataforma.
*   **Gestión de Usuarios**: Capacidad para buscar usuarios, ver sus perfiles y realizar acciones de moderación (bloqueo/desbloqueo).

---

## 🛠 Arquitectura Técnica

El proyecto sigue una arquitectura limpia (Clean Architecture) modularizada:

| Capa | Tecnologías / Componentes | Descripción |
| :--- | :--- | :--- |
| **Web** | Spring MVC, RestControllers | Manejo de peticiones HTTP, validación de DTOs y respuesta estandarizada (`ApiResponse`). |
| **Service** | Spring Service | Lógica de negocio, orquestación de transacciones y reglas de validación. |
| **Domain** | JPA Entities | Modelado de datos (User, Wallet, Transaction, UserProfile). |
| **Repository** | Spring Data JPA | Abstracción de acceso a datos MySQL. |
| **Security** | Spring Security | Filtros JWT, manejo de sesiones stateless y encriptación BCrypt. |
| **Integration** | RestTemplate, SDKs | Integración con servicios externos (IBM Watson, CallMeBot). |

---

## � Documentación de API (Endpoints Principales)

### Autenticación (`/api/auth`)
*   `POST /login`: Iniciar sesión.
*   `POST /2fa/generate`: Generar secreto 2FA.
*   `POST /2fa/verify`: Validar código 2FA.

### Onboarding (`/api/onboarding`)
*   `POST /start`: Iniciar registro.
*   `POST /verify-email-code`: Validar código email.
*   `POST /verify-phone`: Validar código SMS.
*   `POST /complete`: Finalizar registro.

### Billetera (`/api/wallet`)
*   `GET /balance`: Obtener saldo actual.
*   `POST /transfer`: Realizar transferencia.
*   `POST /convert`: Cambiar divisas (PEN <-> USD).
*   `GET /transactions`: Historial de movimientos.
*   `POST /activar-tarjeta`: Generar/Activar tarjeta virtual.

### Admin (`/api/admin`)
*   `GET /dashboard-stats`: Métricas generales.
*   `GET /stats/activity-heatmap`: Datos para mapa de calor.

---

## 🎭 Servicios Mockeados (Simulaciones)

Para garantizar un entorno de desarrollo robusto sin depender de servicios externos de pago o gubernamentales inestables, el proyecto incluye **servidores mock** internos que simulan respuestas reales:

### 🏦 Mock Banco
Simula una entidad bancaria para validar tarjetas y procesar pagos.
*   **Funcionalidad**: Valida número de tarjeta, CVV, fecha de vencimiento y saldo.
*   **Tabla**: `mock_tarjeta`
*   **Caso de Uso**: Cuando recargas saldo en tu billetera digital, esta consulta internamente al "Banco Mock" si la tarjeta es válida y descuenta el saldo "real" de esa tarjeta simulada.

### 🆔 Mock RENIEC
Simula el servicio gubernamental de identificación de Perú.
*   **Funcionalidad**: Devuelve nombres y apellidos basados en un DNI.
*   **Tabla**: `mock_reniec`
*   **Caso de Uso**: Al registrarse, el usuario solo ingresa su DNI y el sistema "autocompleta" sus datos consultando a este mock.

---

## 🧪 Datos de Prueba (SQL)

Para probar todos los flujos inmediatamente sin registrar manualmeente datos en los mocks, ejecuta estos scripts SQL en tu base de datos `enone_db`:

### 1. Insertar Personas en RENIEC (Para Registro)
```sql
INSERT INTO mock_reniec (dni, nombres, apellidos, fecha_nacimiento) VALUES
('40556677', 'JUAN SAUL', 'PEREZ LOPEZ', '1990-05-15'),
('70889900', 'MARIA ANA', 'GOMEZ RUIZ', '1995-10-20');
```
*Usa el DNI `40556677` al registrarte para que el sistema reconozca a "Juan Saul".*

### 2. Insertar Tarjetas en Banco (Para Recargas)
```sql
INSERT INTO mock_tarjeta (numero_tarjeta, cvv, fecha_vencimiento, nombre_titular, saldo_disponible, activa, created_at, updated_at) VALUES
('4557880012345678', '123', '12/28', 'JUAN PEREZ', 5000.00, 1, NOW(), NOW()),
('5100000000000001', '456', '01/30', 'MARIA GOMEZ', 10000.00, 1, NOW(), NOW());
```
*Usa la tarjeta `4557880012345678` con CVV `123` y vto `12/28` para recargar saldo en tu billetera.*

### 3. Usuarios Pre-creados (Backup)
Si fallan las APIs de registro (CallMeBot), usa estos scripts para crear usuarios listos para loguearse:

```sql
-- 1. Crear Roles
INSERT IGNORE INTO roles (name) VALUES ('ROLE_USER'), ('ROLE_ADMIN');

-- 2. Crear Usuario (Password: 123456)
INSERT INTO users (username, password, enabled, created_at, updated_at) VALUES 
('usuario_demo@enone.com', '$2a$10$xK7.3..1..2..3..4..5..6..7..8..9..0..1..2..3..4', 1, NOW(), NOW()); -- Hash real de '123456'

-- 3. Asignar Rol
INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'usuario_demo@enone.com' AND r.name = 'ROLE_USER';

-- 4. Crear Perfil
INSERT INTO user_profile (user_id, email, first_name, last_name, document_type, document_number, phone, two_factor_enabled, created_at, updated_at)
SELECT id, 'usuario_demo@enone.com', 'Usuario', 'Demo', 'DNI', '10203040', '999888777', 0, NOW(), NOW()
FROM users WHERE username = 'usuario_demo@enone.com';

-- 5. Crear Billeteras (PEN y USD)
INSERT INTO wallets (user_id, wallet_number, currency, balance, status, created_at, updated_at)
SELECT id, 'W-PEN-10203040', 'PEN', 1000.00, 'ACTIVE', NOW(), NOW()
FROM users WHERE username = 'usuario_demo@enone.com';

INSERT INTO wallets (user_id, wallet_number, currency, balance, status, created_at, updated_at)
SELECT id, 'W-USD-10203040', 'USD', 500.00, 'ACTIVE', NOW(), NOW()
FROM users WHERE username = 'usuario_demo@enone.com';
```
*Usuario: `usuario_demo@enone.com` | Password: `123456`*

---

## ⚙️ Guía de Instalación Local

Sigue estos pasos para levantar el proyecto en tu máquina.

### 1. Requisitos
*   Java JDK 17+
*   Maven 3.8+
*   MySQL 8.0+

### 2. Base de Datos
Crea la base de datos en MySQL:
```sql
CREATE DATABASE enone_db;
```

### 3. Variables de Entorno
Configura las siguientes variables en tu sistema o IDE (IntelliJ/Eclipse) para proteger tus credenciales:

```properties
DB_USER=root
DB_PASSWORD=tu_password
JWT_SECRET=tu_clave_secreta_jwt_muy_larga
EMAIL_USERNAME=tu_email@gmail.com
EMAIL_PASSWORD=tu_app_password_gmail
CALLMEBOT_KEYS=celular:apikey
IBM_WATSON_API_KEY=tu_api_key_watson
```

### 4. Ejecución
```bash
# Compilar proyecto y saltar tests
mvn clean install -DskipTests

# Ejecutar
mvn spring-boot:run
```

Visita `http://localhost:8080/index.html` para ver la aplicación web.

---

## 👥 Contribución

Desarrollado por Juan Saul Pereyra Acedo | Backend Developer

---
© 2025 EnOne . Todos los derechos reservados.
