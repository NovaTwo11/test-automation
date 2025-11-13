# language: es
@Perfiles
Característica: Gestión de Perfiles de Usuario
  Como usuario autenticado del sistema
  Quiero gestionar mi perfil personalizado
  Para mantener actualizada mi información personal

  Antecedentes:
    Dado que el servicio de perfiles está disponible
    Y que tengo un usuario autenticado en el sistema

  @Smoke @CrearPerfil
  Escenario: Crear un perfil completo para un usuario
    Dado que tengo datos válidos de un perfil nuevo
    Cuando envío una solicitud para crear el perfil
    Entonces debo recibir un código de estado 201
    Y el perfil debe ser creado exitosamente
    Y la respuesta debe cumplir con el esquema de perfil

  @Smoke @ObtenerPerfil
  Escenario: Obtener el perfil de un usuario
    Dado que el usuario tiene un perfil creado
    Cuando envío una solicitud para obtener mi perfil
    Entonces debo recibir un código de estado 200
    Y debo recibir los datos de mi perfil

  @Smoke @ActualizarPerfil
  Escenario: Actualizar información del perfil
    Dado que el usuario tiene un perfil creado
    Y que tengo nuevos datos para actualizar el perfil
    Cuando envío una solicitud para actualizar mi perfil
    Entonces debo recibir un código de estado 200
    Y el perfil debe actualizarse con los nuevos datos