# HybridServer
Proyecto de la asignatura DAI del grado en Ingeniería Informática por la ESEI, curso 2019-2020.
# Autor
Grupo 2.5:
*  González García, Alejandro - 45149510G.
# Comentarios
*  El proyecto está diseñado para ser ejecutado exclusivamente en Java 8. Esto es debido a que usa JAXB y JAX-WS, tecnologías de Java EE marcadas como obsoletas y luego eliminadas en posteriores versiones de Java por razones de mantenibilidad. Aunque [es posible restaurar una versión actualizada de ella en versiones más recientes con librerías externas](https://stackoverflow.com/questions/48204141/replacements-for-deprecated-jpms-modules-with-java-ee-apis), ello no se ha hecho para no quebrantar la condición de que no se pueden usar librerías externas.
*  Para ejecutar los tests que dependen de un SGBD se ha empleado MySQL 5.1.30, obtenido desde https://cdn.mysql.com/archives/mysql-5.1/mysql-noinstall-5.1.30-winx64.zip.
*  El servidor utiliza la jerarquía de clases Logger incluida en Java para mostrar información de diagnóstico a un destino de datos de registro. La visualización de estos mensajes puede ser útil para detectar problemas y ganar perspectiva sobre la interacción entre los objetos que componen al sistema. Para mostrar información de depuración extendida, puede crearse un fichero `logging.properties` con la siguiente configuración:
```
# To use these logging properties, launch the server with:
# -Djava.util.logging.config.file="logging.properties"

# Output logging messages to the console. For available handlers,
# see the hierarchy of the Handler class: https://docs.oracle.com/javase/8/docs/api/java/util/logging/package-tree.html
handlers = java.util.logging.ConsoleHandler

# -vvv logging mode
.level = ALL
java.util.logging.ConsoleHandler.level = ALL
java.util.logging.ConsoleHandler.encoding = UTF-8
```
* La clase `JAXBConfigurationLoader` puede intercambiarse con `XMLConfigurationLoader`, pues son dos implementaciones funcionalmente equivalentes del lector de configuración XML. `JAXBConfigurationLoader` fue creada hace tiempo, cuando el autor creyó que, para los propósitos de esta entrega, podía usar JAXB para leer la configuración XML. Luego pregunté si ello se podía hacer, y la respuesta fue analizar el árbol manualmente con SAX o DOM en su lugar, lo que dio lugar a la clase `XMLConfigurationLoader` final de este proyecto. El propósito de mantener la primera implementación en esta entrega es mostrarla a modo de curiosidad, para que el corrector experimente con ella si quiere. Naturalmente, no me hago responsable de su buen o mal funcionamiento (aunque no debería de funcionar mal, porque ha pasado tests igualmente).
