# language: es
Característica: Seguridad de endpoints

  @publico
  Escenario: Listar usuarios sin token (endpoint público)
    Cuando listo los usuarios sin token
    Entonces obtengo estado 200

  # @protegido
  # Escenario: Listar usuarios sin token (endpoint protegido)
  #   Cuando listo los usuarios sin token
  #   Entonces obtengo estado 401 o 403