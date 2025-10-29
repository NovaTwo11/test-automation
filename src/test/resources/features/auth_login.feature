# language: es
Característica: Autenticación

  Escenario: Login falla con credenciales inválidas
    Cuando intento autenticar con usuario "noexiste@example.com" y password "Xx#123456!"
    Entonces el login debe fallar con 400 o 401

  @api_login
  Escenario: Login exitoso luego de reset de contraseña
    Dado que creo un usuario que resetea su contraseña a "NuevaContraseña123!"
    Cuando hago login en la API con esa contraseña
    Entonces obtengo un token de acceso válido de la API