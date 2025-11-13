# language: es
@Observabilidad
Característica: Endpoints de Observabilidad
  Como equipo de operaciones
  Quiero monitorear el estado y métricas del sistema
  Para asegurar el correcto funcionamiento

  Antecedentes:
    Dado que los servicios del sistema están desplegados

  @Smoke @Health
  Escenario: Verificar endpoint de health check
    Cuando envío una solicitud al endpoint de health
    Entonces debo recibir un código de estado 200
    Y la respuesta debe indicar status UP

  @Smoke @Metrics
  Escenario: Verificar endpoint de métricas de Prometheus
    Cuando envío una solicitud al endpoint de métricas
    Entonces debo recibir un código de estado 200
    Y la respuesta debe estar en formato Prometheus