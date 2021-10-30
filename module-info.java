module com.example.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;

    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;

    opens GUI to javafx.fxml;
    exports GUI;
}