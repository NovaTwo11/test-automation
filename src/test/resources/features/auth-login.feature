# language: es
@AuthLogin
Característica: Autenticación y Login
  Como usuario del sistema
  Quiero poder autenticarme con mis credenciales
  Para acceder a los recursos protegidos de la API

  Antecedentes:
    Dado que el servicio de autenticación está disponible

  @Smoke @Login
  Escenario: Login exitoso con credenciales válidas
    Dado que tengo un usuario registrado con credenciales válidas
    Cuando envío una solicitud de login con las credenciales correctas
    Entonces debo recibir un código de estado 200
    Y debo recibir un token de acceso válido
    Y el token debe contener la información del usuario

  @Negative @Login
  Escenario: Login fallido con credenciales inválidas
    Dado que tengo credenciales inválidas
    Cuando envío una solicitud de login con credenciales incorrectas
    Entonces debo recibir un código de estado 401
    Y no debo recibir un token de acceso

  @Negative @Login
  Escenario: Login fallido con usuario inexistente
    Dado que tengo un usuario que no existe en el sistema
    Cuando envío una solicitud de login con ese usuario
    Entonces debo recibir un código de estado 401
    Y debo recibir un mensaje de error indicando credenciales inválidas

  @Security @Token
  Escenario: Acceso a recurso protegido sin token
    Dado que no tengo un token de autenticación
    Cuando intento acceder a un recurso protegido sin token
    Entonces debo recibir un código de estado 401
    Y debo recibir un mensaje indicando falta de autenticación

  @Security @Token
  Escenario: Acceso a recurso protegido con token inválido
    Dado que tengo un token de autenticación inválido
    Cuando intento acceder a un recurso protegido con el token inválido
    Entonces debo recibir un código de estado 401
    Y debo recibir un mensaje indicando token inválido

  @Security @Token
  Escenario: Acceso exitoso a recurso protegido con token válido
    Dado que tengo un token de autenticación válido
    Cuando accedo a un recurso protegido con el token válido
    Entonces debo recibir un código de estado 200
    Y debo recibir la información del recurso solicitado

  @Token @Refresh
  Escenario: Validación de expiración de token
    Dado que tengo un token de autenticación válido
    Cuando el token está próximo a expirar
    Entonces puedo verificar el tiempo de expiración en el token
    Y el token debe contener la información de expiración correcta