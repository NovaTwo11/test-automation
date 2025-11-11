# language: es
@observabilidad
Característica: Observabilidad del Sistema

  Escenario: El microservicio de monitoreo reporta salud (Reto 5.1)
    Cuando consulto el endpoint de salud de la API
    Entonces el estado debe ser "UP"

  @integracion_full
  Escenario: La creación de usuario se refleja en métricas y logs (Reto 5.2)
    Dado que genero un usuario aleatorio válido
    Y guardo el contador de peticiones POST a "/api/usuarios"
    Cuando creo el usuario vía API
    Entonces la creación responde 201 y cumple el esquema de usuario creado
    Y espero 31 segundos para que Prometheus actualice
    Y el contador de peticiones POST a "/api/usuarios" debe incrementar
    Y espero 11 segundos para que Loki actualice
    Y el log de creación debe existir en Loki para el usuario