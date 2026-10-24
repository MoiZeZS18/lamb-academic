package pe.edu.upeu.lambacademic.baseuniversal.expediente.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.lambacademic.baseuniversal.persona.entity.Persona;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** CABECERA del expediente de una persona (tramite); sus detalles son los documentos. */
@Entity
@Table(name = "EXPEDIENTES")
@Getter
@Setter
@NoArgsConstructor
public class Expediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODIGO_TRAMITE", nullable = false, unique = true, length = 50)
    private String codigoTramite;

    @Column(name = "ASUNTO", nullable = false, length = 255)
    private String asunto;

    @Column(name = "FECHA_REGISTRO", nullable = false)
    private LocalDateTime fechaRegistro;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 30)
    private EstadoExpediente estado = EstadoExpediente.REGISTRADO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_PERSONA", nullable = false)
    private Persona persona;

    /** Calculado en el service a partir de los detalles; no lo envia el cliente. */
    @Column(name = "TOTAL_DOCUMENTOS", nullable = false)
    private Integer totalDocumentos = 0;

    @OneToMany(mappedBy = "expediente", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<DocumentoAdjunto> documentos = new ArrayList<>();

    public void agregarDocumento(DocumentoAdjunto documento) {
        documento.setExpediente(this);
        documentos.add(documento);
        totalDocumentos = documentos.size();
    }
}
