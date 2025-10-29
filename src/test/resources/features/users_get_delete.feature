# language: es
Característica: Consulta y eliminación de usuario

  @min_crud
  Escenario: Consultar usuario inexistente devuelve 404
    Cuando consulto el usuario por id "00000000-0000-0000-0000-000000000000"
    Entonces obtengo estado 404

  @min_crud
  Escenario: Eliminar usuario existente devuelve 200 y luego 404 al consultarlo
    Dado que genero un usuario aleatorio válido
    Y creo el usuario vía API
    Y extraigo y guardo el id del usuario desde la última respuesta
    Cuando elimino el usuario por su id
    Entonces obtengo estado 200
    Y al consultar nuevamente el usuario por su id obtengo 404