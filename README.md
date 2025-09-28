# AREP-Lab05-Properties
Proyecto: Sistema CRUD para gestión de propiedades

## Resumen del proyecto
Este repositorio contiene una aplicación backend en Java (Spring Boot) que implementa un sistema CRUD para gestionar propiedades (inmuebles). La API permite crear, leer, actualizar y eliminar propiedades, además de búsquedas avanzadas por dirección, texto libre, rango de precio y rango de tamaño.

Principales responsabilidades:
- Almacenar y consultar propiedades en una base de datos MySQL.
- Exponer endpoints REST para operaciones CRUD y búsqueda.
- Validar entradas y manejar errores de forma centralizada.

## Video de despliegue
https://youtu.be/JL5P3XX3E4g

<video src="https://youtu.be/JL5P3XX3E4g" controls width="720"></video>

## Arquitectura del sistema

Componentes principales:
- Frontend: (se encuentra en `resources/static`) Puede ser cualquier cliente HTTP/SPA que consuma la API REST.
- Backend: Aplicación Spring Boot ubicada en `src/main/java` que expone endpoints REST en el puerto 8080.
- Base de datos: MySQL. En el repositorio se incluye un `docker-compose.yml` que levanta un servicio `mysql` y el servicio `app` (la API) para desarrollo local.

Interacción:
1. El cliente (frontend o curl/Postman) realiza peticiones HTTP al backend.
2. El backend usa JPA/Hibernate para persistir y consultar datos en MySQL.
3. Las respuestas se devuelven en formato JSON y los errores se manejan por un `@ControllerAdvice` global.

Diagrama simplificado:

Client <--HTTP--> Backend (Spring Boot) <--JPA/Hibernate--> MySQL

En local se puede ejecutar con Docker Compose: el contenedor `mysql` y el contenedor `app` se comunican usando la red definida en `docker-compose.yml`.

## Diseño de clases (overview)

Componentes principales del backend:

- `co.edu.escuelaing.propertiesapi.model.entity.Property`
	- Entidad JPA que representa una propiedad.
	- Campos típicos: `id: Long`, `address: String`, `price: BigDecimal`, `size: Double`, `description: String`, `createdAt`, `updatedAt`.

- `co.edu.escuelaing.propertiesapi.model.dto.PropertyDto`
	- DTO usado para recibir/validar datos en las APIs.
	- Contiene validaciones (`@NotBlank`, `@NotNull`, `@Positive`, etc.).

- `co.edu.escuelaing.propertiesapi.repository.PropertyRepository`
	- Extiende `JpaRepository<Property, Long>` y `JpaSpecificationExecutor<Property>` para consultas paginadas y filtradas.

- `co.edu.escuelaing.propertiesapi.service.PropertyService` (interface)
	- Define operaciones: `create`, `list(Pageable)`, `get`, `update`, `delete`, `search(...)`.

- `co.edu.escuelaing.propertiesapi.service.impl.PropertyServiceImpl`
	- Implementación de `PropertyService`.
	- Maneja lógica de negocio y lanza `NoSuchElementException` cuando no encuentra recursos.

- `co.edu.escuelaing.propertiesapi.controller.PropertyController`
	- Expones los endpoints REST (GET/POST/PUT/DELETE) y delega al servicio.
	- Usa `@Valid` para validar `PropertyDto` y devuelve `201 Created` en creación.

- `co.edu.escuelaing.propertiesapi.controller.GlobalExceptionHandler`
	- Manejador global (`@ControllerAdvice`) que captura `MethodArgumentNotValidException`, `NoSuchElementException`, `DataIntegrityViolationException`, `ConstraintViolationException` y excepciones generales para mapearlas a respuestas HTTP adecuadas (400/404/409/500).

## Endpoints principales

Ejemplos (asumimos base `/api/properties`):
- GET `/api/properties` -> listar (paginado) / búsqueda con parámetros opcionales (address, q, minPrice, maxPrice, minSize, maxSize, page, size)
- GET `/api/properties/{id}` -> obtener por id
- POST `/api/properties` -> crear (body: `PropertyDto`)
- PUT `/api/properties/{id}` -> actualizar
- DELETE `/api/properties/{id}` -> eliminar

## Instrucciones de despliegue

Requisitos previos:
- Java 17+ y Maven para generar el `jar` localmente.
- Docker y Docker Compose para pruebas locales.
- Cuenta AWS con permisos para ECR (registro de imágenes) y ECS/EC2/RDS según la opción de despliegue.

1) Construir artefacto Java (local)

```bash
mvn clean package
```

2) Construir imagen Docker localmente

```bash
docker build -t rivitas13/arep-lab05-properties:latest .
```

