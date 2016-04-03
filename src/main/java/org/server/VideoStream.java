package org.server;

import java.io.FileInputStream;

public class VideoStream
{
	FileInputStream fis;	//video file
	int frameNumber;		//current frame nb

	//-----------------------------------
	// constructor
	//-----------------------------------
	public VideoStream(String filename) throws Exception
	{
		// init variables
		fis = new FileInputStream(filename);
		frameNumber = 0;
	}

	//-----------------------------------
	// returns the next frame as an array of byte and the size of the frame
	//-----------------------------------
	public int getNextFrame(byte[] frame) throws Exception
	{
		int length = 0;
		String length_string;
		byte[] frameLength = new byte[5];

		//read current frame length
		fis.read(frameLength, 0, 5);

		//transform frame_length to integer
		length_string = new String(frameLength);
		length = Integer.parseInt(length_string);

		return fis.read(frame, 0, length);
	}
}