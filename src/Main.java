import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;

public class Main {

    public static void resizeAndCompressImages(String inputFolder, String outputFolder, int maxWidth, int maxHeight, int maxKb) {
        File inputDir = new File(inputFolder);
        File outputDir = new File(outputFolder);

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        for (File imgFile : inputDir.listFiles()) {
            if (!imgFile.isFile() || !isImageFile(imgFile)) continue;


            try {
                BufferedImage originalImage = ImageIO.read(imgFile);
                int originalWidth = originalImage.getWidth();
                int originalHeight = originalImage.getHeight();

                float scale = Math.min((float) maxWidth / originalWidth, (float) maxHeight / originalHeight);
                int newWidth = (int) (originalWidth * scale);
                int newHeight = (int) (originalHeight * scale);

                Image tmp = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = resized.createGraphics();
                g2d.drawImage(tmp, 0, 0, null);
                g2d.dispose();

                File outputFile = new File(outputDir, imgFile.getName());
                int quality = 95;

                while (true) {
                    try (FileOutputStream fos = new FileOutputStream(outputFile);
                         ImageOutputStream ios = ImageIO.createImageOutputStream(fos)) {
                        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
                        if (!writers.hasNext()) throw new RuntimeException("No JPEG writers found");
                        ImageWriter writer = writers.next();

                        ImageWriteParam param = writer.getDefaultWriteParam();
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        param.setCompressionQuality(quality / 100f);

                        writer.setOutput(ios);
                        writer.write(null, new IIOImage(resized, null, null), param);
                        writer.dispose();
                    }

                    if (outputFile.length() <= maxKb * 1024 || quality <= 10) {
                        break;
                    }

                    quality -= 5;
                }

                System.out.printf("Imagem processada: %s (%.2f KB)%n", outputFile.getPath(), outputFile.length() / 1024.0);
            } catch (Exception e) {
                System.err.println("Erro ao processar imagem: " + imgFile.getName());
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        resizeAndCompressImages("images", "output", 1800, 1800, 350);
    }


    private static boolean isImageFile(File file) {
        String[] extensions = {"jpg", "jpeg", "png", "bmp", "gif"};
        String name = file.getName().toLowerCase();
        for (String ext : extensions) {
            if (name.endsWith("." + ext)) {
                return true;
            }
        }
        return false;
    }
}