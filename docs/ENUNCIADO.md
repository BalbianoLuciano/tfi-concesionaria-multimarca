# Enunciado — Trabajo Final Integrador

Transcripción del documento de la cátedra
([original](assets/Trabajo%20Final%20Integrador%20ver.%201.0.docx)) y de la
[infografía guía](assets/infografia-guia-tfi.png).

**Tecnicatura Universitaria en Programación · Cátedra: Programación II · Año 2026**

## Escenario

Se tiene una Concesionaria Multimarcas de vehículos, que vende tanto Automóviles y Camionetas
como Motocicletas. Estos pueden ser usados (con kilometraje recorrido) como nuevos (0 Km.).

A los directivos les interesa saber:

- Marca.
- Modelo.
- Año.
- Color.
- Carrocería (o tipo de Motocicleta).

## Requisitos

Con libre criterio, a los requerimientos en el mencionado Escenario, se pide integrar:

1. Dominio del Problema. Abstracción de la realidad. Modelado. Tipos de datos.
2. Organización de código mediante esquema de "Diseño vs. Negocio".
3. Documentación interna vía remarks y JavaDoc.
4. Clases Abstractas. Herencia. Polimorfismo.
5. Patrones de diseño JavaBean y POJO.
6. Implementación de Interfaces. Transmisión del comportamiento.
7. Tratamiento de Excepciones (incluyendo Excepciones Propias).
8. Tipos Enumerativos.
9. Énfasis en la interacción con el usuario (menúes de Consola con opciones, controles de
   "data-entry", mensajes por consola).
10. Aplicación de Wrappers.
11. Organización de los datos utilizando una Colección. Sobreescritura de métodos esenciales.

## Lo que agrega la infografía

- **Esquema en capas:** separación estricta entre Interfaz (gráfica/consola), Lógica de
  Negocio y Datos.
- **Clasificación por estado:** registro diferenciado de unidades 0 km y usadas con
  kilometraje.
- **Atributos obligatorios:** marca, modelo, año, color, tipo de carrocería.
