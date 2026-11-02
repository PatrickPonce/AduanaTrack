package pe.edu.usmp.aduanatrack.cita.entity;

/** Motivo de la cita (HU-11): inspección física de la mercancía o retiro tras el levante. */
public enum TipoCita {
    INSPECCION("Inspección"),
    RETIRO("Retiro de mercancía");

    private final String texto;

    TipoCita(String texto) {
        this.texto = texto;
    }

    public String texto() {
        return texto;
    }
}
