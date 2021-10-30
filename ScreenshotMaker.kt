import javafx.application.Application
import javafx.beans.value.ChangeListener
import javafx.event.EventHandler
import javafx.geometry.Orientation
import javafx.scene.Scene
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.input.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.transform.Scale
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.stage.Window
import java.awt.Container
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_4BYTE_ABGR
import java.io.*
import java.lang.Double.max
import java.lang.Double.min
import javax.imageio.ImageIO
import kotlin.math.abs


class ScreenshotMaker : Application() {
    override fun start(primaryStage: Stage) {
        var cutImageMode = false

        //Выбор цвета и размера кисти
        var pickColor = ColorPicker(Color.RED)
        var pickColorContainer = VBox(Label("Цвет кисти"), pickColor)

        var pickSize = Slider(1.0, 100.0, 10.0)
        var pickSizeContainer = VBox(Label("Размер кисти"), pickSize)
        //Полотно
        var stackPaneContainer = Pane()
        var stackPane = StackPane()
        stackPaneContainer.children.add(stackPane)
        var screenRect = Rectangle(Toolkit.getDefaultToolkit().screenSize)
        stackPaneContainer.prefHeight = (screenRect.height/2).toDouble()
        stackPaneContainer.prefWidth = (screenRect.width/2).toDouble()
        var screenshotCanvas = Canvas()
        var drawingCanvas = Canvas()
        var selectingCanvas = Canvas()
        stackPane.children.addAll(screenshotCanvas, drawingCanvas, selectingCanvas)
        var screenshotCtx = screenshotCanvas.graphicsContext2D
        var drawingCtx = drawingCanvas.graphicsContext2D
        var selectingCtx = selectingCanvas.graphicsContext2D

        var prevX = -1.0
        var prevY = -1.0
        stackPane.onMousePressed = EventHandler<MouseEvent> { e ->
            if (cutImageMode) {
                prevX = e.x
                prevY = e.y
                selectingCtx.fill = Color.rgb(0, 0, 0, 0.5)
                selectingCtx.fillRect(0.0, 0.0, drawingCanvas.width, drawingCanvas.height)
            } else {
                var size = pickSize.value
                var x = e.x - size / 2
                var y = e.y - size / 2
                if (e.button == MouseButton.SECONDARY) {
                    drawingCtx.clearRect(x, y, size, size)
                } else {
                    drawingCtx.fill = pickColor.value
                    drawingCtx.fillOval(x, y, size, size)
                }
            }
        }
        stackPane.onMouseDragged = EventHandler<MouseEvent>  { e ->
            if (cutImageMode) {
                selectingCtx.clearRect(0.0, 0.0, drawingCanvas.width, drawingCanvas.height)
                selectingCtx.fill = Color.rgb(0, 0, 0, 0.5)
                selectingCtx.fillRect(0.0, 0.0, drawingCanvas.width, drawingCanvas.height)
                selectingCtx.clearRect(min(prevX, e.x), min(prevY, e.y), abs(e.x - prevX), abs(e.y - prevY))
            } else {
                var size = pickSize.value
                var x = e.x - size / 2
                var y = e.y - size / 2
                if (e.button == MouseButton.SECONDARY) {
                    drawingCtx.clearRect(x, y, size, size)
                } else {
                    drawingCtx.fill = pickColor.value
                    drawingCtx.fillOval(x, y, size, size)
                }
            }
        }
        stackPane.onMouseReleased = EventHandler<MouseEvent> { e ->
            if (cutImageMode) {
                selectingCtx.clearRect(0.0, 0.0, drawingCanvas.width, drawingCanvas.height)
                var params = SnapshotParameters()
                params.fill = Color.TRANSPARENT
                var image1 = screenshotCanvas.snapshot(params, null)
                var image2 = drawingCanvas.snapshot(params, null)
                var x = max(min(prevX, e.x), 0.0).toInt()
                var y = max(min(prevY, e.y), 0.0).toInt()
                var width = min(abs(e.x - prevX), screenshotCanvas.width - x).toInt()
                var height = min(abs(e.y - prevY), screenshotCanvas.height - y).toInt()
                var croppedImage1 = WritableImage(image1.pixelReader, x, y, width, height)
                var croppedImage2 = WritableImage(image2.pixelReader, x, y, width, height)

                changeCanvasImage(stackPane, screenshotCanvas, drawingCanvas, selectingCanvas, croppedImage1)
                drawingCtx.drawImage(croppedImage2, 0.0, 0.0)
            }
        }
        //Слайдер задержки скриншота
        var timerSlider = Slider(0.0, 10.0, 0.0)
        timerSlider.majorTickUnit = 1.0
        timerSlider.minorTickCount = 0
        timerSlider.isShowTickLabels = true
        timerSlider.isSnapToTicks = true
        var timerSliderContainer = VBox(Label("Задержка перед снимком"), timerSlider)
        //Галочка скрыть окно для скриншота
        var hideWindowCheckbox = CheckBox("Скрыть окно для скриншота");
        //Кнопка сделать скриншот
        var takeScreenshotButton = Button("Сделать скриншот")
        takeScreenshotButton.onAction = EventHandler {
            takeScreenshot(hideWindowCheckbox.isSelected, timerSlider.value, stackPane, screenshotCanvas, drawingCanvas, selectingCanvas, primaryStage)
        }
        //Меню бар
        var menuBar = MenuBar()
        var fileMenu = Menu("Фаил")
        var fileMenuScreenshot = MenuItem("Сделать скриншот")
        fileMenuScreenshot.onAction = EventHandler {
            takeScreenshot(hideWindowCheckbox.isSelected, timerSlider.value, stackPane, screenshotCanvas, drawingCanvas, selectingCanvas, primaryStage)
        }
        var fileMenuSave = MenuItem("Сохранить")
        fileMenuSave.onAction = EventHandler {
            saveImage(false, stackPane, screenshotCanvas, drawingCanvas, selectingCanvas, primaryStage)
        }
        var fileMenuQuickSave = MenuItem("Быстрое сохранение")
        fileMenuQuickSave.onAction = EventHandler {
            saveImage(true, stackPane, screenshotCanvas, drawingCanvas, selectingCanvas, primaryStage)
        }
        var fileMenuOpen = MenuItem("Открыть")
        fileMenuOpen.onAction = EventHandler {
            openImage(stackPane, screenshotCanvas, drawingCanvas, selectingCanvas, primaryStage)
        }
        var fileMenuExit = MenuItem("Выйти")
        fileMenuExit.onAction = EventHandler {
            primaryStage.close();
        }
        fileMenuScreenshot.accelerator = KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN)
        fileMenuSave.accelerator = KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN)
        fileMenuQuickSave.accelerator = KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN)
        fileMenu.items.addAll(fileMenuScreenshot, fileMenuOpen, fileMenuSave, fileMenuQuickSave, fileMenuExit)
        menuBar.menus.add(fileMenu)
        //Кнопка обрезать изображение
        var cutImageButton = Button("Обрезать")

        var editImagePanel = FlowPane(Orientation.VERTICAL, 8.0, 8.0)
        editImagePanel.children.add(takeScreenshotButton)
        editImagePanel.children.add(timerSliderContainer)
        editImagePanel.children.add(hideWindowCheckbox)
        editImagePanel.children.add(pickColorContainer)
        editImagePanel.children.add(pickSizeContainer)
        editImagePanel.children.add(cutImageButton)

        var cancelCutImageButton = Button("Назад")

        var cutImagePanel = FlowPane(Orientation.VERTICAL, 8.0, 8.0)
        cutImagePanel.isVisible = false
        cutImagePanel.children.add(cancelCutImageButton)

        cutImageButton.onAction = EventHandler {
            editImagePanel.isVisible = false
            cutImagePanel.isVisible = true
            cutImageMode = true
        }
        cancelCutImageButton.onAction = EventHandler {
            editImagePanel.isVisible = true
            cutImagePanel.isVisible = false
            cutImageMode = false
        }
        var leftPanel = StackPane()
        leftPanel.children.add(editImagePanel)
        leftPanel.children.add(cutImagePanel)
        //Сцена
        var root = VBox()
        var hBox = HBox()
        var leftAnchorPane = AnchorPane(leftPanel)
        AnchorPane.setTopAnchor(leftPanel, 8.0);
        AnchorPane.setLeftAnchor(leftPanel, 8.0);
        AnchorPane.setRightAnchor(leftPanel, 8.0);
        root.children.add(menuBar)
        hBox.children.add(leftAnchorPane)
        hBox.children.add(stackPaneContainer)
        root.children.add(hBox)

        var scene = Scene(root)
        primaryStage.title = "Скриншотоделалка"
        primaryStage.scene = scene
        primaryStage.show()
        primaryStage.sizeToScene()
        primaryStage.isResizable = false
    }

    private fun takeScreenshot(hideWindow: Boolean, timer: Double, stackPane: StackPane, c1:Canvas, c2: Canvas, c3:Canvas, primaryStage: Stage) {
        if (hideWindow) {
            primaryStage.hide();
            Thread.sleep(200)
        }
        Thread.sleep((timer * 1000).toLong())
        var image = getScreenshot()
        changeCanvasImage(stackPane, c1, c2, c3, image)
        primaryStage.show();
    }
    private fun getScreenshot(): Image? {
        try {
            var robot = Robot()
            var screenRect = Rectangle(Toolkit.getDefaultToolkit().screenSize)
            var screenFullImage = robot.createScreenCapture(screenRect)
            return convertToFxImage(screenFullImage)
        } catch (ex: IOException) {
            print(ex)
            return null
        }
    }
    private fun changeCanvasImage(stackPane: StackPane, c1:Canvas, c2: Canvas, c3:Canvas, image: Image?) {
        if (image != null) {
            c2.graphicsContext2D.clearRect(0.0, 0.0, c2.width, c2.height)
            c1.height = image.height
            c1.width = image.width
            c2.height = image.height
            c2.width = image.width
            c3.height = image.height
            c3.width = image.width
            var screenRect = Rectangle(Toolkit.getDefaultToolkit().screenSize)
            var scaleMult = min(1.0, 0.5 * min(screenRect.getHeight() / image.height, screenRect.getWidth() / image.width))
            var scale = Scale(scaleMult, scaleMult)
            scale.pivotX = 0.0
            scale.pivotY = 0.0
            stackPane.transforms.setAll(scale)
            c1.graphicsContext2D.drawImage(image, 0.0, 0.0)
        }
    }
    private fun openImage(stackPane: StackPane, c1:Canvas, c2: Canvas, c3:Canvas, primaryStage: Stage) {
        var fileChooser = FileChooser()
        var imageFilter = FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png")
        fileChooser.extensionFilters.add(imageFilter)
        fileChooser.initialDirectory = File(getLastDirectory("open"))
        var file = fileChooser.showOpenDialog(primaryStage)
        writeLastDirectory("open", file.path)
        var image = Image(file.toURI().toString());
        changeCanvasImage(stackPane, c1, c2, c3, image)
    }
    private fun saveImage(useDefaultDirectory: Boolean, stackPane: StackPane, c1:Canvas, c2: Canvas, c3: Canvas, primaryStage: Stage) {
        var file: File
        if (useDefaultDirectory) {
            file = File(System.getenv("USERPROFILE") + "\\Documents\\quickSave.png")
        } else {
            var fileChooser = FileChooser()
            var imageFilter = FileChooser.ExtensionFilter("Image Files", "*.png")
            fileChooser.extensionFilters.add(imageFilter)
            fileChooser.initialDirectory = File(getLastDirectory("save"))
            file = fileChooser.showSaveDialog(primaryStage)
        }
        var params = SnapshotParameters()
        params.fill = Color.TRANSPARENT
        var image1 = c1.snapshot(params, null)
        var image2 = c2.snapshot(params, null)
        c3.graphicsContext2D.drawImage(image1, 0.0, 0.0)
        c3.graphicsContext2D.drawImage(image2, 0.0, 0.0)
        image1 = c3.snapshot(params, null)
        c3.graphicsContext2D.clearRect(0.0, 0.0, image1.width, image1.height)
        ImageIO.write(convertToBufferedImage(image1), "png", file)
        if (!useDefaultDirectory) {
            writeLastDirectory("save", file.path)
        }
    }
    private fun writeLastDirectory(filename: String, text: String) {
        try {
            BufferedWriter(PrintWriter(System.getenv("USERPROFILE") + "\\Documents\\" + filename + ".txt")).use { bw ->
                var file = File(text);
                if (file.isFile)
                    bw.write(file.parent)
                else
                    bw.write(text)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    private fun getLastDirectory(filename: String): String {
        try {
            BufferedReader(FileReader(System.getenv("USERPROFILE") + "\\Documents\\" + filename + ".txt")).use { reader ->
                return reader.readLine()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return System.getenv("USERPROFILE") + "\\Documents\\"
        }
    }

    private fun convertToFxImage(image: BufferedImage?): Image? {
        var wr: WritableImage? = null
        if (image != null) {
            wr = WritableImage(image.width, image.height)
            var pw = wr.pixelWriter
            for (x in 0 until image.width) {
                for (y in 0 until image.height) {
                    pw.setArgb(x, y, image.getRGB(x, y))
                }
            }
        }
        return ImageView(wr).image
    }
    private fun convertToBufferedImage(image: Image?): BufferedImage? {
        var wr: BufferedImage? = null
        if (image != null) {
            wr = BufferedImage(image.width.toInt(), image.height.toInt(), TYPE_4BYTE_ABGR)
            var pw = image.pixelReader
            for (x in 0 until image.width.toInt()) {
                for (y in 0 until image.height.toInt()) {
                    wr.setRGB(x, y, pw.getArgb(x, y))
                }
            }
        }
        return wr
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(ScreenshotMaker::class.java)
        }
    }
}

