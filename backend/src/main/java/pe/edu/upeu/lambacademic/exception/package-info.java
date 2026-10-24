/**
 * Paquete transversal: excepciones base y su manejo global son compartidos por todos
 * los modulos, no la API de uno solo (a diferencia de @NamedInterface). Ver S4 3.5.
 */
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package pe.edu.upeu.lambacademic.exception;
