# language: es
@Password
Característica: Gestión de Contraseñas
  Como usuario del sistema
  Quiero poder cambiar y recuperar mi contraseña
  Para mantener la seguridad de mi cuenta

  Antecedentes:
    Dado que el servicio de gestión de contraseñas está disponible

  @Smoke @CambiarPassword
  Escenario: Cambiar contraseña con credenciales válidas
    Dado que creo un usuario temporal para la prueba de contraseña
    Y que inicio sesión como el usuario temporal para obtener su token
    Y que tengo la contraseña original del usuario temporal
    Y que tengo una nueva contraseña válida para el usuario temporal
    Cuando envío una solicitud para cambiar la contraseña con el token del usuario temporal
    Entonces debo recibir un código de estado 200
    Y debo recibir un mensaje de confirmación
    Y el usuario temporal puede iniciar sesión con la nueva contraseña

  @Negative @CambiarPassword
  Escenario: Intentar cambiar contraseña con contraseña actual incorrecta
    Dado que creo un usuario temporal para la prueba de contraseña
    Y que inicio sesión como el usuario temporal para obtener su token
    Y que proporciono una contraseña actual incorrecta para el usuario temporal
    Y que tengo una nueva contraseña válida para el usuario temporal
    Cuando envío una solicitud para cambiar la contraseña con el token del usuario temporal
    Entonces debo recibir un código de estado 400
    Y debo recibir un mensaje indicando contraseña actual incorrecta