3) Ejecutar en local con Docker Compose (levanta MySQL y la app)

```bash
docker-compose up --build
```

Ajustes importantes en `docker-compose.yml`:
- El servicio `mysql` expone el puerto 3306 y tiene variables de entorno para la contraseña/usuario.
- El servicio `app` usa variables `DB_URL`, `DB_USER`, `DB_PASS` y el `SPRING_PROFILES_ACTIVE`.

4) Publicar la imagen en Docker hub

- Crear un repositorio en dockerhub (o usar uno existente).

```bash
docker push rivitas13/arep-lab05-properties:latest
```



5) Desplegar en AWS

Opciones recomendadas:
- ECS Fargate (serverless containers): crear Cluster -> Task Definition apuntando a la imagen en ECR -> Service (Fargate) con ALB si es necesario.
- EC2 con Docker Compose: lanzar EC2, instalar Docker/Docker Compose, desplegar `docker-compose.yml` (adecuar `DB_URL` a la dirección de RDS o instancia MySQL local en EC2).

Resumen pasos (ECS Fargate):

1. Crear RDS MySQL (o usar Amazon Aurora) con la base `properties` y credenciales.
2. Configurar Security Groups: permitir tráfico desde tareas ECS al puerto 3306 del RDS.
3. Crear repositorio ECR y subir imagen (ver arriba).
4. Crear Task Definition (container definition que referencia la imagen ECR). Configurar variables de entorno `DB_URL`, `DB_USER`, `DB_PASS` apuntando a RDS.
5. Crear Service en ECS con desired count > 0 y, si es necesario, un Application Load Balancer para exponer puerto 8080.

Notas de configuración:
- Asegúrate de que `DB_URL` use el hostname/dns del RDS y no `localhost` cuando la app corra en ECS.
- Si usas secretos, almacena credenciales en AWS Secrets Manager y referéncialos desde la Task Definition.

Comandos útiles (AWS CLI + ECS orientativo):

```bash
# Registrar definición de tarea (ejemplo simplificado):
aws ecs register-task-definition --cli-input-json file://task-def.json

# Crear servicio
aws ecs create-service --cluster my-cluster --service-name propertiesapi-svc --task-definition propertiesapi:1 --launch-type FARGATE --desired-count 2 --network-configuration "awsvpcConfiguration={subnets=[subnet-...],securityGroups=[sg-...],assignPublicIp=ENABLED}"
```

6) Alternativa: EC2 + Docker Compose

1. Provisiona una EC2 (Ubuntu) e instala Docker/Docker Compose.
2. Copia el repositorio o el `docker-compose.yml` y construye la imagen o usa la imagen ECR.
3. Ajusta `DB_URL` para apuntar a tu RDS o a la instancia MySQL en la red.
4. `docker compose up -d --build`

## Manejo de errores y buenas prácticas
- Validar DTOs con anotaciones `jakarta.validation` para asegurar entradas correctas.
- Manejar excepciones de BD (`DataIntegrityViolationException`, `ConstraintViolationException`) en el `@ControllerAdvice` y mapear a 400/409 cuando proceda.
- No incluir credenciales en el repositorio; usar variables de entorno o secretos del proveedor en producción.

## Screenshots (placeholders)

Incluye capturas demostrando operaciones CRUD. Se recomienda guardar las imágenes en `docs/screenshots` y referenciarlas en este README.

Ejemplos de endpoints para generar las capturas (usar Postman o curl):

1. Crear una propiedad (POST):

```bash
curl -X POST http://localhost:8080/api/properties \
	-H 'Content-Type: application/json' \
	-d '{"address":"Calle 123","price":100000.00,"size":120.5,"description":"Apartamento céntrico"}'
```

2. Listar (GET):

```bash
curl http://localhost:8080/api/properties
```

3. Actualizar (PUT) y Eliminar (DELETE) para completar flujo CRUD.

Placeholder imágenes (agrega archivos reales bajo `docs/screenshots`):

- `docs/screenshots/create.png` - petición POST y respuesta 201
- `docs/screenshots/list.png` - listado paginado
- `docs/screenshots/get.png` - detalle GET /{id}
- `docs/screenshots/update.png` - PUT y resultado
- `docs/screenshots/delete.png` - DELETE y resultado

## Cómo contribuir / notas finales

- Para desarrollo, usar el perfil `dev` (archivo `src/main/resources/application-dev.properties`) que puede contener settings para H2 o una instancia MySQL local.
- Para producción, usar `application-prod.properties` y variables de entorno seguras.
- Si quieres que agregue diagramas en formato PlantUML o capture de pantalla real, puedo generarlos y añadirlos en `docs/`.

---
Versión: 1.0 — documentación inicial generada.
