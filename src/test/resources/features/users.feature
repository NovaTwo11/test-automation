# language: es
Característica: Gestión de usuarios

  Escenario: Listar usuarios con token válido
    Dado que tengo un token válido de administración
    Cuando listo los usuarios
    Entonces obtengo un listado de usuarios con estado 200