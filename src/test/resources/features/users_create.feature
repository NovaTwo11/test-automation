# language: es
Característica: Creación de usuarios

  @creacion_201
  Escenario: Crear usuario nuevo exitosamente
    Dado que genero un usuario aleatorio válido
    Cuando creo el usuario vía API
    Entonces la creación responde 201 y cumple el esquema de usuario creado

  @creacion_409
  Escenario: Intentar crear usuario duplicado
    Dado que genero un usuario aleatorio válido
    Y creo el usuario vía API (idempotente)
    Cuando intento crear el mismo usuario de nuevo
    Entonces la creación responde 409 conflicto