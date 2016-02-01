package org.server;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**--------------------------------------------------------------------------------------------
 * Converts an image into specified format or adjusts image quality in case of congestion.
 * --------------------------------------------------------------------------------------------*/
public class ImageEncoder
{
	private float compressionQuality;
	private ByteArrayOutputStream byteArrayOutputStream;
	private BufferedImage bufferedImage;
	private Iterator<ImageWriter>imageWriters;
	private ImageWriter imageWriter;
	private ImageWriteParam imageWriterParam;
	private ImageOutputStream imageOutputStream;

	private static String IMAGE_FORMAT = "jpeg";

	/**----------------------------------------------------------------
	 * Constructor.
	 * ----------------------------------------------------------------*/
	public ImageEncoder(float quality)
	{
		// compression quality value (0 to 1)
		compressionQuality = quality;

		try
		{
			byteArrayOutputStream =  new ByteArrayOutputStream();
			imageOutputStream = ImageIO.createImageOutputStream(byteArrayOutputStream);

			imageWriters = ImageIO.getImageWritersByFormatName(IMAGE_FORMAT);
			imageWriter = imageWriters.next();
			imageWriter.setOutput(imageOutputStream);

			imageWriterParam = imageWriter.getDefaultWriteParam();
			imageWriterParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			imageWriterParam.setCompressionQuality(compressionQuality);
		}
		catch (Exception ex)
		{
			System.out.println("Error encoding Exception caught: "+ex);
			System.exit(0);
		}
	}

	/**--------------------------------------------------------------------------------------------
	 * Compresses given array of bytes (input byte stream) into specified image format.
	 * --------------------------------------------------------------------------------------------*/
	public byte[] compress(byte[] imageBytes)
	{
		/* reset count field and reuse allocated buffer space to append a complete single image
		 * stream to the output. The output is assigned to an output stream with "setOutput". */
		try
		{
			byteArrayOutputStream.reset();
			bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
			imageWriter.write(null, new IIOImage(bufferedImage, null, null), imageWriterParam);
		}
		catch (Exception ex)
		{
			System.out.println("Exception caught: "+ex);
			System.exit(0);
		}
		return byteArrayOutputStream.toByteArray();
	}

	/**--------------------------------------------------------------------------------------------
	 * Sets compression quality to the value b/w 0 and 1.
	 * Is used to adjust image quality when congestion is detected.
	 * --------------------------------------------------------------------------------------------*/
	public void setCompressionQuality(float quality)
	{
		compressionQuality = quality;
		imageWriterParam.setCompressionQuality(compressionQuality);
	}
}