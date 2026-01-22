# EnOne - Billetera Digital Inteligente

EnOne es una plataforma financiera digital completa que permite a los usuarios gestionar múltiples billeteras (PEN/USD), realizar transferencias en tiempo real, recargas, y consultas, todo respaldado por una arquitectura segura de microservicios con Spring Boot y una interfaz moderna.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.7-green)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Security](https://img.shields.io/badge/Spring_Security-JWT-red)

## 🚀 Características Principales

### 🔐 Seguridad y Autenticación
- **JWT (JSON Web Tokens)**: Implementación robusta para sesiones sin estado.
- **Autenticación de Dos Factores (2FA)**: Protección adicional para operaciones sensibles.
- **Verificación KYC**: Flujo de "Know Your Customer" para validación de identidad.
- **BCrypt Hashing**: Almacenamiento seguro de contraseñas.

### 💰 Gestión Financiera
- **Múltiples Billeteras**: Soporte nativo para Soles (PEN) y Dólares (USD).
- **Transferencias en Tiempo Real**: Envíos instantáneos entre usuarios de la plataforma o interbancarios.
- **Conversión de Divisas**: Tipo de cambio actualizado para operaciones inter-moneda.
- **Historial Transaccional**: Registro detallado de ingresos y egresos con generación de comprobantes.

### 🤖 Asistencia Inteligente
- **Chatbot IBM Watson**: Asistente virtual integrado para resolver dudas frecuentes y guiar al usuario 24/7.
- **Integración WhatsApp (CallMeBot)**: Notificaciones y verificaciones enviadas directamente al WhatsApp del usuario.

### 🛠 Panel Administrativo
- **Gestión de Usuarios**: Dashboard para administradores para ver, bloquear o moderar usuarios.
- **Monitoreo de Transacciones**: Vista global de las operaciones en la plataforma.

## 🏗 Arquitectura y Tecnologías

El proyecto sigue una arquitectura en capas, separando claramente responsabilidades:

- **Backend**: Java 17, Spring Boot 3.5.7
  - *Spring Data JPA*: Persistencia de datos.
  - *Spring Security*: Control de acceso y roles (USER, ADMIN).
  - *Spring Mail*: Envío de correos electrónicos transaccionales.
- **Base de Datos**: MySQL
- **Frontend**: HTML5, CSS3, JavaScript (Vanilla ES6+)
  - Diseño responsivo y moderno.
  - Consumo de API RESTful mediante Fetch API.
- **Integraciones Externas**:
  - IBM Watson Assistant
  - CallMeBot API

## 📋 Requisitos Previos

- Java Development Kit (JDK) 17 o superior.
- Maven 3.8+.
- MySQL Server 8.0+.
- Cuenta en IBM Cloud (para Watson, opcional si se deshabilita).

## ⚙️ Configuración e Instalación

1.  **Clonar el repositorio**
    ```bash
    git clone https://github.com/JuanSaul-Programador/enone-spring.git
    cd enone-spring
    ```

2.  **Configurar Base de Datos**
    Crea una base de datos MySQL llamada `enone_db`:
    ```sql
    CREATE DATABASE enone_db;
    ```
    *Nota: Las tablas se crearán automáticamente gracias a `hibernate.ddl-auto: update`.*

3.  **Configurar Variables de Entorno**
    Por seguridad, este proyecto utiliza variables de entorno para las credenciales. Debes configurarlas en tu IDE o sistema operativo:

    | Variable | Descripción | Ejemplo |
    | :--- | :--- | :--- |
    | `DB_USER` | Usuario de MySQL | `root` |
    | `DB_PASSWORD` | Contraseña de MySQL | `123456` |
    | `JWT_SECRET` | Clave secreta para firmar tokens | `una_clave_muy_segura_y_larga` |
    | `EMAIL_USERNAME` | Correo remitente (Gmail) | `tu_correo@gmail.com` |
    | `EMAIL_PASSWORD` | App Password de Gmail | `abcd efgh ijkl mnop` |
    | `CALLMEBOT_KEYS` | API Keys para WhatsApp | `celular:apikey` |
    | `IBM_WATSON_API_KEY` | API Key de IBM Watson | `tu_ibm_api_key` |

4.  **Ejecutar la Aplicación**
    ```bash
    mvn spring-boot:run
    ```

5.  **Acceso**
    La aplicación estará disponible en `http://localhost:8080`.
    - **Página de inicio**: `/index.html`
    - **Login**: `/login.html`
    - **Documentación API**: Consultar código fuente en `controllers`.

## 📂 Estructura del Proyecto

```
src/main
├── java/com/enone
│   ├── config/      # Configuraciones (Seguridad, CORS)
│   ├── controller/  # Controladores REST
│   ├── domain/      # Entidades y Repositorios
│   ├── service/     # Lógica de Negocio
│   ├── security/    # Filtros y Utilidades JWT
│   └── util/        # Utilidades (SMS, Validaciones)
└── resources
    ├── static/      # Frontend (HTML, CSS, JS)
    │   ├── assets/  # Recursos estáticos
    │   └── *.html   # Páginas de la aplicación
    └── application.yml # Configuración principal
```

## 🤝 Contribución

Las contribuciones son bienvenidas. Por favor, abre un issue para discutir cambios mayores antes de enviar un Pull Request.

## 📄 Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para más detalles.

---
Desarrollado con ❤️ por [Juan Saul](https://github.com/JuanSaul-Programador)
