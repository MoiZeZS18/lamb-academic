package pe.edu.upeu.lambacademic.baseuniversal.expediente.entity;

/** Tipos de archivo aceptados; la extension del nombre de archivo debe coincidir. */
public enum TipoArchivo {
    PDF("pdf"),
    JPG("jpg"),
    PNG("png"),
    DOCX("docx");

    private final String extension;

    TipoArchivo(String extension) {
        this.extension = extension;
    }

    public boolean coincideCon(String nombreArchivo) {
        return nombreArchivo != null && nombreArchivo.toLowerCase().endsWith("." + extension);
    }
}
