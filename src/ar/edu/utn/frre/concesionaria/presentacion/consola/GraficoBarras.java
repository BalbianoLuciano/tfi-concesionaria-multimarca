package ar.edu.utn.frre.concesionaria.presentacion.consola;

/**
 * Barras horizontales con caracteres ({@code █} y {@code ░}), siempre acompañadas del número
 * por quien las usa (SPEC §5).
 */
public final class GraficoBarras {

    private GraficoBarras() {
    }

    /**
     * @param valor el valor
     * @param maximo el valor que llena la barra entera
     * @param ancho  cantidad de caracteres de la barra
     * @return la barra, por ejemplo {@code ██████░░░░}
     */
    public static String barra(double valor, double maximo, int ancho) {
        int llenos = maximo <= 0 ? 0 : (int) Math.round(valor / maximo * ancho);
        if (valor > 0 && llenos == 0) {
            llenos = 1;
        }
        llenos = Math.min(llenos, ancho);
        return "█".repeat(llenos) + "░".repeat(ancho - llenos);
    }
}
