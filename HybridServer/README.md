# HybridServer
Proyecto de la asignatura DAI del grado en Ingeniería Informática por la ESEI, curso 2019-2020.
# Autores
Grupo 2.5:
*  Durán Cerviño, José Ángel - 53860913L.
*  González García, Alejandro - 45149510G.
# Comentarios
*  Uno de los desarrolladores emplea OpenJDK 13 en su estación de trabajo, que implementa una especificación de Java más reciente que la versión 1.8. Algunos cambios introducidos en versiones de Java posteriores pueden introducir incompatibilidades que no hemos detectado, aunque se ha tenido cuidado en no incurrir en ellos.
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
