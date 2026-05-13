package com.udjcs.common;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtil {

    private static final int MAX_DIM = 1200;
    private static final float QUALITY = 0.75f;

    public static byte[] compressToJpeg(InputStream in) throws IOException {
        byte[] raw = in.readAllBytes();

        BufferedImage img;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(raw)) {
            img = ImageIO.read(bais);
        }

        if (img == null) {
            return raw;
        }

        int w = img.getWidth();
        int h = img.getHeight();
        if (w > MAX_DIM || h > MAX_DIM) {
            double scale = Math.min((double) MAX_DIM / w, (double) MAX_DIM / h);
            int nw = (int) (w * scale);
            int nh = (int) (h * scale);
            BufferedImage scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(img, 0, 0, nw, nh, null);
            g.dispose();
            img = scaled;
        }

        if (img.getType() != BufferedImage.TYPE_INT_RGB) {
            BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            g.drawImage(img, 0, 0, null);
            g.dispose();
            img = rgb;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(QUALITY);
        writer.setOutput(new MemoryCacheImageOutputStream(out));
        writer.write(null, new IIOImage(img, null, null), params);
        writer.dispose();
        return out.toByteArray();
    }
}
