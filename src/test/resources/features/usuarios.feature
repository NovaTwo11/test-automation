# language: es
@Usuarios
Característica: Gestión de Usuarios
  Como administrador del sistema
  Quiero gestionar los usuarios de la aplicación
  Para mantener el control de accesos y permisos

  Antecedentes:
    Dado que el servicio de usuarios está disponible
    Y que tengo permisos de administrador

  @Smoke @CrearUsuario
  Escenario: Crear un nuevo usuario con datos válidos
    Dado que tengo los datos válidos de un nuevo usuario
    Cuando envío una solicitud para crear el usuario
    Entonces debo recibir un código de estado 201
    Y el usuario debe ser creado exitosamente
    Y la respuesta debe cumplir con el esquema de usuario
    Y debe contener el ID del usuario creado

  @Validation @CrearUsuario
  Escenario: Intentar crear usuario con email duplicado
    Dado que existe un usuario con email "test@example.com"
    Y que tengo datos de un nuevo usuario con el mismo email
    Cuando envío una solicitud para crear el usuario
    Entonces debo recibir un código de estado 409
    Y debo recibir un mensaje indicando que el email ya existe

  @Validation @CrearUsuario
  Escenario: Intentar crear usuario con email duplicado (simulando username)
    Dado que existe un usuario con email "testuser@test.com"
    Y que tengo datos de un nuevo usuario con el mismo email
    Cuando envío una solicitud para crear el usuario
    Entonces debo recibir un código de estado 409
    Y debo recibir un mensaje indicando que el email ya existe

  @Validation @CrearUsuario
  Esquema del escenario: Validar campos requeridos al crear usuario
    Dado que tengo datos de usuario sin el campo "<campo>"
    Cuando envío una solicitud para crear el usuario
    Entonces debo recibir un código de estado 400
    Y debo recibir un mensaje indicando que el campo "<campo>" es requerido

    Ejemplos:
      | campo    |
      | email    |
      | password |
      | nombre   |

  @Smoke @ListarUsuarios
  Escenario: Listar todos los usuarios
    Dado que existen usuarios en el sistema
    Cuando envío una solicitud para listar todos los usuarios
    Entonces debo recibir un código de estado 200
    Y debo recibir una lista de usuarios
    Y cada usuario debe cumplir con el esquema de usuario

  @Pagination @ListarUsuarios
  Escenario: Listar usuarios con paginación
    Dado que existen múltiples usuarios en el sistema
    Cuando envío una solicitud para listar usuarios con página 0 y tamaño 5
    Entonces debo recibir un código de estado 200
    Y debo recibir máximo 5 usuarios
    Y la respuesta debe contener información de paginación

  @Smoke @ObtenerUsuario
  Escenario: Obtener un usuario por ID
    Dado que existe un usuario en el sistema
    Cuando envío una solicitud para obtener ese usuario por ID
    Entonces debo recibir un código de estado 200
    Y debo recibir los datos del usuario
    Y la respuesta debe cumplir con el esquema de usuario

  @Negative @ObtenerUsuario
  Escenario: Intentar obtener usuario con ID inexistente
    Dado que tengo un ID de usuario que no existe
    Cuando envío una solicitud para obtener ese usuario
    Entonces debo recibir un código de estado 404
    Y debo recibir un mensaje indicando que el usuario no fue encontrado

  @Smoke @ActualizarUsuario
  Escenario: Actualizar datos de un usuario
    Dado que existe un usuario en el sistema
    Y que tengo nuevos datos válidos para actualizar
    Cuando envío una solicitud para actualizar el usuario
    Entonces debo recibir un código de estado 200
    Y el usuario debe ser actualizado con los nuevos datos
    Y la respuesta debe cumplir con el esquema de usuario

  @Validation @ActualizarUsuario
  Escenario: Intentar actualizar usuario con email ya existente
    Dado que existen dos usuarios en el sistema
    Cuando intento actualizar el email del primer usuario con el email del segundo
    Entonces debo recibir un código de estado 409
    Y debo recibir un mensaje indicando que el email ya está en uso

  @Smoke @EliminarUsuario
  Escenario: Eliminar un usuario existente
    Dado que existe un usuario en el sistema
    Cuando envío una solicitud para eliminar ese usuario
    Entonces debo recibir un código de estado 204
    Y el usuario debe ser eliminado del sistema
    Y al intentar obtener el usuario debo recibir 404

  @Negative @EliminarUsuario
  Escenario: Intentar eliminar usuario inexistente
    Dado que tengo un ID de usuario que no existe
    Cuando envío una solicitud para eliminar ese usuario
    Entonces debo recibir un código de estado 404

  @Search @BuscarUsuarios
  Escenario: Buscar usuarios por nombre
    Dado que existen usuarios con nombres similares
    Cuando envío una solicitud para buscar usuarios por nombre "Juan"
    Entonces debo recibir un código de estado 200
    Y debo recibir usuarios que coincidan con el nombre

  @Security @Permissions
  Escenario: Usuario sin permisos no puede crear otros usuarios
    Dado que tengo un token de usuario sin permisos de administrador
    Y que tengo datos de un nuevo usuario
    Cuando envío una solicitud para crear el usuario
    Entonces debo recibir un código de estado 403
    Y debo recibir un mensaje indicando falta de permisos