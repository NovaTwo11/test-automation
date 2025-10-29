# language: es
Característica: Reseteo de contraseña

  Escenario: Usuario solicita y confirma reseteo de contraseña exitosamente
    Dado que existe un usuario con email "testuser@example.com"
    Cuando solicito el reseteo de contraseña para ese usuario
    Y cambio la contraseña a "NuevaContraseña123!" usando el token de reseteo
    Entonces el reseteo de contraseña debe ser exitoso