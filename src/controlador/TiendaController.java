package controlador;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import modelo.Producto;

public class TiendaController implements Initializable {

    @FXML
    private TableView<Producto> tablaProductos;

    @FXML
    private TableColumn<Producto, String> colNombre;

    @FXML
    private TableColumn<Producto, Double> colPrecio;

    @FXML
    private TableColumn<Producto, Integer> colStock;

    @FXML
    private TextField txtCantidad;

    @FXML
    private ListView<String> listCarrito;

    @FXML
    private Label lblMensaje;


    private ObservableList<Producto> listaProductos = FXCollections.observableArrayList();

    private Map<Producto, Integer> carrito = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
    colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
    colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));

    File archivo = new File("productos.dat");

    if (archivo.exists()) {
        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream(archivo))) {

            @SuppressWarnings("unchecked")
            ArrayList<Producto> lista = (ArrayList<Producto>) ois.readObject();

            listaProductos = FXCollections.observableArrayList(lista);
            tablaProductos.setItems(listaProductos);
            lblMensaje.setText("Productos cargados desde productos.dat.");
            return; 
        } catch (Exception e) {
            e.printStackTrace();
            lblMensaje.setText("Error leyendo productos.dat. Se cargan productos por defecto.");
        }
    } else {
        lblMensaje.setText("No se encontró productos.dat. Se cargan productos por defecto.");
    }

    }

    @FXML
    private void agregarAlCarrito() {

        lblMensaje.setText("");

        Producto seleccionado = tablaProductos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            lblMensaje.setText("Seleccione un producto.");
            return;
        }

        String textoCantidad = txtCantidad.getText().trim();
        if (textoCantidad.isEmpty()) {
            lblMensaje.setText("Ingrese una cantidad.");
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(textoCantidad);
        } catch (NumberFormatException e) {
            lblMensaje.setText("La cantidad debe ser numérica.");
            return;
        }

        if (cantidad <= 0) {
            lblMensaje.setText("La cantidad debe ser mayor que cero.");
            return;
        }

        if (seleccionado.getStock() < cantidad) {
            lblMensaje.setText("Stock insuficiente para ese producto.");
            return;
        }

        seleccionado.setStock(seleccionado.getStock() - cantidad);
        tablaProductos.refresh();

        Integer cantidadAnterior = carrito.get(seleccionado);
        if (cantidadAnterior == null) {
            carrito.put(seleccionado, cantidad);
        } else {
            carrito.put(seleccionado, cantidadAnterior + cantidad);
        }

        actualizarVistaCarrito();

        txtCantidad.clear();
        lblMensaje.setText("Producto agregado al carrito.");
    }

    private void actualizarVistaCarrito() {
        listCarrito.getItems().clear();

        for (Map.Entry<Producto, Integer> entrada : carrito.entrySet()) {
            Producto p = entrada.getKey();
            int cant = entrada.getValue();
            double subtotal = p.getPrecio() * cant;

            String linea = p.getNombre() + " x " + cant + " - Subtotal: $" + subtotal;
            listCarrito.getItems().add(linea);
        }
    }

    @FXML
    private void finalizarCompra() {

        if (carrito.isEmpty()) {
            lblMensaje.setText("El carrito está vacío.");
            return;
        }

        double total = 0;
        for (Map.Entry<Producto, Integer> entrada : carrito.entrySet()) {
            Producto p = entrada.getKey();
            int cant = entrada.getValue();
            total += p.getPrecio() * cant;
        }

        try {
            PrintWriter pw = new PrintWriter(new FileOutputStream("ticket.txt"));

            pw.println("TICKET DE COMPRA");
            pw.println("------------------------------");
            pw.println();

            for (Map.Entry<Producto, Integer> entrada : carrito.entrySet()) {
                Producto p = entrada.getKey();
                int cant = entrada.getValue();
                double subtotal = p.getPrecio() * cant;

                pw.println("Producto: " + p.getNombre());
                pw.println("Cantidad: " + cant);
                pw.println("Precio unitario: " + p.getPrecio());
                pw.println("Subtotal: " + subtotal);
                pw.println();
            }

            pw.println("TOTAL: " + total);

            pw.close();
        } catch (Exception e) {
            lblMensaje.setText("Error al generar ticket: " + e.getMessage());
            return;
        }

        try {
            ArrayList<Producto> lista = new ArrayList<>(listaProductos);

            FileOutputStream fos = new FileOutputStream("productos.dat");
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(lista);

            oos.close();
            fos.close();
        } catch (Exception e) {
            lblMensaje.setText("Error al actualizar productos.dat: " + e.getMessage());
            return;
        }

        carrito.clear();
        listCarrito.getItems().clear();

        lblMensaje.setText("Compra finalizada. Total: $" + total);
    }
}
