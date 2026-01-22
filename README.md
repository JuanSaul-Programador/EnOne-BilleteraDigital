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

Este proyecto es parte del portafolio profesional de **Juan Saul**. Si deseas contribuir o reportar un bug, por favor abre un Issue en el repositorio.

---
© 2025 EnOne Fintech. Todos los derechos reservados.
